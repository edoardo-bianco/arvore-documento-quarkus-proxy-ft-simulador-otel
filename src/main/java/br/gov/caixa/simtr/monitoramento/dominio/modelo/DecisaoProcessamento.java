package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import jakarta.enterprise.inject.Vetoed;

/**
 * Expressar a decisao semantica para a borda, sem handles de settlement.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 7.1/8.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Definir as alternativas semânticas de decisão nos itens 7.1/8.1, alinhadas à política e ao contrato de resultado.</li>
 * <li>Esclarecer a divisão entre efeitos coordenados pela aplicação e operações executadas pelos adapters antes de implementar a decisão.</li>
 * <li>Não armazenar cliente, mensagem Azure, contexto de entrega ou handle de settlement neste tipo.</li>
 * <li>Provar decisões terminais/não conclusivas e suas falhas; a classe vazia não representa conclusão de processamento.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
@Vetoed
public final class DecisaoProcessamento {
}
