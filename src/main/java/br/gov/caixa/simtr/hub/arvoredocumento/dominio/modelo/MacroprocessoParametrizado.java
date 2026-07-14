package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

public record MacroprocessoParametrizado(
        Long identificadorNegocial,
        String nome,
        Boolean ativo,
        String ultimaAlteracao
) {
}
