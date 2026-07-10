package br.gov.caixa.simtr.arvoredocumento.domain.gestaodocumento;

public record GestaoDocumentoCredencialContainerVo(
        String sas,
        Object validade,
        String urlStorage,
        String nomeContainer
) {
}
