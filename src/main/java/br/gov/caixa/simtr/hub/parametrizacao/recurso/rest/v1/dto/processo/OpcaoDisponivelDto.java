package br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpcaoDisponivelDto(
        @JsonProperty("valor_opcao") String valorOpcao,
        @JsonProperty("descricao_opcao") String descricaoOpcao,
        Boolean ativo
) {
}
