package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoDocumentoGarantiaVo(
        Integer codigoBacen,
        Integer produtoOperacao,
        Integer produtoModalidade,
        List<DossieProdutoDocumentoClienteVo> clienteAvalista
) {
}
