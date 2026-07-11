package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoValidacaoNegocialRespostaFormularioVo(
        Long campoFormulario,
        String resposta,
        List<String> opcoesSelecionadas
) {
}
