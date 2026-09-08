package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.dto;

import java.time.Instant;

public record MonitorarDossieMtrV1(
        int schemaVersion,
        String monitoramentoId,
        String orquestracaoId,
        String idDossiePreValidacao,
        String idDossieMtr,
        int tentativaAtual,
        Instant iniciadoEm,
        Instant limiteEm,
        String politicaMonitoramentoVersao) {
}
