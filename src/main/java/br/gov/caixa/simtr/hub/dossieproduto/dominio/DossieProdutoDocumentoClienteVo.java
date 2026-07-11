package br.gov.caixa.simtr.hub.dossieproduto.dominio;

public record DossieProdutoDocumentoClienteVo(
        String cpf,
        String cnpj,
        Long tipoVinculo
) {
}
