package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

public record DossieProdutoClienteVo(
        String cpf,
        String cnpj,
        Long tipoVinculo,
        DossieProdutoClienteRelacionadoVo clienteRelacionado,
        Integer sequenciaTitularidade
) {
}
