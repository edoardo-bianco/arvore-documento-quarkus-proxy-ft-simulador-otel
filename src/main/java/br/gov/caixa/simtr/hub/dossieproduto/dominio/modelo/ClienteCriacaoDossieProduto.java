package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

public record ClienteCriacaoDossieProduto(
        String cpf,
        String cnpj,
        Long tipoVinculo,
        ClienteRelacionadoCriacaoDossieProduto clienteRelacionado,
        Integer sequenciaTitularidade
) {
}
