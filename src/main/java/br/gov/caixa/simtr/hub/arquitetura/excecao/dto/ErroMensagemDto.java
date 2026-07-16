package br.gov.caixa.simtr.hub.arquitetura.excecao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroMensagemDto(
        String mensagem
) implements Serializable {
}
