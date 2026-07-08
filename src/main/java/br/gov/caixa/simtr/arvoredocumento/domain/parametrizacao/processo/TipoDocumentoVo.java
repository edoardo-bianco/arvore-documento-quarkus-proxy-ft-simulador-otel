package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo;

public record TipoDocumentoVo(
        String codigoTipologia,
        String nome,
        Boolean permiteReuso,
        Boolean permiteMultiplo,
        Boolean ativo,
        ChecklistReferenciaVo checklist
) {
}
