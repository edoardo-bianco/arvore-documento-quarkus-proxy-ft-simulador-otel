package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoValidacaoNegocialVo(
        List<DossieProdutoValidacaoNegocialVerificacaoVo> verificacoes,
        List<DossieProdutoValidacaoNegocialRespostaFormularioVo> respostasFormulario
) {
}
