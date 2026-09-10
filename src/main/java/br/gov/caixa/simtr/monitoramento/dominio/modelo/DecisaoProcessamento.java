package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import java.time.Duration;
import java.time.Instant;

/** Decisão semântica sem SDK ou settlement; intenção pendente não comprova agendamento. */
public sealed interface DecisaoProcessamento {

    record Ignorar() implements DecisaoProcessamento {
    }

    /** Emitida somente depois da confirmação da porta de publicação. */
    record ResultadoPublicado(ResultadoMonitoramento resultado, PoliticaAplicada politica)
            implements DecisaoProcessamento {
    }

    /**
     * Contador e intervalo decididos, sem reconsultar configuracao ou renovar o prazo.
     * ReagendamentoMonitoramento limita a data ao prazo original antes de executar a porta.
     */
    record ReagendamentoPendente(TentativaMonitoramento tentativa, int proximaTentativa,
            Duration intervalo, Instant processadoEm, PoliticaAplicada politica)
            implements DecisaoProcessamento {
    }

    /** Evidência imutável da resolução, sem transportar a estratégia executável. */
    record PoliticaAplicada(String versaoSolicitada, String versaoEfetiva, boolean padraoAplicado) {
    }
}
