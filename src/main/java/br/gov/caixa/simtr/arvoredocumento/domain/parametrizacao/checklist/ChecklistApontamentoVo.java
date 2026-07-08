package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.checklist;

public record ChecklistApontamentoVo(
        Long identificadorNegocial,
        String nome,
        String descricao,
        String orientacaoOperador,
        Boolean indicadorReanalise,
        Integer sequenciaApresentacao
) {
}
