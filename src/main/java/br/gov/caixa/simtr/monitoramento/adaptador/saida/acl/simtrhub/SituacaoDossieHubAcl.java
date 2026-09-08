package br.gov.caixa.simtr.monitoramento.adaptador.saida.acl.simtrhub;

import jakarta.enterprise.inject.Vetoed;

/**
 * Traduzir identificador e situacao pela porta publica do Hub.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 5.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Implementar a porta do consumidor usando somente ConsultarDossieProduto e os tipos públicos referenciados por essa API.</li>
 * <li>Converter o identificador MTR validado para Long positivo e traduzir a situação mínima para modelo próprio.</li>
 * <li>Não importar DTO REST/MTR, Resource, caso de uso concreto ou porta de saída do Hub; não fazer chamada HTTP local.</li>
 * <li>Provar limites do identificador, tradução, falhas e dependências permitidas com ArchUnit.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 *
 * @see br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarSituacaoDossie
 * @see br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto
 */
@Vetoed
public final class SituacaoDossieHubAcl {
}
