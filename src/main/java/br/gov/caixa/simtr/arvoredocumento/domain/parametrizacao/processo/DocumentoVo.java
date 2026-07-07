package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo;

public record DocumentoVo(
        FuncaoDocumentalVo funcaoDocumental,
        TipoDocumentoVo tipoDocumento,
        Boolean obrigatorio
) {
}
