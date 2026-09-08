package br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao;

import jakarta.enterprise.inject.Vetoed;

/**
 * Atender a consulta com cenarios simulados explicitamente habilitados.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 5.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Implementar a porta de consulta com cenários determinísticos de pré-validação.</li>
 * <li>Usar DTO/mapper próprios do simulador e traduzir somente os dados necessários ao consumidor.</li>
 * <li>Exigir habilitação explícita, identificar origem simulada e impedir fallback silencioso em produção.</li>
 * <li>Provar cenários, tradução, seleção de configuração e falhas sem importar fixtures ou adapters internos do Hub.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 *
 * @see br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarPreValidacao
 */
@Vetoed
public final class PreValidacaoSimuladaAdapter {
}
