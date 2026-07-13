package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CriacaoDossieProdutoRequest(
        @NotNull(message = "O processo deve ser informado.") Long processo,
        @JsonProperty("chave_correlacao_canal")
        @NotNull(message = "A chave de correlacao do canal deve ser informada.") Long chaveCorrelacaoCanal,
        @JsonProperty("numero_negocio") Long numeroNegocio,
        List<@Valid DossieProdutoClienteDto> clientes
) {
}
