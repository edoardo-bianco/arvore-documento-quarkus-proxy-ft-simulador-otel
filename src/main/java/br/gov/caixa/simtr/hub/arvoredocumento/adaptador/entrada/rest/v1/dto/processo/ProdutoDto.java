package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProdutoDto(
        @JsonProperty("codigo_operacao") Long codigoOperacao,
        @JsonProperty("codigo_modalidade") Long codigoModalidade,
        String nome,
        @JsonProperty("campos_formulario") List<CampoFormularioDto> camposFormulario,
        List<DocumentoDto> documentos,
        List<GarantiaDto> garantias,
        ChecklistReferenciaDto checklist
) {
}
