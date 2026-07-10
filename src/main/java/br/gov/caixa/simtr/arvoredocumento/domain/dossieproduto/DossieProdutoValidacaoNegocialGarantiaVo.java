package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoValidacaoNegocialGarantiaVo(
        Integer codigoBacen,
        List<DossieProdutoValidacaoNegocialClienteAvalistaVo> clientesAvalistas
) {
}
