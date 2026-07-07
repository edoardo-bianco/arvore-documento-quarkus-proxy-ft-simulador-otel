package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo;

import java.util.List;

public record ProdutoVo(
        Long codigoOperacao,
        Long codigoModalidade,
        String nome,
        List<CampoFormularioVo> camposFormulario,
        List<DocumentoVo> documentos,
        List<GarantiaVo> garantias,
        ChecklistReferenciaVo checklist
) {
}
