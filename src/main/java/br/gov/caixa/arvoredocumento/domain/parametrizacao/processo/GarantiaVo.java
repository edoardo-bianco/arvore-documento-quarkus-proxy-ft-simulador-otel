package br.gov.caixa.arvoredocumento.domain.parametrizacao.processo;

import java.util.List;

public record GarantiaVo(
        Long codigoBacen,
        String nomeGarantia,
        Boolean fidejussoria,
        List<CampoFormularioVo> camposFormulario,
        List<DocumentoVo> documentos,
        ChecklistReferenciaVo checklist
) {
}
