package br.gov.caixa.arvoredocumento.api.dto.erro;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroMensagemDto(
        String mensagem
) {
}
