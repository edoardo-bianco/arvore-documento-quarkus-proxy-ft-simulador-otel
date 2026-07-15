package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record GarantiaDocumentoDossieProduto(
        Integer codigoBacen,
        Integer produtoOperacao,
        Integer produtoModalidade,
        List<ClienteDocumentoDossieProduto> clientesAvalistas
) {
}
