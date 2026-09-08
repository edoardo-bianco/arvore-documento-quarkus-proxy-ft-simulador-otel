package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Erro tecnico proprio da borda de reagendamento, com campos previamente sanitizados.
 *
 * @param recurso fila de entrada configurada
 * @param idErro identidade da ocorrencia
 * @param codigoErro classificacao local estavel
 * @param erros mensagens fixas, sem valores rejeitados
 * @param detalhe descricao segura da falha
 * @param stacktrace somente tipo e frames da falha de serializacao, ausente na validacao
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroReagendamentoDto(
        String recurso,
        @JsonProperty("id_erro") String idErro,
        @JsonProperty("codigo_erro") String codigoErro,
        List<Mensagem> erros,
        String detalhe,
        String stacktrace) {

    /**
     * @param mensagem descricao segura para o array de erros do log
     */
    public record Mensagem(String mensagem) {
    }
}
