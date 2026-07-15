package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import java.util.List;

public record ProdutoProcessoParametrizado(
        Long codigoOperacao,
        Long codigoModalidade,
        String nome,
        List<CampoFormularioProcessoParametrizado> camposFormulario,
        List<DocumentoProcessoParametrizado> documentos,
        List<GarantiaProcessoParametrizado> garantias,
        ReferenciaChecklistProcessoParametrizado checklist
) {
}
