package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoRespostaFormularioVo(
        Long campoFormulario,
        String resposta,
        List<String> opcoesSelecionadas,
        Boolean excluir
) {
}
