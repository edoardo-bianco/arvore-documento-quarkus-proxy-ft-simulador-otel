package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import java.util.List;

public record FuncaoDocumentalProcessoParametrizado(
        String nome,
        List<TipoDocumentoProcessoParametrizado> tiposDocumento,
        ReferenciaChecklistProcessoParametrizado checklist
) {
}
