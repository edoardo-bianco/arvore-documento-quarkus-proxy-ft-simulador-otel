package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import java.util.List;

public record GarantiaProcessoParametrizado(
        Long codigoBacen,
        String nomeGarantia,
        Boolean fidejussoria,
        List<CampoFormularioProcessoParametrizado> camposFormulario,
        List<DocumentoProcessoParametrizado> documentos,
        ReferenciaChecklistProcessoParametrizado checklist
) {
}
