package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import java.time.Instant;

final class ResultadoFixture {

    static final String JSON = """
            {"schemaVersion":1,"monitoramentoId":"MON-1","orquestracaoId":"ORQ-1",
             "idDossiePreValidacao":"pre-externa","idDossieMtr":"0009223372036854775807",
             "resultadoMonitoramento":"CONCLUSIVO","situacaoMtr":"PENDENTE_INFORMACAO",
             "situacaoPreValidacao":"NAO_CONFORME","motivo":"SITUACAO_CONCLUSIVA_MTR",
             "tentativasRealizadas":3,"iniciadoEm":"2026-09-04T12:00:00.123Z",
             "concluidoEm":"2026-09-04T18:30:00.123Z","inputSequenceNumber":13527}
            """;

    private ResultadoFixture() {
    }

    static ResultadoMonitoramento resultado() {
        return new ResultadoMonitoramento("MON-1", "ORQ-1", "pre-externa", "0009223372036854775807",
                "CONCLUSIVO", "PENDENTE_INFORMACAO", "NAO_CONFORME", "SITUACAO_CONCLUSIVA_MTR", 3,
                Instant.parse("2026-09-04T12:00:00.123Z"), Instant.parse("2026-09-04T18:30:00.123Z"), 13527);
    }

    static ResultadoMonitoramento quarentena(int tentativas) {
        var origem = resultado();
        return new ResultadoMonitoramento(origem.monitoramentoId(), origem.orquestracaoId(),
                origem.idDossiePreValidacao(), origem.idDossieMtr(), "QUARENTENA", null,
                "QUARENTENA", "PRAZO_MAXIMO", tentativas, origem.iniciadoEm(),
                origem.concluidoEm(), origem.inputSequenceNumber());
    }
}
