package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import java.util.List;

public record CampoFormularioProcessoParametrizado(
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
        List<OpcaoDisponivelProcessoParametrizado> opcoesDisponiveis
) {
}
