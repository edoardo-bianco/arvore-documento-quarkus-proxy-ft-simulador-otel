package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record ComandoInclusaoDocumentoDossieProduto(
        Long identificadorDossieProduto,
        Long identificadorDocumento,
        String caminhoArmazenamento,
        String codigoGed,
        String repositorioGed,
        String tipoDocumento,
        VinculoDocumentoDossieProduto vinculoDossie,
        List<AtributoDocumentoDossieProduto> atributos,
        List<PropriedadeDocumentoDossieProduto> propriedades
) {
}
