package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

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
