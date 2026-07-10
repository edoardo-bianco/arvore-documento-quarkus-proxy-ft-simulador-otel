package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoRespostaFormularioVo(
        Long campoFormulario,
        String resposta,
        List<String> opcoesSelecionadas,
        Boolean excluir
) {
}
