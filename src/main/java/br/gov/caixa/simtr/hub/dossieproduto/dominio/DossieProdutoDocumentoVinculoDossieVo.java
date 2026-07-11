package br.gov.caixa.simtr.hub.dossieproduto.dominio;

public record DossieProdutoDocumentoVinculoDossieVo(
        DossieProdutoDocumentoClienteVo cliente,
        Long elementoConteudo,
        DossieProdutoDocumentoGarantiaVo garantia
) {
}
