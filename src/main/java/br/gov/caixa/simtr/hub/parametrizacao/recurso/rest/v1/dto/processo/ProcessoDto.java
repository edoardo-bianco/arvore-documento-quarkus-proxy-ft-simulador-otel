package br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessoDto(
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        String nome,
        Boolean ativo,
        @JsonProperty("ultima_alteracao") String ultimaAlteracao,
        @JsonProperty("indicador_produto_obrigatorio") Boolean indicadorProdutoObrigatorio,
        MacroprocessoDto macroprocesso,
        List<RelacionamentoDto> relacionamentos,
        List<ProdutoDto> produtos,
        List<FaseDto> fases,
        List<DocumentoDto> documentos,
        ChecklistReferenciaDto checklist
) {
}
