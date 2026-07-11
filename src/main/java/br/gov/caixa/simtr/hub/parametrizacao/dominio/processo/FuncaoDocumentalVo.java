package br.gov.caixa.simtr.hub.parametrizacao.dominio.processo;

import java.util.List;

public record FuncaoDocumentalVo(
        String nome,
        List<TipoDocumentoVo> tiposDocumento,
        ChecklistReferenciaVo checklist
) {
}
