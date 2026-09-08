package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada;
import io.smallrye.mutiny.Uni;

/**
 * Consulta a situação do dossiê pela API pública existente do Hub.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 5.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Implementar em SituacaoDossieHubAcl: traduzir o identificador para a porta pública ConsultarDossieProduto e mapear a situação para o modelo próprio do monitoramento. Preservar o package dossie, o Hub e seu simulador existente.
 *
 * <p><strong>Verificação prevista:</strong> Provar limites do identificador e tradução de situação, sem HTTP local nem dependência em Resource, DTO MTR/REST ou caso de uso concreto do Hub.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface ConsultarSituacaoDossie {

    /**
     * Consulta a situação do dossiê pela API pública existente do Hub.
     *
     * <p>Comportamento a implementar no item 5.1: Implementar em SituacaoDossieHubAcl: traduzir o identificador para a porta pública ConsultarDossieProduto e mapear a situação para o modelo próprio do monitoramento. Preservar o package dossie, o Hub e seu simulador existente.
     *
     * @param idDossieMtr identificador decimal positivo em string, no intervalo aceito de Long; conversão pertence à ACL
     * @return situação traduzida para o tipo próprio; ausência e falhas devem ser detalhadas no item 5.1
     */
    Uni<SituacaoDossieConsultada> executar(String idDossieMtr);
}
