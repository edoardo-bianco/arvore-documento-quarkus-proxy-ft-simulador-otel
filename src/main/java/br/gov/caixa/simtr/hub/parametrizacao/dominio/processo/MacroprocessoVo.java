package br.gov.caixa.simtr.hub.parametrizacao.dominio.processo;

public record MacroprocessoVo(
        Long identificadorNegocial,
        String nome,
        Boolean ativo,
        String ultimaAlteracao
) {
}
