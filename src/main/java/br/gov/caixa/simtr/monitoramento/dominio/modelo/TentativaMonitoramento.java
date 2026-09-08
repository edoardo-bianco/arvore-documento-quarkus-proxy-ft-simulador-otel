package br.gov.caixa.simtr.monitoramento.dominio.modelo;

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
