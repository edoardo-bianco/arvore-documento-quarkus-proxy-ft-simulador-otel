package br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Contrato de erro proprio da borda de resultado do orquestrador.
 *
 * @param recurso fila de saida configurada
 * @param idErro identidade da ocorrencia
 * @param codigoErro classificacao da falha
 * @param erros mensagens fixas, sem valores rejeitados
 * @param detalhe descricao segura
 * @param stacktrace tipo/frames da falha tecnica, ausente em rejeicoes de contrato
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResultadoDto(
        String recurso,
        @JsonProperty("id_erro") String idErro,
        @JsonProperty("codigo_erro") String codigoErro,
        List<Mensagem> erros,
        String detalhe,
        String stacktrace) {

    /** @param mensagem descricao controlada para o array JSON de erros */
    public record Mensagem(String mensagem) {
    }
}
