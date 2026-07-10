package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoVinculoGarantiaVo(
        Integer codigoBacen,
        Integer produtoOperacao,
        Integer produtoModalidade,
        List<DossieProdutoClienteAvalistaVo> clientesAvalistas
) {
}
