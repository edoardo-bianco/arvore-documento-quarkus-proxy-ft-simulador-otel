package br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto;

import java.util.List;

public record DossieProdutoDocumentoAtributoVo(
        String chave,
        String valor,
        String objeto,
        List<String> opcoesSelecionadas
) {
}
