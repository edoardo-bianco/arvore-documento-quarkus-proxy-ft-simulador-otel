package br.gov.caixa.simtr.hub.dossieproduto.dominio;

import java.util.List;

public record DossieProdutoDocumentoAtributoVo(
        String chave,
        String valor,
        String objeto,
        List<String> opcoesSelecionadas
) {
}
