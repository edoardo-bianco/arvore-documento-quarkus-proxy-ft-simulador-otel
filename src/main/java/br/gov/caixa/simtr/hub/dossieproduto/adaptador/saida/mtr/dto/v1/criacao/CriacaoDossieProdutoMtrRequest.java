package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.criacao;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CriacaoDossieProdutoMtrRequest(
        Long processo,
        @JsonProperty("chave_correlacao_canal") Long chaveCorrelacaoCanal,
        @JsonProperty("numero_negocio") Long numeroNegocio,
        List<Cliente> clientes
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Cliente(
            String cpf,
            String cnpj,
            @JsonProperty("tipo_vinculo") Long tipoVinculo,
            @JsonProperty("cliente_relacionado") ClienteRelacionado clienteRelacionado,
            @JsonProperty("sequencia_titularidade") Integer sequenciaTitularidade
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClienteRelacionado(String cpf, String cnpj) {
    }
}
