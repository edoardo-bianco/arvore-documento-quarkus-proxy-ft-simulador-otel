package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo;

import java.util.List;

public record FaseVo(
        Long identificadorNegocial,
        String nome,
        Boolean ativo,
        String ultimaAlteracao,
        Integer ordem,
        String orientacaoUsuario,
        List<ProdutoVo> produtos,
        List<GarantiaVo> garantias,
        List<CampoFormularioVo> camposFormulario,
        List<DocumentoVo> documentos,
        ChecklistReferenciaVo checklist
) {
}
