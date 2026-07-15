package br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo;

public record CredencialContainer(
        String sas,
        Object validade,
        String urlStorage,
        String nomeContainer
) {
}
