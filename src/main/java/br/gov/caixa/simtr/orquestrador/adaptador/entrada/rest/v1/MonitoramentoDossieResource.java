package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1;

import jakarta.enterprise.inject.Vetoed;

/**
 * Expor a entrada REST somente quando a fatia funcional estiver pronta.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 6.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Habilitar o POST aprovado apenas com a fatia funcional, validando o request e usando o mapper REST existente.</li>
 * <li>Acionar a porta de início e produzir 202 depois da confirmação da publicação; preservar path, JSON e segurança aprovados.</li>
 * <li>Traduzir falhas pelo contrato de erro da borda, sem expor broker/credencial nem alterar erros do Hub.</li>
 * <li>Provar request válido/inválido, resposta, falha de publicação e OpenAPI; não chamar publisher ou caso de uso concreto diretamente.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 *
 * @see br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.IniciarMonitoramento
 * @see br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.MonitoramentoDossieRestMapper
 */
@Vetoed
public final class MonitoramentoDossieResource {
}
