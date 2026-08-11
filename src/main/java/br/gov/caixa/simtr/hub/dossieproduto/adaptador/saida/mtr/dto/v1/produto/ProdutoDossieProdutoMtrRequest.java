package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.produto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProdutoDossieProdutoMtrRequest(
        @JsonProperty("codigo_operacao") Integer codigoOperacao,
        @JsonProperty("codigo_modalidade") Integer codigoModalidade,
        Boolean excluir
) {
}
