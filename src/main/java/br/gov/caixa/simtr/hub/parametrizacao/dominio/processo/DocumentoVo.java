package br.gov.caixa.simtr.hub.parametrizacao.dominio.processo;

public record DocumentoVo(
        FuncaoDocumentalVo funcaoDocumental,
        TipoDocumentoVo tipoDocumento,
        Boolean obrigatorio
) {
}
