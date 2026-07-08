package br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.checklist;

import java.util.List;

public record ChecklistVo(
        String nome,
        Long identificadorNegocial,
        Integer versao,
        String dataHoraCriacao,
        String dataHoraUltimaAlteracao,
        Boolean verificacaoPrevia,
        String orientacaoOperador,
        List<ChecklistApontamentoVo> apontamentos
) {
}
