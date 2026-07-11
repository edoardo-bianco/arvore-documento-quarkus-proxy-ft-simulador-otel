package br.gov.caixa.simtr.hub.arquitetura.excecao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroMensagemDto(
        String mensagem
) {
}
