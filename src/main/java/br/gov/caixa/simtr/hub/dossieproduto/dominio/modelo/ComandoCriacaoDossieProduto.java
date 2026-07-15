package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record ComandoCriacaoDossieProduto(
        Long processo,
        Long chaveCorrelacaoCanal,
        Long numeroNegocio,
        List<ClienteCriacaoDossieProduto> clientes
) {
}
