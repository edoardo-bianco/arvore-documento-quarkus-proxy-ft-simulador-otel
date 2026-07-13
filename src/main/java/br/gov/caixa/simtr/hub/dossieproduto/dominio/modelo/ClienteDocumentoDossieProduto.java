package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

public record ClienteDocumentoDossieProduto(
        String cpf,
        String cnpj,
        Long tipoVinculo
) {
}
