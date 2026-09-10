package br.gov.caixa.simtr.monitoramento.dominio.politica;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface PoliticaMonitoramento {

    String versao();

    Instant calcularLimite(Instant iniciadoEm);

    /** Avalia o prazo original e a contagem já realizada; zero permite a primeira consulta. */
    Optional<MotivoEncerramento> motivoEncerramento(
            int tentativasRealizadas, Instant processadoEm, Instant limiteEm);

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
