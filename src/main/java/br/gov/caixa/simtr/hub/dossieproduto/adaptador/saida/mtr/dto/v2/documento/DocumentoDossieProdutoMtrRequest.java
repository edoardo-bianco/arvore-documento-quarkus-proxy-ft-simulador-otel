package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentoDossieProdutoMtrRequest(
        Long id,
        @JsonProperty("path_storage") String pathStorage,
        @JsonProperty("codigo_ged") String codigoGed,
        @JsonProperty("object_store_ged") String objectStoreGed,
        @JsonProperty("tipo_documento") String tipoDocumento,
        @JsonProperty("vinculo_dossie") VinculoDossie vinculoDossie,
        List<Atributo> atributos,
        List<Propriedade> propriedades
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VinculoDossie(
            Cliente cliente,
            @JsonProperty("elemento_conteudo") Long elementoConteudo,
            Garantia garantia
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
    public record Garantia(
            @JsonProperty("codigo_bacen") Integer codigoBacen,
            @JsonProperty("produto_operacao") Integer produtoOperacao,
            @JsonProperty("produto_modalidade") Integer produtoModalidade,
            @JsonProperty("cliente_avalista") List<Cliente> clienteAvalista
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Atributo(
            String chave,
            String valor,
            String objeto,
            @JsonProperty("opcoes_selecionadas") List<String> opcoesSelecionadas
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Propriedade(
            String chave,
            String valor,
            String objeto
    ) {
    }
}
