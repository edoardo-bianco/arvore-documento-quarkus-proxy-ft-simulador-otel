package br.gov.caixa.arvoredocumento.domain.parametrizacao.processo;

import java.util.List;

public record ProcessoVo(
        Long identificadorNegocial,
        String nome,
        Boolean ativo,
        String ultimaAlteracao,
        MacroprocessoVo macroprocesso,
        List<RelacionamentoVo> relacionamentos,
        List<ProdutoVo> produtos,
        List<FaseVo> fases,
        List<DocumentoVo> documentos,
        ChecklistReferenciaVo checklist
) {
}
