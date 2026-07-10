package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoValidacaoNegocialVerificacaoVo(
        Long identificadorInstanciaDocumento,
        Long identificadorChecklist,
        Integer versaoChecklist,
        Boolean analiseRealizada,
        List<DossieProdutoValidacaoNegocialParecerApontamentoVo> parecerApontamentos,
        DossieProdutoValidacaoNegocialGarantiaVo garantia,
        DossieProdutoValidacaoNegocialProdutoVo produto,
        Boolean previo
) {
}
