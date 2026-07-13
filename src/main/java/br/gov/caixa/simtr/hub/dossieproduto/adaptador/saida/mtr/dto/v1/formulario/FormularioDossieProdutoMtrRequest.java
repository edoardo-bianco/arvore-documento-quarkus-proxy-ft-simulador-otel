package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormularioDossieProdutoMtrRequest(
        @JsonProperty("vinculo_dossie") VinculoDossie vinculoDossie
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VinculoDossie(
            Long fase,
            Cliente cliente,
            Produto produto,
            Garantia garantia,
            @JsonProperty("respostas_formulario") List<Resposta> respostasFormulario
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Cliente(
            String cpf,
            String cnpj,
            @JsonProperty("tipo_vinculo") Long tipoVinculo
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Produto(
            @JsonProperty("codigo_operacao") Integer codigoOperacao,
            @JsonProperty("codigo_modalidade") Integer codigoModalidade
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Garantia(
            @JsonProperty("codigo_bacen") Integer codigoBacen,
            @JsonProperty("produto_operacao") Integer produtoOperacao,
            @JsonProperty("produto_modalidade") Integer produtoModalidade,
            @JsonProperty("clientes_avalistas") List<ClienteAvalista> clientesAvalistas
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClienteAvalista(String cpf, String cnpj) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resposta(
            @JsonProperty("campo_formulario") Long campoFormulario,
            String resposta,
            @JsonProperty("opcoes_selecionadas") List<String> opcoesSelecionadas,
            Boolean excluir
    ) {
    }
}
