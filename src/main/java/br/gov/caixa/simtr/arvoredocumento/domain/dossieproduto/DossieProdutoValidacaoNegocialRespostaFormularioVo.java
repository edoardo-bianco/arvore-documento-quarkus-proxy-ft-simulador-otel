package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoValidacaoNegocialRespostaFormularioVo(
        Long campoFormulario,
        String resposta,
        List<String> opcoesSelecionadas
) {
}
