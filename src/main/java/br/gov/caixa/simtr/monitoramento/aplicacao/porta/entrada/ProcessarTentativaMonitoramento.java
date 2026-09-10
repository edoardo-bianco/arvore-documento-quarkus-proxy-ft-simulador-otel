package br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import io.smallrye.mutiny.Uni;

/** Processa uma tentativa pelas portas de consulta, política e publicação do monitoramento. */
public interface ProcessarTentativaMonitoramento {

    /**
     * Pré-validação não elegível produz no-op. Limites esgotados e situações conclusivas
     * produzem resultado confirmado; nao conclusivo elegivel produz intencao para a borda de reagendamento.
     * Falhas técnicas propagam sem retry adicional. Settlement pertence à borda.
     *
     * @param tentativa tentativa validada pela borda, preservando início, limite e versão
     * @param inputSequenceNumber sequência escalar da entrega; não é contador funcional
     * @return processamento adiado e memorizado por invocação; somente ResultadoPublicado
     *         comprova a confirmação de envio, sem comprovar Complete ou persistência
     */
    Uni<DecisaoProcessamento> executar(TentativaMonitoramento tentativa, long inputSequenceNumber);
}
