package br.gov.caixa.simtr.orquestrador.adaptador.saida.log;

import jakarta.enterprise.inject.Vetoed;

/**
 * Implementar o registro do resultado sem alterar logs do Hub.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 9.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Implementar a porta de registro do resultado com os campos/evento aprovados para o novo componente.</li>
 * <li>Conservar correlação por monitoramentoId/orquestracaoId, sem registrar payload, dados sensíveis ou credenciais.</li>
 * <li>Concluir a operação após o registro; não executar settlement nem alterar logger, helper ou erro do Hub.</li>
 * <li>Provar o conteúdo real do log e a propagação de falha para o listener decidir a entrega.</li>
 * </ul>
 *
 * <p><strong>Fluxo aprovado a implementar:</strong> Implementar {@code RegistrarResultadoMonitoramento.executar} com o evento {@code orquestrador.monitoramento-dossie.resultado.registrado} e os campos aprovados. O log é o efeito final após o consumo da fila de saída; o listener permanece responsável pelo Complete. A emissão não altera configuração, código, logger, erros ou contratos do Hub.
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md}.
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 *
 * @see br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.RegistrarResultadoMonitoramento
 */
@Vetoed
public final class ResultadoMonitoramentoLogAdapter {
}
