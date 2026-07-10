package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoCriacaoVo(
        Long processo,
        Long chaveCorrelacaoCanal,
        Long numeroNegocio,
        List<DossieProdutoClienteVo> clientes
) {
}
