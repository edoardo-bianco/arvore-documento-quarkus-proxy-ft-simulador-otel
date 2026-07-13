package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record GarantiaFormularioDossieProduto(
        Integer codigoBacen,
        Integer produtoOperacao,
        Integer produtoModalidade,
        List<ClienteAvalistaFormularioDossieProduto> clientesAvalistas
) {
}
