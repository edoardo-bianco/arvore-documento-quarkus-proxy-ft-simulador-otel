package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.dto.v2.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessoParametrizadoMtrResponse(
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        String nome,
        Boolean ativo,
        @JsonProperty("ultima_alteracao") String ultimaAlteracao,
        @JsonProperty("indicador_produto_obrigatorio") Boolean indicadorProdutoObrigatorio,
        Macroprocesso macroprocesso,
        List<Relacionamento> relacionamentos,
        List<Produto> produtos,
        List<Fase> fases,
        List<Documento> documentos,
        ReferenciaChecklist checklist
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Macroprocesso(
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            String nome,
            Boolean ativo,
            @JsonProperty("ultima_alteracao") String ultimaAlteracao
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Relacionamento(
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            String nome,
            @JsonProperty("tipo_pessoa") String tipoPessoa,
            Boolean principal,
            Boolean obrigatorio,
            Boolean relacionado,
            Boolean sequencia,
            @JsonProperty("campos_formulario") List<CampoFormulario> camposFormulario,
            List<Documento> documentos
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Produto(
            @JsonProperty("codigo_operacao") Long codigoOperacao,
            @JsonProperty("codigo_modalidade") Long codigoModalidade,
            String nome,
            @JsonProperty("campos_formulario") List<CampoFormulario> camposFormulario,
            List<Documento> documentos,
            List<Garantia> garantias,
            ReferenciaChecklist checklist
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Fase(
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            String nome,
            Boolean ativo,
            @JsonProperty("ultima_alteracao") String ultimaAlteracao,
            Integer ordem,
            @JsonProperty("orientacao_usuario") String orientacaoUsuario,
            List<Produto> produtos,
            List<Garantia> garantias,
            @JsonProperty("campos_formulario") List<CampoFormulario> camposFormulario,
            List<Documento> documentos,
            List<ReferenciaChecklist> checklist
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Garantia(
            @JsonProperty("codigo_bacen") Long codigoBacen,
            @JsonProperty("nome_garantia") String nomeGarantia,
            Boolean fidejussoria,
            @JsonProperty("campos_formulario") List<CampoFormulario> camposFormulario,
            List<Documento> documentos,
            ReferenciaChecklist checklist
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Documento(
            @JsonProperty("funcao_documental") FuncaoDocumental funcaoDocumental,
            @JsonProperty("tipo_documento") TipoDocumento tipoDocumento,
            Boolean obrigatorio
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FuncaoDocumental(
            String nome,
            @JsonProperty("tipos_documento") List<TipoDocumento> tiposDocumento,
            ReferenciaChecklist checklist
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TipoDocumento(
            @JsonProperty("codigo_tipologia") String codigoTipologia,
            String nome,
            @JsonProperty("permite_reuso") Boolean permiteReuso,
            @JsonProperty("permite_multiplo") Boolean permiteMultiplo,
            Boolean ativo,
            ReferenciaChecklist checklist
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CampoFormulario(
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            String label,
            Boolean obrigatorio,
            Boolean ativo,
            @JsonProperty("exibicao_condicional") String exibicaoCondicional,
            @JsonProperty("tamanho_apresentacao") Integer tamanhoApresentacao,
            @JsonProperty("ordem_apresentacao") Integer ordemApresentacao,
            String tipo,
            String mascara,
            String placeholder,
            @JsonProperty("tamanho_minimo") Integer tamanhoMinimo,
            @JsonProperty("tamanho_maximo") Integer tamanhoMaximo,
            @JsonProperty("orientacao_preenchimento") String orientacaoPreenchimento,
            @JsonProperty("bloquear_edicao") Boolean bloquearEdicao,
            @JsonProperty("opcoes_disponiveis") List<OpcaoDisponivel> opcoesDisponiveis
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OpcaoDisponivel(
            @JsonProperty("valor_opcao") String valorOpcao,
            @JsonProperty("descricao_opcao") String descricaoOpcao,
            Boolean ativo
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = ReferenciaChecklistProcessoMtrDeserializer.class)
    public record ReferenciaChecklist(
            @JsonProperty("identificador_checklist") Long identificadorChecklist,
            @JsonProperty("versao_checklist") Integer versaoChecklist
    ) {
    }
}
