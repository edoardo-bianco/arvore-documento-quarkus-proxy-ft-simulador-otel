package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GarantiaDto(
        @JsonProperty("codigo_bacen") Long codigoBacen,
        @JsonProperty("nome_garantia") String nomeGarantia,
        Boolean fidejussoria,
        @JsonProperty("campos_formulario") List<CampoFormularioDto> camposFormulario,
        List<DocumentoDto> documentos,
        ChecklistReferenciaDto checklist
) {
}
