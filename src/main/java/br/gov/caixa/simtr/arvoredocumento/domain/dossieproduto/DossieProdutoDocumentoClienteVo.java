package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

public record DossieProdutoDocumentoClienteVo(
        String cpf,
        String cnpj,
        Long tipoVinculo
) {
}
