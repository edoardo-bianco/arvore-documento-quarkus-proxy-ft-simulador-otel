package br.gov.caixa.simtr.hub.conformidade.dominio.modelo;

public record ApontamentoChecklist(
        Long identificadorNegocial,
        String nome,
        String descricao,
        String orientacaoOperador,
        Boolean indicadorReanalise,
        Integer sequenciaApresentacao
) {
}
