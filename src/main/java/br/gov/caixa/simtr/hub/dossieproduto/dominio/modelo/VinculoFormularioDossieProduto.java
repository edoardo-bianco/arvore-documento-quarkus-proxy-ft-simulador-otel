package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record VinculoFormularioDossieProduto(
        Long fase,
        ClienteFormularioDossieProduto cliente,
        ProdutoFormularioDossieProduto produto,
        GarantiaFormularioDossieProduto garantia,
        List<RespostaFormularioDossieProduto> respostasFormulario
) {
}
