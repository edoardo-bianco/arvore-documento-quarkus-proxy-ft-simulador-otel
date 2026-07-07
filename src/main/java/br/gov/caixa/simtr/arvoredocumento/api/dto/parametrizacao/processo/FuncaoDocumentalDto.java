package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncaoDocumentalDto(
        String nome,
        @JsonProperty("tipos_documento") List<TipoDocumentoDto> tiposDocumento,
        ChecklistReferenciaDto checklist
) {
}
