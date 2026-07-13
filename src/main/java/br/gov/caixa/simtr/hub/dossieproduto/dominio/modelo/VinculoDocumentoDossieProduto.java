package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

public record VinculoDocumentoDossieProduto(
        ClienteDocumentoDossieProduto cliente,
        Long elementoConteudo,
        GarantiaDocumentoDossieProduto garantia
) {
}
