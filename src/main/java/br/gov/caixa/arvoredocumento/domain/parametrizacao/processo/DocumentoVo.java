package br.gov.caixa.arvoredocumento.domain.parametrizacao.processo;

public record DocumentoVo(
        FuncaoDocumentalVo funcaoDocumental,
        TipoDocumentoVo tipoDocumento,
        Boolean obrigatorio
) {
}
