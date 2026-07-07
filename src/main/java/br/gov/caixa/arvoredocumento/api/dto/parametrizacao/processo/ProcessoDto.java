package br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessoDto(
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        String nome,
        Boolean ativo,
        @JsonProperty("ultima_alteracao") String ultimaAlteracao,
        MacroprocessoDto macroprocesso,
        List<RelacionamentoDto> relacionamentos,
        List<ProdutoDto> produtos,
        List<FaseDto> fases,
        List<DocumentoDto> documentos,
        ChecklistReferenciaDto checklist
) {
}
