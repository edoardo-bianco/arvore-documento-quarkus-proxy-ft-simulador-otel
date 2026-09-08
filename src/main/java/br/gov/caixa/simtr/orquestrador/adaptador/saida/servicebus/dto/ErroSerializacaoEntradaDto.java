package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroSerializacaoEntradaDto(
        String recurso,
        @JsonProperty("id_erro") String idErro,
        @JsonProperty("codigo_erro") String codigoErro,
        List<Mensagem> erros,
        String detalhe,
        String stacktrace) {

    public record Mensagem(String mensagem) {
    }
}
