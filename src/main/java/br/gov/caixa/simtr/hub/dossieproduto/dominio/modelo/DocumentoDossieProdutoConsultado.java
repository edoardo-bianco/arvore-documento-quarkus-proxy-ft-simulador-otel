package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record DocumentoDossieProdutoConsultado(
        Long idInstanciaDocumento,
        Long idDocumento,
        String codigoGed,
        String dataHoraCaptura,
        String dataHoraValidade,
        String matriculaCaptura,
        TipoDocumento tipoDocumento,
        String situacaoDocumento,
        VinculoDossie vinculoDossie,
        String url,
        List<Atributo> atributos,
        List<AssinaturaDigital> assinaturasDigitais,
        List<Conformidade> conformidades,
        List<Propriedade> propriedades,
        List<Outsourcing> outsourcings,
        List<Armazenamento> armazenamentos
) {

    public record TipoDocumento(
            Long id,
            String nome,
            String codigoTipologia,
            Boolean ativo
    ) {
    }

    public record VinculoDossie(
            Cliente cliente,
            Produto produto,
            Garantia garantia,
            Fase fase,
            Processo processo
    ) {
    }

    public record Cliente(
            String cpf,
            String cnpj,
            String nome,
            String razaoSocial,
            String tipoVinculo,
            Long identificadorNegocialVinculo,
            Boolean principal
    ) {
    }

    public record Produto(
            Integer id,
            String nome,
            Integer modalidade,
            Integer operacao
    ) {
    }

    public record Garantia(
            Long id,
            String nome,
            Integer codigoBacen,
            Produto produto,
            List<ClienteAvalista> clientesAvalistas
    ) {
    }

    public record ClienteAvalista(
            String cpf,
            String cnpj
    ) {
    }

    public record Fase(
            Integer id,
            String nome,
            Long identificadorNegocial
    ) {
    }

    public record Processo(
            Integer id,
            String nome,
            Long identificadorNegocial,
            String macroprocesso
    ) {
    }

    public record Atributo(
            String chave,
            String valor,
            List<String> opcoesSelecionadas
    ) {
    }

    public record AssinaturaDigital(
            String dataAssinatura,
            String emissor,
            String cpf,
            String cnpj,
            String nome,
            String cpfResponsavelPj,
            String nomeResponsavelPj
    ) {
    }

    public record Conformidade(
            Long id,
            String tipoConformidade,
            Long unidadeConformidade,
            String dataHoraVerificacao,
            Long dossieProduto,
            String fornecedor,
            String resultado,
            Checklist checklist
    ) {
    }

    public record Checklist(
            Long id,
            Long identificadorNegocial,
            String nome,
            String versao,
            List<Apontamento> apontamentos
    ) {
    }

    public record Apontamento(
            Long id,
            Long identificadorNegocial,
            String nome,
            String tipo,
            String complexidade,
            Boolean aprovado,
            String orientacao,
            String comentario
    ) {
    }

    public record Propriedade(
            String chave,
            String valor
    ) {
    }

    @SuppressWarnings("java:S107") // Os campos refletem o resultado v4 aprovado sem generalizacao.
    public record Outsourcing(
            String solicitacao,
            String processo,
            Boolean analiseConjunta,
            Long codigoControle,
            String codigoFornecedor,
            String siglaFornecedor,
            String siglaCanal,
            String codigoCanal,
            Boolean retornoAgrupado,
            String dataHoraEnvio,
            String statusEnvio,
            Boolean solicitacaoTratamentoImagem,
            Boolean solicitacaoExtracao,
            Boolean solicitacaoValidacaoNegocial,
            Boolean solicitacaoClassificacao,
            Boolean solicitacaoAvaliacaoCadastral,
            Boolean solicitacaoAvaliacaoAutenticidade,
            Boolean solicitacaoGrafoscopia,
            Boolean solicitacaoValidacaoExterna,
            Boolean solicitacaoConsultaExterna,
            String janelaExtracao,
            String dataHoraRetornoClassificacao,
            String dataHoraRetornoExtracao,
            String dataHoraRetornoValidacaoNegocial,
            String dataHoraRetornoGrafoscopia,
            String dataHoraRetornoAvaliacaoAutenticidade,
            String dataHoraRetornoImagemTratada,
            String dataHoraValidacaoExterna,
            String dataConsultaExterna,
            String dataAvaliacaoCadastral,
            Double resultadoIndiceAvaliacaoAutenticidade,
            Double resultadoIndiceGrafoscopia,
            String resultadoCodigoRejeicao,
            String resultadoDescricaoRejeicao,
            String resultadoConsultaExterna,
            String resultadoClassificacao,
            String resultadoExtracao,
            ResultadoValidacaoNegocial resultadoValidacaoNegocial
    ) {
    }

    public record ResultadoValidacaoNegocial(
            Long identificadorChecklist,
            Integer versaoChecklist,
            List<ResultadoValidacaoNegocialApontamento> apontamentos
    ) {
    }

    public record ResultadoValidacaoNegocialApontamento(
            Long identificadorApontamento,
            Boolean aprovado,
            String comentario
    ) {
    }

    public record Armazenamento(
            Long id,
            String dataHoraArmazenamento,
            String tipoArmazenamento,
            String pathStorage,
            String objectStoreGed,
            String codigoGed,
            String dataHoraPrevisaoExclusao,
            String dataHoraExclusao
    ) {
    }
}
