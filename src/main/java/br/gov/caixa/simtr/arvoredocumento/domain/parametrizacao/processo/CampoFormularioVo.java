package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo;

import java.util.List;

public record CampoFormularioVo(
        Long identificadorNegocial,
        String label,
        Boolean obrigatorio,
        Boolean ativo,
        String exibicaoCondicional,
        Integer tamanhoApresentacao,
        Integer ordemApresentacao,
        String tipo,
        String mascara,
        String placeholder,
        Integer tamanhoMinimo,
        Integer tamanhoMaximo,
        String orientacaoPreenchimento,
        Boolean bloquearEdicao,
        List<OpcaoDisponivelVo> opcoesDisponiveis
) {
}
