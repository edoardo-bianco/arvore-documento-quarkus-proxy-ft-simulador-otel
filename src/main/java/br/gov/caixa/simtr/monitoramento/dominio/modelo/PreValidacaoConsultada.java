package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import jakarta.enterprise.inject.Vetoed;

/**
 * Representar os dados minimos obtidos da pre-validacao.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 5.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Definir somente os dados de pré-validação necessários ao processamento da tentativa.</li>
 * <li>Manter o tipo independente de DTOs do simulador e de modelos de outros componentes.</li>
 * <li>Fixar invariantes e situações ausentes/inválidas junto da porta e do mapper no item 5.1.</li>
 * <li>Provar tradução e casos de ausência/falha; esta classe vazia ainda não representa uma consulta realizada.</li>
 * </ul>
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
@Vetoed
public final class PreValidacaoConsultada {
}
