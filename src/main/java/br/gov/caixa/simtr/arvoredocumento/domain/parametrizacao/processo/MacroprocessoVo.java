package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo;

public record MacroprocessoVo(
        Long identificadorNegocial,
        String nome,
        Boolean ativo,
        String ultimaAlteracao
) {
}
