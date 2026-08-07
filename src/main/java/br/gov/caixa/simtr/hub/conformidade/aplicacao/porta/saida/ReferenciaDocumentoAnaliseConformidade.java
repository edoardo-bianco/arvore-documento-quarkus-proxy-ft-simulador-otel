package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import java.util.regex.Pattern;

public record ReferenciaDocumentoAnaliseConformidade(
        String documentoRef,
        String hashConteudo,
        Short versaoSchema) {

    public static final Short VERSAO_INICIAL = 1;
    private static final Pattern SHA_256_HEXADECIMAL_MINUSCULO =
            Pattern.compile("[0-9a-f]{64}");

    public ReferenciaDocumentoAnaliseConformidade {
        if (documentoRef == null || documentoRef.isBlank()) {
            throw new IllegalArgumentException("documentoRef obrigatório");
        }
        if (hashConteudo == null
                || !SHA_256_HEXADECIMAL_MINUSCULO.matcher(hashConteudo).matches()) {
            throw new IllegalArgumentException("hashConteudo deve ser SHA-256 hexadecimal minúsculo");
        }
        if (!VERSAO_INICIAL.equals(versaoSchema)) {
            throw new IllegalArgumentException("versaoSchema não suportada");
        }
    }
}
