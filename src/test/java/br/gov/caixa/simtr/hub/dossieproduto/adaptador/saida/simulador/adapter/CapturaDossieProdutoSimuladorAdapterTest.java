package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.CapturaDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.CapturaDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Qualifier;

class CapturaDossieProdutoSimuladorAdapterTest {

    private static final long IDENTIFICADOR = 123L;
    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/captura-dossie-produto.md";

    @Test
    void implementaPortaComQualifierSimuladorExclusivo() {
        var tipo = CapturaDossieProdutoSimuladorAdapter.class;

        assertTrue(SolicitarCapturaDossieProduto.class.isAssignableFrom(tipo));
        assertNotNull(tipo.getAnnotation(ApplicationScoped.class));
        assertNotNull(tipo.getAnnotation(CapturaSimulador.class));
        assertNotNull(CapturaSimulador.class.getAnnotation(Qualifier.class));
        assertEquals(RUNTIME, CapturaSimulador.class.getAnnotation(Retention.class).value());
        assertEquals(Set.of(TYPE, FIELD, PARAMETER, METHOD),
                Set.of(CapturaSimulador.class.getAnnotation(Target.class).value()));
        assertNotSame(CapturaMtr.class, CapturaSimulador.class);
    }

    @Test
    void capturaSomenteOIdentificadorRegistradoNaFixture() {
        var adapter = new CapturaDossieProdutoSimuladorAdapter(
                new MarkdownJsonMockReader(new ObjectMapper()),
                new CapturaDossieProdutoSimuladorMapper());

        var resultado = adapter.capturar(new IdentificadorDossieProduto(IDENTIFICADOR))
                .await().indefinitely();

        assertEquals(IDENTIFICADOR, resultado.identificadorDossieProduto());
    }

    @Test
    void retornaFalhaDeNegocio404QuandoFixtureNaoExiste() {
        var reader = mock(MarkdownJsonMockReader.class);
        var mapper = mock(CapturaDossieProdutoSimuladorMapper.class);
        when(reader.readFirstJsonObject(
                MOCK_RESOURCE,
                CapturaDossieProdutoSimuladorResponse.class)).thenReturn(null);
        var adapter = new CapturaDossieProdutoSimuladorAdapter(reader, mapper);

        var falha = executarFalha(adapter, IDENTIFICADOR);

        assertNaoEncontrado(falha, IDENTIFICADOR);
        verify(reader).readFirstJsonObject(
                MOCK_RESOURCE,
                CapturaDossieProdutoSimuladorResponse.class);
        verifyNoMoreInteractions(reader);
        verifyNoInteractions(mapper);
    }

    @Test
    void retornaFalhaDeNegocio404QuandoIdentificadorDivergeDaFixture() {
        var reader = mock(MarkdownJsonMockReader.class);
        var mapper = mock(CapturaDossieProdutoSimuladorMapper.class);
        when(reader.readFirstJsonObject(
                MOCK_RESOURCE,
                CapturaDossieProdutoSimuladorResponse.class)).thenReturn(
                        new CapturaDossieProdutoSimuladorResponse(IDENTIFICADOR));
        var adapter = new CapturaDossieProdutoSimuladorAdapter(reader, mapper);

        var falha = executarFalha(adapter, 456L);

        assertNaoEncontrado(falha, 456L);
        verifyNoInteractions(mapper);
    }

    @Test
    void retornaFalhaDeNegocio404QuandoFixtureNaoTemIdentificador() {
        var reader = mock(MarkdownJsonMockReader.class);
        var mapper = mock(CapturaDossieProdutoSimuladorMapper.class);
        when(reader.readFirstJsonObject(
                MOCK_RESOURCE,
                CapturaDossieProdutoSimuladorResponse.class)).thenReturn(
                        new CapturaDossieProdutoSimuladorResponse(null));
        var adapter = new CapturaDossieProdutoSimuladorAdapter(reader, mapper);

        var falha = executarFalha(adapter, IDENTIFICADOR);

        assertNaoEncontrado(falha, IDENTIFICADOR);
        verifyNoInteractions(mapper);
    }

    @Test
    void marcaOrigemMockEIdentificadorSemCriarSpanClient() {
        var exporter = InMemorySpanExporter.create();
        var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        try {
            var openTelemetry = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
            var span = openTelemetry.getTracer("teste-captura-dossie-simulador")
                    .spanBuilder("teste.captura-dossie.simulador")
                    .startSpan();
            try (var _ = span.makeCurrent()) {
                var adapter = new CapturaDossieProdutoSimuladorAdapter(
                        new MarkdownJsonMockReader(new ObjectMapper()),
                        new CapturaDossieProdutoSimuladorMapper());
                adapter.capturar(new IdentificadorDossieProduto(IDENTIFICADOR))
                        .await().indefinitely();
            } finally {
                span.end();
            }
            tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);

            assertEquals(1, exporter.getFinishedSpanItems().size());
            var spanObservado = exporter.getFinishedSpanItems().getFirst();
            assertEquals("mock", spanObservado.getAttributes().get(
                    AttributeKey.stringKey("simtr_hub.origem_dados")));
            assertEquals(IDENTIFICADOR, spanObservado.getAttributes().get(
                    AttributeKey.longKey("dossie_produto.id")));
        } finally {
            tracerProvider.close();
        }
    }

    @Test
    void naoAplicaFaultToleranceAoAdapterSimulado() throws NoSuchMethodException {
        var tipo = CapturaDossieProdutoSimuladorAdapter.class;
        var metodo = tipo.getMethod("capturar", IdentificadorDossieProduto.class);

        assertFalse(tipo.isAnnotationPresent(Timeout.class));
        assertFalse(tipo.isAnnotationPresent(Retry.class));
        assertFalse(tipo.isAnnotationPresent(CircuitBreaker.class));
        assertFalse(metodo.isAnnotationPresent(Timeout.class));
        assertFalse(metodo.isAnnotationPresent(Retry.class));
        assertFalse(metodo.isAnnotationPresent(CircuitBreaker.class));
    }

    private static FalhaCapturaDossieProduto executarFalha(
            CapturaDossieProdutoSimuladorAdapter adapter,
            Long identificador
    ) {
        var espera = adapter.capturar(new IdentificadorDossieProduto(identificador)).await();
        return assertThrows(FalhaCapturaDossieProduto.class, espera::indefinitely);
    }

    private static void assertNaoEncontrado(
            FalhaCapturaDossieProduto falha,
            Long identificador
    ) {
        assertEquals(FalhaCapturaDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("simtr-dossie-produto", falha.recurso());
        assertEquals("mock-captura-dossie-produto-" + identificador, falha.idErro());
        assertEquals("DOSSIE_PRODUTO_NAO_ENCONTRADO", falha.codigoErro());
        assertEquals(
                "Dossie produto " + identificador + " nao encontrado no simulador.",
                falha.mensagens().getFirst());
    }
}
