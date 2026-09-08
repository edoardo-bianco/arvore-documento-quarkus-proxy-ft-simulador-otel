package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import jakarta.enterprise.inject.Vetoed;

/**
 * Representar a proxima tentativa e o instante de agendamento; a transacao pertence a borda.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 8.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Definir os dados da próxima tentativa e do instante previsto de agendamento no item 8.1.</li>
 * <li>Preservar os parâmetros da tentativa original e expressar somente a intenção de negócio.</li>
 * <li>Detalhar a associação com a entrega atual na borda; não carregar handles Azure nem definir transação no domínio.</li>
 * <li>Provar preservação de prazo/versão e distinção entre tentativa funcional e redelivery técnica.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
@Vetoed
public final class ReagendamentoMonitoramento {
}
