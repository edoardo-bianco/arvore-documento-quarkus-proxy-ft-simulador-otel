package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record AtributoDocumentoDossieProduto(
        String chave,
        String valor,
        String objeto,
        List<String> opcoesSelecionadas
) {
}
