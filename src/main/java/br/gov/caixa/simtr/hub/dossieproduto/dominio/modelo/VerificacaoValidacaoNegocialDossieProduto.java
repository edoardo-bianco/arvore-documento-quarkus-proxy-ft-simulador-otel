package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record VerificacaoValidacaoNegocialDossieProduto(
        Long identificadorInstanciaDocumento,
        Long identificadorChecklist,
        Integer versaoChecklist,
        Boolean analiseRealizada,
        List<ParecerApontamentoValidacaoNegocialDossieProduto> parecerApontamentos,
        GarantiaValidacaoNegocialDossieProduto garantia,
        ProdutoValidacaoNegocialDossieProduto produto,
        Boolean previo
) {
}
