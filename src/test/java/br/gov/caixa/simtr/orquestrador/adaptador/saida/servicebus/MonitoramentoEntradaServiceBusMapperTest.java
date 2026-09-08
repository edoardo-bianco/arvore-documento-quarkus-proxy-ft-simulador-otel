package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class MonitoramentoEntradaServiceBusMapperTest {

    @Inject
    ObjectMapper json;

    @Inject
    br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.MonitoramentoEntradaServiceBusMapper consumidor;

    @Test
    void deveMontarContratoV1ComStringsEDatasIsoSemCamposDeTransporte() throws Exception {
        var tentativa = tentativa();

        var mensagem = MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, json);

        assertEquals(json.readTree("""
                {"schemaVersion":1,"monitoramentoId":"MON-1","orquestracaoId":"ORQ-1",
                 "idDossiePreValidacao":"pre-externa","idDossieMtr":"0009223372036854775807",
                 "tentativaAtual":1,"iniciadoEm":"2026-09-04T12:00:00.123Z",
                 "limiteEm":"2026-09-05T12:00:00.123Z","politicaMonitoramentoVersao":"politica-v7"}
                """), json.readTree(mensagem.getBody().toString()));
    }

    @Test
    void deveMontarPropriedadesAmqpDeterministicasSemAgendar() {
        var tentativa = tentativa();

        var primeira = MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, json);
        var repetida = MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, json);

        assertEquals("MON-1:tentativa:1", primeira.getMessageId());
        assertEquals("ORQ-1", primeira.getCorrelationId());
        assertEquals("MONITORAR_DOSSIE_MTR", primeira.getSubject());
        assertEquals("application/json", primeira.getContentType());
        assertNull(primeira.getScheduledEnqueueTime());
        assertEquals(primeira.getMessageId(), repetida.getMessageId());
        assertEquals(primeira.getBody().toString(), repetida.getBody().toString());
    }

    @Test
    void deveSerCompativelComContratoIndependenteDoConsumidor() {
        var tentativa = tentativa();
        var mensagem = MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, json);

        var recebida = consumidor.paraTentativa(mensagem.getBody().toString(), mensagem.getMessageId(),
                mensagem.getCorrelationId(), mensagem.getSubject(), mensagem.getContentType());

        assertEquals(new br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento(
                "MON-1", "ORQ-1", "pre-externa", "0009223372036854775807", 1,
                Instant.parse("2026-09-04T12:00:00.123Z"), Instant.parse("2026-09-05T12:00:00.123Z"),
                "politica-v7"), recebida);
    }

    private static TentativaMonitoramento tentativa() {
        return new TentativaMonitoramento("MON-1", "ORQ-1", "pre-externa", "0009223372036854775807", 1,
                Instant.parse("2026-09-04T12:00:00.123Z"), Instant.parse("2026-09-05T12:00:00.123Z"),
                "politica-v7");
    }
}
