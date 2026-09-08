package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import java.time.Instant;

final class ReagendamentoFixture {

    private ReagendamentoFixture() {
    }

    static TentativaMonitoramento tentativa() {
        return new TentativaMonitoramento("MON-1", "ORQ-1", "pre-externa", "0009223372036854775807", 2,
                Instant.parse("2026-09-04T12:00:00.123Z"), Instant.parse("2026-09-05T12:00:00.123Z"),
                "politica-v7");
    }
}
