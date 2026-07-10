package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoVinculoDossieVo(
        Long fase,
        DossieProdutoVinculoClienteVo cliente,
        DossieProdutoVinculoProdutoVo produto,
        DossieProdutoVinculoGarantiaVo garantia,
        List<DossieProdutoRespostaFormularioVo> respostasFormulario
) {
}
