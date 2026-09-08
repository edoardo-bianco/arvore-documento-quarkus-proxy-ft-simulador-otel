package br.gov.caixa.simtr.orquestrador.dominio.modelo;

import java.time.Instant;

public record TentativaMonitoramento(
        String monitoramentoId,
        String orquestracaoId,
        String idDossiePreValidacao,
        String idDossieMtr,
        int tentativaAtual,
        Instant iniciadoEm,
        Instant limiteEm,
        String politicaMonitoramentoVersao) {
}
