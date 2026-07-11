package br.gov.caixa.simtr.hub.dossieproduto.dominio;

public record DossieProdutoClienteVo(
        String cpf,
        String cnpj,
        Long tipoVinculo,
        DossieProdutoClienteRelacionadoVo clienteRelacionado,
        Integer sequenciaTitularidade
) {
}
