package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoDocumentoInclusaoVo(
        Long id,
        String pathStorage,
        String codigoGed,
        String objectStoreGed,
        String tipoDocumento,
        DossieProdutoDocumentoVinculoDossieVo vinculoDossie,
        List<DossieProdutoDocumentoAtributoVo> atributos,
        List<DossieProdutoDocumentoPropriedadeVo> propriedades
) {
}
