package br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentoDto(
        @JsonProperty("funcao_documental") FuncaoDocumentalDto funcaoDocumental,
        @JsonProperty("tipo_documento") TipoDocumentoDto tipoDocumento,
        Boolean obrigatorio
) {
}
