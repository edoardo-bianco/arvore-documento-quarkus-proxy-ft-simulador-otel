package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

public record TipoDocumentoProcessoParametrizado(
        String codigoTipologia,
        String nome,
        Boolean permiteReuso,
        Boolean permiteMultiplo,
        Boolean ativo,
        ReferenciaChecklistProcessoParametrizado checklist
) {
}
