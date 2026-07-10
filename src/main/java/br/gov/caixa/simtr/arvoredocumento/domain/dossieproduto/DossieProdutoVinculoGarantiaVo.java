package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoVinculoGarantiaVo(
        Integer codigoBacen,
        Integer produtoOperacao,
        Integer produtoModalidade,
        List<DossieProdutoClienteAvalistaVo> clientesAvalistas
) {
}
