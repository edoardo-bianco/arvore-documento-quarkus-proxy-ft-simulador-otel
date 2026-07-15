package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import java.util.List;

public record ProcessoParametrizado(
        Long identificadorNegocial,
        String nome,
        Boolean ativo,
        String ultimaAlteracao,
        Boolean indicadorProdutoObrigatorio,
        MacroprocessoParametrizado macroprocesso,
        List<RelacionamentoProcessoParametrizado> relacionamentos,
        List<ProdutoProcessoParametrizado> produtos,
        List<FaseProcessoParametrizado> fases,
        List<DocumentoProcessoParametrizado> documentos,
        ReferenciaChecklistProcessoParametrizado checklist
) {
}
