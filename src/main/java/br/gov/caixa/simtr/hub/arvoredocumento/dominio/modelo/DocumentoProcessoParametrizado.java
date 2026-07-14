package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

public record DocumentoProcessoParametrizado(
        FuncaoDocumentalProcessoParametrizado funcaoDocumental,
        TipoDocumentoProcessoParametrizado tipoDocumento,
        Boolean obrigatorio
) {
}
