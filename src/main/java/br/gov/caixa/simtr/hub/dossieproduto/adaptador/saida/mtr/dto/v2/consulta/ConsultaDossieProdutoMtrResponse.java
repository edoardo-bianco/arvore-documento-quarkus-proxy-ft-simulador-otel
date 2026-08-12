package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.consulta;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ConsultaDossieProdutoMtrResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("chave_correlacao_canal") Long chaveCorrelacaoCanal,
        @JsonProperty("instancia_jbpm") Long instanciaJbpm,
        @JsonProperty("numero_negocio") Long numeroNegocio,
        @JsonProperty("canal_criacao") String canalCriacao,
        @JsonProperty("unidade_criacao") Integer unidadeCriacao,
        @JsonProperty("data_criacao") String dataCriacao,
        @JsonProperty("clientes") List<Cliente> clientes,
        @JsonProperty("processo") Processo processo,
        @JsonProperty("fase_atual") Fase faseAtual,
        @JsonProperty("situacao_atual") Situacao situacaoAtual,
        @JsonProperty("unidades_tratamento") List<Integer> unidadesTratamento,
        @JsonProperty("produtos_contratados") List<ProdutoContratado> produtosContratados
) {

    public record Cliente(
            @JsonProperty("cpf") String cpf,
            @JsonProperty("cnpj") String cnpj,
            @JsonProperty("nome") String nome,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("tipo_vinculo") String tipoVinculo,
            @JsonProperty("identificador_negocial_vinculo") Long identificadorNegocialVinculo,
            @JsonProperty("principal") Boolean principal
    ) {
    }

    public record Processo(
            @JsonProperty("id") Integer id,
            @JsonProperty("nome") String nome,
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            @JsonProperty("macroprocesso") String macroprocesso,
            @JsonProperty("data") String data,
            @JsonProperty("tratamento_seletivo") Boolean tratamentoSeletivo,
            @JsonProperty("complementacao_seletiva") Boolean complementacaoSeletiva
    ) {
    }

    public record Fase(
            @JsonProperty("id") Integer id,
            @JsonProperty("nome") String nome,
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            @JsonProperty("data") String data
    ) {
    }

    public record Situacao(
            @JsonProperty("id") Integer id,
            @JsonProperty("nome") String nome,
            @JsonProperty("data") String data,
            @JsonProperty("matricula") String matricula
    ) {
    }

    public record ProdutoContratado(
            @JsonProperty("id") Integer id,
            @JsonProperty("codigo_operacao") Integer codigoOperacao,
            @JsonProperty("codigo_modalidade") Integer codigoModalidade,
            @JsonProperty("nome") String nome
    ) {
    }
}
