package br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso;

import jakarta.enterprise.inject.Vetoed;

/**
 * Coordenar parametros iniciais, geracao de IDs e publicacao.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 6.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Implementar a porta de início, gerar IDs e o instante inicial no servidor e obter limite/versão pela porta de parâmetros.</li>
 * <li>Montar a tentativa no modelo do orquestrador e publicar pela porta de saída; concluir o início apenas após confirmação.</li>
 * <li>Não importar política/configuração do monitoramento nem tipos Azure; a tradução entre componentes pertence à ACL.</li>
 * <li>Provar geração e preservação dos identificadores, ordem de consulta/publicação e propagação da falha do broker.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 *
 * @see br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.IniciarMonitoramento
 * @see br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento
 * @see br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.PublicarTentativaMonitoramento
 */
@Vetoed
public final class IniciarMonitoramentoUseCase {
}
