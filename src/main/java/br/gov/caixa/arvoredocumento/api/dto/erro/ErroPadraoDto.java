package br.gov.caixa.arvoredocumento.api.dto.erro;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroPadraoDto(
        @JsonProperty("codigo_http") Integer codigoHttp,
        String recurso,
        @JsonProperty("id_erro") String idErro,
        @JsonProperty("codigo_erro") String codigoErro,
        List<ErroMensagemDto> erros,
        String detalhe,
        String stacktrace
) {
}
