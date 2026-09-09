package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Contrato REST da propria borda, compativel com o formato publico de erro existente. */
public record ErroInicioMonitoramentoDto(
        @JsonProperty("codigo_http") int codigoHttp,
        String recurso,
        @JsonProperty("id_erro") String idErro,
        @JsonProperty("codigo_erro") String codigoErro,
        List<Mensagem> erros) {

    public record Mensagem(String mensagem) {
    }
}
