package br.gov.caixa.simtr.hub.gestaodocumento.dominio;

public record GestaoDocumentoCredencialContainerVo(
        String sas,
        Object validade,
        String urlStorage,
        String nomeContainer
) {
}
