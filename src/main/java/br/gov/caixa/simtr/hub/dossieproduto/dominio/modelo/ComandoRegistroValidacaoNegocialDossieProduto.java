package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record ComandoRegistroValidacaoNegocialDossieProduto(
        Long identificadorDossieProduto,
        List<VerificacaoValidacaoNegocialDossieProduto> verificacoes,
        List<RespostaFormularioValidacaoNegocialDossieProduto> respostasFormulario
) {
}
