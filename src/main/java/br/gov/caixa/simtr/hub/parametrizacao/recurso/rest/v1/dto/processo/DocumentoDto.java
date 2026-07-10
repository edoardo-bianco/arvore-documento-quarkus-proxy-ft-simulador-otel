package br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentoDto(
        @JsonProperty("funcao_documental") FuncaoDocumentalDto funcaoDocumental,
        @JsonProperty("tipo_documento") TipoDocumentoDto tipoDocumento,
        Boolean obrigatorio
) {
}
