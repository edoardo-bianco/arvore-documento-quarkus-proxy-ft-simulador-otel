package br.gov.caixa.simtr.orquestrador.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Caracterizacao terminal; o mock da porta nao comprova spans internos do Hub. */
@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(MonitoramentoTelemetriaEmuladorTest.TerminalProfile.class)
class MonitoramentoTelemetriaEmuladorTest extends SuporteTelemetriaMonitoramento {
    @InjectMock ConsultarDossieProduto hub;

    @Override
    protected String cenario() {
        return "terminal";
    }

    @Test
    void deveCaracterizarPerdaDeContextoEntrePostConsultaELog() throws Exception {
        var chamada = new LinkedBlockingQueue<SpanContext>();
        var resposta = new CompletableFuture<DossieProdutoConsultado>();
        when(hub.executar(any())).thenAnswer(_ -> {
            chamada.add(Span.current().getSpanContext());
            return Uni.createFrom().completionStage(resposta);
        });
        try {
            iniciarPost("0007");
            var contextoHub = chamada.poll(ESPERA.toSeconds(), TimeUnit.SECONDS);
            assertNotNull(contextoHub, "O listener real deve chamar a porta do Hub.");
            assertFalse(contextoHub.isValid());
            observarEntrada();
            assertEquals(1, observadas.size());
            assertEquals(1, observadas.getFirst().tentativa());
            assertEquals(0, contar(registros(), EVENTO_FINAL));
            assertFalse(Span.current().getSpanContext().isValid(), "O teste nao pode propagar contexto ao liberar o Hub.");
            resposta.complete(respostaHub());
            aguardarFluxo(1);
            var capturados = spans();
            var http = verificarCadeiaInicial(capturados, 0);
            var log = verificarLogFinal();
            verificarSemContexto(log);
            verificarLogsListener(http, 0);
            verificarLogsPublicacaoInicial(registros(), 2);
            var resultado = verificarResultado("CONCLUSIVO", "SITUACAO_CONCLUSIVA_MTR", 1);
            assertEquals("CONFORME", resultado.situacaoPreValidacao());
            verify(hub).executar(new IdentificadorDossieProduto(7L));
            registrarInventarioSanitizado();
        } finally {
            resposta.complete(respostaHub());
        }
    }

    private static DossieProdutoConsultado respostaHub() {
        return new DossieProdutoConsultado(7L, null, null, null, null, null, null,
                List.of(), null, null, new DossieProdutoConsultado.Situacao(null, "FINALIZADO_CONFORME", null, null),
                List.of(), List.of());
    }

    public static class TerminalProfile extends Perfil {
        @Override
        protected String cenario() {
            return "terminal";
        }
    }
}
