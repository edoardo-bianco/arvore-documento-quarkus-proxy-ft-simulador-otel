package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record RespostaFormularioValidacaoNegocialDossieProduto(
        Long campoFormulario,
        String resposta,
        List<String> opcoesSelecionadas
) {
}
