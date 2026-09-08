package br.gov.caixa.simtr.orquestrador.adaptador.saida.acl.monitoramento;

import jakarta.enterprise.inject.Vetoed;

/**
 * Traduzir os parametros publicos do monitoramento para tipos do orquestrador.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 6.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Implementar ObterParametrosMonitoramento chamando somente a porta pública PrepararMonitoramento.</li>
 * <li>Traduzir o resultado do fornecedor para o record próprio do orquestrador, preservando limite e versão.</li>
 * <li>Manter a operação local/síncrona e somente em memória; não duplicar política, importar configuração ou chamar REST local.</li>
 * <li>Provar o instante fornecido à porta pública e a preservação dos campos na tradução.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 *
 * @see br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento
 * @see br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.PrepararMonitoramento
 */
@Vetoed
public final class ParametrosMonitoramentoAcl {
}
