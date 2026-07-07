package br.gov.caixa.arvoredocumento.domain.parametrizacao.processo;

import java.util.List;

public record FuncaoDocumentalVo(
        String nome,
        List<TipoDocumentoVo> tiposDocumento,
        ChecklistReferenciaVo checklist
) {
}
