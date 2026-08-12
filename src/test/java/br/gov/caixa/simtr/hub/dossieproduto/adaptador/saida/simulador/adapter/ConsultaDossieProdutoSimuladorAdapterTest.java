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

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ConsultaDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ConsultaDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.Test;

class ConsultaDossieProdutoSimuladorAdapterTest {

    private static final long IDENTIFICADOR = 4324680L;

    @Test
    void implementaPortaComQualifierSimuladorExclusivo() {
        var tipo = ConsultaDossieProdutoSimuladorAdapter.class;

        assertTrue(ObterDossieProduto.class.isAssignableFrom(tipo));
        assertNotNull(tipo.getAnnotation(ApplicationScoped.class));
        assertNotNull(tipo.getAnnotation(ConsultaDossieProdutoSimulador.class));
        assertNotNull(ConsultaDossieProdutoSimulador.class.getAnnotation(Qualifier.class));
        assertEquals(RUNTIME,
                ConsultaDossieProdutoSimulador.class.getAnnotation(Retention.class).value());
        assertEquals(Set.of(TYPE, FIELD, PARAMETER, METHOD), Set.of(
                ConsultaDossieProdutoSimulador.class.getAnnotation(Target.class).value()));
        assertNotSame(ConsultaDossieProdutoMtr.class, ConsultaDossieProdutoSimulador.class);
    }

    @Test
    void obtemDossieSomenteDaFixtureCorrespondenteAoIdentificador() {
        var adapter = new ConsultaDossieProdutoSimuladorAdapter(
                new MarkdownJsonMockReader(new ObjectMapper()),
                new ConsultaDossieProdutoSimuladorMapper()
        );

        var resultado = adapter.obter(new IdentificadorDossieProduto(IDENTIFICADOR))
                .await().indefinitely();

        assertEquals(IDENTIFICADOR, resultado.id());
        assertEquals("00000000000", resultado.clientes().getFirst().cpf());
        assertEquals("CLIENTE SIMULADO", resultado.clientes().getFirst().nome());
    }

    @Test
    void retornaFalhaDeNegocio404SemFallbackQuandoFixtureDoIdNaoExiste() {
        var reader = mock(MarkdownJsonMockReader.class);
        var mapper = mock(ConsultaDossieProdutoSimuladorMapper.class);
        var recurso = "mock/dossieproduto/9999999-v2-consulta-dossie-produto.md";
        when(reader.readFirstJsonObject(recurso, ConsultaDossieProdutoSimuladorResponse.class))
                .thenReturn(null);
        var adapter = new ConsultaDossieProdutoSimuladorAdapter(reader, mapper);

        var espera = adapter.obter(new IdentificadorDossieProduto(9999999L)).await();
        var falha = assertThrows(FalhaConsultaDossieProduto.class, espera::indefinitely);

        assertEquals(FalhaConsultaDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("simtr-dossie-produto", falha.recurso());
        assertEquals("DOSSIE_PRODUTO_NAO_ENCONTRADO", falha.codigoErro());
        assertEquals("Dossie produto 9999999 nao encontrado no simulador.",
                falha.mensagens().getFirst());
        verify(reader).readFirstJsonObject(
                recurso, ConsultaDossieProdutoSimuladorResponse.class);
        verifyNoMoreInteractions(reader);
        verifyNoInteractions(mapper);
    }

    @Test
    void marcaOrigemMockEIdentificadorNoSpanAtivo() {
        var exporter = InMemorySpanExporter.create();
        var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        try {
            var openTelemetry = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
            var span = openTelemetry.getTracer("teste-consulta-dossie-simulador")
                    .spanBuilder("teste.consulta-dossie.simulador")
                    .startSpan();
            try (var _ = span.makeCurrent()) {
                var adapter = new ConsultaDossieProdutoSimuladorAdapter(
                        new MarkdownJsonMockReader(new ObjectMapper()),
                        new ConsultaDossieProdutoSimuladorMapper()
                );
                adapter.obter(new IdentificadorDossieProduto(IDENTIFICADOR))
                        .await().indefinitely();
            } finally {
                span.end();
            }
            tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);

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
        var tipo = ConsultaDossieProdutoSimuladorAdapter.class;
        var metodo = tipo.getMethod("obter", IdentificadorDossieProduto.class);

        assertFalse(tipo.isAnnotationPresent(Timeout.class));
        assertFalse(tipo.isAnnotationPresent(Retry.class));
        assertFalse(tipo.isAnnotationPresent(CircuitBreaker.class));
        assertFalse(metodo.isAnnotationPresent(Timeout.class));
        assertFalse(metodo.isAnnotationPresent(Retry.class));
        assertFalse(metodo.isAnnotationPresent(CircuitBreaker.class));
    }
}
