package br.gov.caixa.simtr.orquestrador.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Wrapper CDI e fixture reais do Hub, sem mock/spy de sua porta publica. */
@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(MonitoramentoTelemetriaHubEmuladorTest.HubProfile.class)
class MonitoramentoTelemetriaHubEmuladorTest extends SuporteTelemetriaMonitoramento {
    @Override
    protected String cenario() {
        return "hub-real";
    }

    @Test
    void deveCaracterizarTresConsultasDoHubAteQuarentenaEmTracesSeparados() throws Exception {
        iniciarPost("4324680");
        aguardarFluxo(3);
        var capturados = spans();
        var http = verificarCadeiaInicial(capturados, 3);
        var consultas = capturados.stream().filter(span -> SPAN_HUB.equals(span.getName())).toList();
        assertEquals(3, consultas.size());
        assertEquals(3, consultas.stream().map(SpanData::getTraceId).distinct().count());
        for (var consulta : consultas) {
            assertEquals(SpanKind.INTERNAL, consulta.getKind());
            assertNotEquals(tracePost, consulta.getTraceId());
            assertFalse(consulta.getParentSpanContext().isValid());
            assertTrue(consulta.getLinks().isEmpty());
            assertEquals(4324680L, consulta.getAttributes().get(AttributeKey.longKey("dossie_produto.id")));
            assertEquals("mock", consulta.getAttributes().get(AttributeKey.stringKey("simtr_hub.origem_dados")));
        }
        for (int tentativa : new int[]{2, 3}) {
            assertTrue(observadas.stream().anyMatch(mensagem ->
                    mensagem.tentativa() == tentativa && "SCHEDULED".equals(mensagem.estado())),
                    "Peek deve observar cada reagendamento real antes da ativacao.");
        }
        var logs = registros();
        var eventosHub = List.of("simtr-hub.dossie-produto.consulta.service.iniciada",
                "simtr-hub.dossie-produto.consulta.simulador.usado",
                "simtr-hub.dossie-produto.consulta.service.concluida");
        for (var consulta : consultas) {
            var logsConsulta = logs.stream().filter(log -> eventosHub.contains(evento(log))
                    && consulta.getTraceId().equals(traceLog(log))).toList();
            assertEquals(eventosHub, logsConsulta.stream().map(SuporteTelemetriaMonitoramento::evento).toList());
            for (var log : logsConsulta) {
                assertEquals(consulta.getSpanId(), log.path("mdc").path("spanId").asText());
            }
        }
        verificarLogsPublicacaoInicial(logs, 15);
        verificarLogsListener(http, 2);
        verificarSemContexto(verificarLogFinal());
        var resultado = verificarResultado("QUARENTENA", "MAXIMO_TENTATIVAS", 3);
        assertEquals("QUARENTENA", resultado.situacaoPreValidacao());
        assertEquals("Rascunho", resultado.situacaoMtr());
        registrarInventarioSanitizado();
    }

    public static class HubProfile extends Perfil {
        @Override
        protected String cenario() {
            return "hub-real";
        }
    }
}
