package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import java.util.List;

public record FaseProcessoParametrizado(
        Long identificadorNegocial,
        String nome,
        Boolean ativo,
        String ultimaAlteracao,
        Integer ordem,
        String orientacaoUsuario,
        List<ProdutoProcessoParametrizado> produtos,
        List<GarantiaProcessoParametrizado> garantias,
        List<CampoFormularioProcessoParametrizado> camposFormulario,
        List<DocumentoProcessoParametrizado> documentos,
        List<ReferenciaChecklistProcessoParametrizado> checklist
) {
}
