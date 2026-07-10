package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoDocumentoGarantiaVo(
        Integer codigoBacen,
        Integer produtoOperacao,
        Integer produtoModalidade,
        List<DossieProdutoDocumentoClienteVo> clienteAvalista
) {
}
