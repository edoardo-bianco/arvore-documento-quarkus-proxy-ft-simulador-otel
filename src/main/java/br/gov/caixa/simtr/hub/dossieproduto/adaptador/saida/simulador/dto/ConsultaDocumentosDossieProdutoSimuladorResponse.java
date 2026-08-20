package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ConsultaDocumentosDossieProdutoSimuladorResponse(
        @JsonProperty("documentos") List<Documento> documentos
) {

    public record Documento(
            @JsonProperty("id_instancia_documento") Long idInstanciaDocumento,
            @JsonProperty("id_documento") Long idDocumento,
            @JsonProperty("codigo_ged") String codigoGed,
            @JsonProperty("data_hora_captura") String dataHoraCaptura,
            @JsonProperty("data_hora_validade") String dataHoraValidade,
            @JsonProperty("matricula_captura") String matriculaCaptura,
            @JsonProperty("tipo_documento") TipoDocumento tipoDocumento,
            @JsonProperty("situacao_documento") String situacaoDocumento,
            @JsonProperty("vinculo_dossie") VinculoDossie vinculoDossie,
            @JsonProperty("url") String url,
            @JsonProperty("atributos") List<Atributo> atributos,
            @JsonProperty("assinaturas_digitais") List<AssinaturaDigital> assinaturasDigitais,
            @JsonProperty("conformidade") List<Conformidade> conformidade,
            @JsonProperty("propriedades") List<Propriedade> propriedades,
            @JsonProperty("outsourcing") List<Outsourcing> outsourcing,
            @JsonProperty("armazenamento") List<Armazenamento> armazenamento
    ) {
    }

    public record TipoDocumento(
            @JsonProperty("id") Long id,
            @JsonProperty("nome") String nome,
            @JsonProperty("codigo_tipologia") String codigoTipologia,
            @JsonProperty("ativo") Boolean ativo
    ) {
    }

    public record VinculoDossie(
            @JsonProperty("cliente") Cliente cliente,
            @JsonProperty("produto") Produto produto,
            @JsonProperty("garantia") Garantia garantia,
            @JsonProperty("fase") Fase fase,
            @JsonProperty("processo") Processo processo
    ) {
    }

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

    public record Produto(
            @JsonProperty("id") Integer id,
            @JsonProperty("nome") String nome,
            @JsonProperty("modalidade") Integer modalidade,
            @JsonProperty("operacao") Integer operacao
    ) {
    }

    public record Garantia(
            @JsonProperty("id") Long id,
            @JsonProperty("nome") String nome,
            @JsonProperty("codigo_bacen") Integer codigoBacen,
            @JsonProperty("produto") Produto produto,
            @JsonProperty("clientes_avalistas") List<ClienteAvalista> clientesAvalistas
    ) {
    }

    public record ClienteAvalista(
            @JsonProperty("cpf") String cpf,
            @JsonProperty("cnpj") String cnpj
    ) {
    }

    public record Fase(
            @JsonProperty("id") Integer id,
            @JsonProperty("nome") String nome,
            @JsonProperty("identificador_negocial") Long identificadorNegocial
    ) {
    }

    public record Processo(
            @JsonProperty("id") Integer id,
            @JsonProperty("nome") String nome,
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            @JsonProperty("macroprocesso") String macroprocesso
    ) {
    }

    public record Atributo(
            @JsonProperty("chave") String chave,
            @JsonProperty("valor") String valor,
            @JsonProperty("opcoes_selecionadas") List<String> opcoesSelecionadas
    ) {
    }

    public record AssinaturaDigital(
            @JsonProperty("data_assinatura") String dataAssinatura,
            @JsonProperty("emissor") String emissor,
            @JsonProperty("cpf") String cpf,
            @JsonProperty("cnpj") String cnpj,
            @JsonProperty("nome") String nome,
            @JsonProperty("cpf_responsavel_pj") String cpfResponsavelPj,
            @JsonProperty("nome_responsavel_pj") String nomeResponsavelPj
    ) {
    }

    public record Conformidade(
            @JsonProperty("id") Long id,
            @JsonProperty("tipo_conformidade") String tipoConformidade,
            @JsonProperty("unidade_conformidade") Long unidadeConformidade,
            @JsonProperty("data_hora_verificacao") String dataHoraVerificacao,
            @JsonProperty("dossie_produto") Long dossieProduto,
            @JsonProperty("fornecedor") String fornecedor,
            @JsonProperty("resultado") String resultado,
            @JsonProperty("checklist") Checklist checklist
    ) {
    }

    public record Checklist(
            @JsonProperty("id") Long id,
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            @JsonProperty("nome") String nome,
            @JsonProperty("versao") String versao,
            @JsonProperty("apontamentos") List<Apontamento> apontamentos
    ) {
    }

    public record Apontamento(
            @JsonProperty("id") Long id,
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            @JsonProperty("nome") String nome,
            @JsonProperty("tipo") String tipo,
            @JsonProperty("complexidade") String complexidade,
            @JsonProperty("aprovado") Boolean aprovado,
            @JsonProperty("orientacao") String orientacao,
            @JsonProperty("comentario") String comentario
    ) {
    }

    public record Propriedade(
            @JsonProperty("chave") String chave,
            @JsonProperty("valor") String valor
    ) {
    }

    @SuppressWarnings("java:S107") // Os campos refletem o contrato v4 da fixture do simulador.
    public record Outsourcing(
            @JsonProperty("solicitacao") String solicitacao,
            @JsonProperty("processo") String processo,
            @JsonProperty("analise_conjunta") Boolean analiseConjunta,
            @JsonProperty("codigo_controle") Long codigoControle,
            @JsonProperty("codigo_fornecedor") String codigoFornecedor,
            @JsonProperty("sigla_fornecedor") String siglaFornecedor,
            @JsonProperty("sigla_canal") String siglaCanal,
            @JsonProperty("codigo_canal") String codigoCanal,
            @JsonProperty("retorno_agrupado") Boolean retornoAgrupado,
            @JsonProperty("data_hora_envio") String dataHoraEnvio,
            @JsonProperty("status_envio") String statusEnvio,
            @JsonProperty("solicitacao_tratamento_imagem")
            Boolean solicitacaoTratamentoImagem,
            @JsonProperty("solicitacao_extracao") Boolean solicitacaoExtracao,
            @JsonProperty("solicitacao_validacao_negocial")
            Boolean solicitacaoValidacaoNegocial,
            @JsonProperty("solicitacao_classificacao") Boolean solicitacaoClassificacao,
            @JsonProperty("solicitacao_avaliacao_cadastral")
            Boolean solicitacaoAvaliacaoCadastral,
            @JsonProperty("solicitacao_avaliacao_autenticidade")
            Boolean solicitacaoAvaliacaoAutenticidade,
            @JsonProperty("solicitacao_grafoscopia") Boolean solicitacaoGrafoscopia,
            @JsonProperty("solicitacao_validacao_externa") Boolean solicitacaoValidacaoExterna,
            @JsonProperty("solicitacao_consulta_externa") Boolean solicitacaoConsultaExterna,
            @JsonProperty("janela_extracao") String janelaExtracao,
            @JsonProperty("data_hora_retorno_classificacao")
            String dataHoraRetornoClassificacao,
            @JsonProperty("data_hora_retorno_extracao") String dataHoraRetornoExtracao,
            @JsonProperty("data_hora_retorno_validacao_negocial")
            String dataHoraRetornoValidacaoNegocial,
            @JsonProperty("data_hora_retorno_grafoscopia") String dataHoraRetornoGrafoscopia,
            @JsonProperty("data_hora_retorno_avaliacao_autenticidade")
            String dataHoraRetornoAvaliacaoAutenticidade,
            @JsonProperty("data_hora_retorno_imagem_tratada")
            String dataHoraRetornoImagemTratada,
            @JsonProperty("data_hora_validacao_externa") String dataHoraValidacaoExterna,
            @JsonProperty("data_consulta_externa") String dataConsultaExterna,
            @JsonProperty("data_avaliacao_cadastral") String dataAvaliacaoCadastral,
            @JsonProperty("resultado_indice_avaliacao_autenticidade")
            Double resultadoIndiceAvaliacaoAutenticidade,
            @JsonProperty("resultado_indice_grafoscopia") Double resultadoIndiceGrafoscopia,
            @JsonProperty("resultado_codigo_rejeicao") String resultadoCodigoRejeicao,
            @JsonProperty("resultado_descricao_rejeicao") String resultadoDescricaoRejeicao,
            @JsonProperty("resultado_consulta_externa") String resultadoConsultaExterna,
            @JsonProperty("resultado_classificacao") String resultadoClassificacao,
            @JsonProperty("resultado_extracao") String resultadoExtracao,
            @JsonProperty("resultado_validacao_negocial")
            ResultadoValidacaoNegocial resultadoValidacaoNegocial
    ) {
    }

    public record ResultadoValidacaoNegocial(
            @JsonProperty("identificador_checklist") Long identificadorChecklist,
            @JsonProperty("versao_checklist") Integer versaoChecklist,
            @JsonProperty("apontamentos")
            List<ResultadoValidacaoNegocialApontamento> apontamentos
    ) {
    }

    public record ResultadoValidacaoNegocialApontamento(
            @JsonProperty("identificador_apontamento") Long identificadorApontamento,
            @JsonProperty("aprovado") Boolean aprovado,
            @JsonProperty("comentario") String comentario
    ) {
    }

    public record Armazenamento(
            @JsonProperty("id") Long id,
            @JsonProperty("data_hora_armazenamento") String dataHoraArmazenamento,
            @JsonProperty("tipo_armazenamento") String tipoArmazenamento,
            @JsonProperty("path_storage") String pathStorage,
            @JsonProperty("object_store_ged") String objectStoreGed,
            @JsonProperty("codigo_ged") String codigoGed,
            @JsonProperty("data_hora_previsao_exclusao") String dataHoraPrevisaoExclusao,
            @JsonProperty("data_hora_exclusao") String dataHoraExclusao
    ) {
    }
}
