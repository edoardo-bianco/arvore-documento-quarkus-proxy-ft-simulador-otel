package br.gov.caixa.simtr.hub.parametrizacao.dominio.checklist;

public record ChecklistApontamentoVo(
        Long identificadorNegocial,
        String nome,
        String descricao,
        String orientacaoOperador,
        Boolean indicadorReanalise,
        Integer sequenciaApresentacao
) {
}
