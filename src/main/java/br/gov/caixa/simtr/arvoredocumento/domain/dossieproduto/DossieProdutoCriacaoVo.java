package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoCriacaoVo(
        Long processo,
        Long chaveCorrelacaoCanal,
        Long numeroNegocio,
        List<DossieProdutoClienteVo> clientes
) {
}
