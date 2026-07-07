package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FaseDto(
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        String nome,
        Boolean ativo,
        @JsonProperty("ultima_alteracao") String ultimaAlteracao,
        Integer ordem,
        @JsonProperty("orientacao_usuario") String orientacaoUsuario,
        List<ProdutoDto> produtos,
        List<GarantiaDto> garantias,
        @JsonProperty("campos_formulario") List<CampoFormularioDto> camposFormulario,
        List<DocumentoDto> documentos,
        ChecklistReferenciaDto checklist
) {
}
