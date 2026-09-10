package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Proxima tentativa e horario dentro da janela original; transacao pertence a borda. */
public record ReagendamentoMonitoramento(TentativaMonitoramento proximaTentativa, Instant agendadoEm) {
    public ReagendamentoMonitoramento {
        Objects.requireNonNull(proximaTentativa, "proximaTentativa");
        Objects.requireNonNull(agendadoEm, "agendadoEm");
        if (agendadoEm.isBefore(proximaTentativa.iniciadoEm())
                || agendadoEm.isAfter(proximaTentativa.limiteEm())) {
            throw new IllegalArgumentException("Agendamento fora da janela original.");
        }
    }

    public static ReagendamentoMonitoramento aPartirDe(DecisaoProcessamento.ReagendamentoPendente decisao) {
        var atual = decisao.tentativa();
        var intervalo = decisao.intervalo();
        if (intervalo.isNegative() || intervalo.isZero()
                || !decisao.processadoEm().isBefore(atual.limiteEm())
                || decisao.proximaTentativa() != (long) atual.tentativaAtual() + 1) {
            throw new IllegalArgumentException("Decisao de reagendamento invalida.");
        }
        // Comparar antes de somar tambem protege contra intervalos que excedem Instant.
        var restante = Duration.between(decisao.processadoEm(), atual.limiteEm());
        var horario = intervalo.compareTo(restante) >= 0
                ? atual.limiteEm() : decisao.processadoEm().plus(intervalo);
        var proxima = new TentativaMonitoramento(atual.monitoramentoId(), atual.orquestracaoId(),
                atual.idDossiePreValidacao(), atual.idDossieMtr(), decisao.proximaTentativa(),
                atual.iniciadoEm(), atual.limiteEm(), atual.politicaMonitoramentoVersao());
        return new ReagendamentoMonitoramento(proxima, horario);
    }
}
