package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CampoFormularioDto(
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
        @JsonProperty("opcoes_disponiveis") List<OpcaoDisponivelDto> opcoesDisponiveis
) {
}
