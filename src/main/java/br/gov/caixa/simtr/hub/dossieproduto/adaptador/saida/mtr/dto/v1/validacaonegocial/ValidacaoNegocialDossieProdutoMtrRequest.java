package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.validacaonegocial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidacaoNegocialDossieProdutoMtrRequest(
        List<Verificacao> verificacoes,
        @JsonProperty("respostas_formulario") List<RespostaFormulario> respostasFormulario
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Verificacao(
            @JsonProperty("identificador_instancia_documento")
            Long identificadorInstanciaDocumento,
            @JsonProperty("identificador_checklist") Long identificadorChecklist,
            @JsonProperty("versao_checklist") Integer versaoChecklist,
            @JsonProperty("analise_realizada") Boolean analiseRealizada,
            @JsonProperty("parecer_apontamentos") List<ParecerApontamento> parecerApontamentos,
            Garantia garantia,
            Produto produto,
            Boolean previo
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ParecerApontamento(
            @JsonProperty("identificador_apontamento") Long identificadorApontamento,
            String resultado,
            String comentario,
            @JsonProperty("necessidade_reanalise") Boolean necessidadeReanalise,
            @JsonProperty("indice_ia") Double indiceIa
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Garantia(
            @JsonProperty("codigo_bacen") Integer codigoBacen,
            @JsonProperty("clientes_avalistas") List<ClienteAvalista> clientesAvalistas
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClienteAvalista(String cpf, String cnpj) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Produto(
            @JsonProperty("codigo_operacao") Integer codigoOperacao,
            @JsonProperty("codigo_modalidade") Integer codigoModalidade
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RespostaFormulario(
            @JsonProperty("campo_formulario") Long campoFormulario,
            String resposta,
            @JsonProperty("opcoes_selecionadas") List<String> opcoesSelecionadas
    ) {
    }
}
