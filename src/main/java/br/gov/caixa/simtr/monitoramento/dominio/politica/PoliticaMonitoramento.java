package br.gov.caixa.simtr.monitoramento.dominio.politica;

import java.time.Duration;
import java.time.Instant;

public interface PoliticaMonitoramento {

    String versao();

    Instant calcularLimite(Instant iniciadoEm);

    Decisao avaliarTentativaNaoConclusiva(
            int tentativaAtual,
            Instant processadoEm,
            Instant limiteEm);

    sealed interface Decisao {

        record Reagendar(int proximaTentativa, Duration intervalo) implements Decisao {
        }

        record Encerrar(MotivoEncerramento motivo) implements Decisao {
        }
    }

    enum MotivoEncerramento {
        PRAZO_MAXIMO,
        MAXIMO_TENTATIVAS
    }
}
