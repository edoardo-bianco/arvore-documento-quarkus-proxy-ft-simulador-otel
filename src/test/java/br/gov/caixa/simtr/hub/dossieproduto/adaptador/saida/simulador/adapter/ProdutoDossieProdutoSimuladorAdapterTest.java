package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ProdutoDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ProdutoDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

class ProdutoDossieProdutoSimuladorAdapterTest {

    @Test
    void leFixturePropriaERetornaVoid() {
        var adapter = new ProdutoDossieProdutoSimuladorAdapter(
                new MarkdownJsonMockReader(new ObjectMapper()),
                new ProdutoDossieProdutoSimuladorMapper());
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(123L, List.of());

        var resultado = adapter.alterar(comando).await().indefinitely();

        assertNull(resultado);
    }

    @Test
    void falhaExplicitamenteQuandoFixtureNaoExiste() {
        MarkdownJsonMockReader reader = mock(MarkdownJsonMockReader.class);
        when(reader.readFirstJsonObject(
                anyString(),
                eq(ProdutoDossieProdutoSimuladorResponse.class))).thenReturn(null);
        var adapter = new ProdutoDossieProdutoSimuladorAdapter(
                reader,
                new ProdutoDossieProdutoSimuladorMapper());

        var falha = assertThrows(
                IllegalStateException.class,
                () -> adapter.alterar(null));

        assertEquals(
                "Arquivo de mock nao encontrado no classpath: "
                        + "mock/dossieproduto/produto-dossie-produto.md",
                falha.getMessage());
    }

    @Test
    void marcaOrigemMockNoSpanAtivo() {
        var exporter = InMemorySpanExporter.create();
        var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        try {
            var openTelemetry = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
            var span = openTelemetry.getTracer("teste-produto-simulador")
                    .spanBuilder("teste.produto.simulador")
                    .startSpan();
            try (var _ = span.makeCurrent()) {
                var adapter = new ProdutoDossieProdutoSimuladorAdapter(
                        new MarkdownJsonMockReader(new ObjectMapper()),
                        new ProdutoDossieProdutoSimuladorMapper());
                adapter.alterar(new ComandoAlteracaoProdutosContratadosDossieProduto(
                        123L,
                        List.of())).await().indefinitely();
            } finally {
                span.end();
            }
            tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);

            var spanObservado = exporter.getFinishedSpanItems().getFirst();
            assertEquals(
                    "mock",
                    spanObservado.getAttributes().get(
                            AttributeKey.stringKey("simtr_hub.origem_dados")));
        } finally {
            tracerProvider.close();
        }
    }

    @Test
    void naoAplicaFaultToleranceAoAdapterSimulado() throws NoSuchMethodException {
        var tipo = ProdutoDossieProdutoSimuladorAdapter.class;
        var metodo = tipo.getMethod(
                "alterar",
                ComandoAlteracaoProdutosContratadosDossieProduto.class);

        assertFalse(tipo.isAnnotationPresent(Timeout.class));
        assertFalse(tipo.isAnnotationPresent(Retry.class));
        assertFalse(tipo.isAnnotationPresent(CircuitBreaker.class));
        assertFalse(metodo.isAnnotationPresent(Timeout.class));
        assertFalse(metodo.isAnnotationPresent(Retry.class));
        assertFalse(metodo.isAnnotationPresent(CircuitBreaker.class));
    }
}
