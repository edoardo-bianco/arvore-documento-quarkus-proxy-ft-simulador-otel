package br.gov.caixa.simtr.hub.parametrizacao.dominio.processo;

public record TipoDocumentoVo(
        String codigoTipologia,
        String nome,
        Boolean permiteReuso,
        Boolean permiteMultiplo,
        Boolean ativo,
        ChecklistReferenciaVo checklist
) {
}
