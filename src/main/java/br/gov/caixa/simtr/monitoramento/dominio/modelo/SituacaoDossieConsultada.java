package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import jakarta.enterprise.inject.Vetoed;

/**
 * Representar a situacao minima traduzida da API publica do Hub.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 5.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Definir a visão mínima de situação consumida pelo monitoramento.</li>
 * <li>Receber dados traduzidos pela ACL, sem incorporar o modelo completo, DTO ou metadados de transporte do Hub.</li>
 * <li>Detalhar campos necessários e tratamento de situação ausente/desconhecida no item 5.1.</li>
 * <li>Provar a tradução do modelo público e as invariantes do consumidor.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
@Vetoed
public final class SituacaoDossieConsultada {
}
