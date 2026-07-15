package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

public record ClienteFormularioDossieProduto(
        String cpf,
        String cnpj,
        Long tipoVinculo
) {
}
