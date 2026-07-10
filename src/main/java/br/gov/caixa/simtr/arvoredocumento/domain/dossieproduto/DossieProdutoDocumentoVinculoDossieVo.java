package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

public record DossieProdutoDocumentoVinculoDossieVo(
        DossieProdutoDocumentoClienteVo cliente,
        Long elementoConteudo,
        DossieProdutoDocumentoGarantiaVo garantia
) {
}
