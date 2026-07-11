package br.gov.caixa.simtr.hub.dossieproduto.dominio;

public record DossieProdutoVinculoClienteVo(
        String cpf,
        String cnpj,
        Long tipoVinculo
) {
}
