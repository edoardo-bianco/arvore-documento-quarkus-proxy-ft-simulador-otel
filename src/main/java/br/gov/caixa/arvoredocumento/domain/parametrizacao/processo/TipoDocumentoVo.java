package br.gov.caixa.arvoredocumento.domain.parametrizacao.processo;

public record TipoDocumentoVo(
        String codigoTipologia,
        String nome,
        Boolean permiteReuso,
        Boolean ativo,
        ChecklistReferenciaVo checklist
) {
}
