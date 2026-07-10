package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoValidacaoNegocialVo(
        List<DossieProdutoValidacaoNegocialVerificacaoVo> verificacoes,
        List<DossieProdutoValidacaoNegocialRespostaFormularioVo> respostasFormulario
) {
}
