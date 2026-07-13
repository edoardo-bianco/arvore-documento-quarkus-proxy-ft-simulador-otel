package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record RespostaFormularioDossieProduto(
        Long campoFormulario,
        String resposta,
        List<String> opcoesSelecionadas,
        Boolean excluir
) {
}
