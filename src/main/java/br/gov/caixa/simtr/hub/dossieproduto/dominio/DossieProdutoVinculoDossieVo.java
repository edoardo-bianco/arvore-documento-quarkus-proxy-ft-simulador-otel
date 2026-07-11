package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoVinculoDossieVo(
        Long fase,
        DossieProdutoVinculoClienteVo cliente,
        DossieProdutoVinculoProdutoVo produto,
        DossieProdutoVinculoGarantiaVo garantia,
        List<DossieProdutoRespostaFormularioVo> respostasFormulario
) {
}
