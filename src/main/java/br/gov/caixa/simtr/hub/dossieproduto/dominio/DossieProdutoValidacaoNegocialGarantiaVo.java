package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoValidacaoNegocialGarantiaVo(
        Integer codigoBacen,
        List<DossieProdutoValidacaoNegocialClienteAvalistaVo> clientesAvalistas
) {
}
