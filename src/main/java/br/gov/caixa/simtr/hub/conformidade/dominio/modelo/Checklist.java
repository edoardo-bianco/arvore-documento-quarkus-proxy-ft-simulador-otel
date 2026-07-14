package br.gov.caixa.simtr.hub.conformidade.dominio.modelo;

import java.util.List;

public record Checklist(
        String nome,
        Long identificadorNegocial,
        Integer versao,
        String dataHoraCriacao,
        String dataHoraUltimaAlteracao,
        Boolean verificacaoPrevia,
        String orientacaoOperador,
        List<ApontamentoChecklist> apontamentos
) {
}
