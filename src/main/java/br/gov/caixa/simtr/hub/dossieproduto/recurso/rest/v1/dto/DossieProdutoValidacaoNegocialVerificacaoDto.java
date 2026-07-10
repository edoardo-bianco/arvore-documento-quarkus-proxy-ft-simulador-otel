package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoValidacaoNegocialVerificacaoDto(
        @JsonProperty("identificador_instancia_documento") Long identificadorInstanciaDocumento,
        @JsonProperty("identificador_checklist")
        @NotNull(message = "O identificador do checklist deve ser informado.") Long identificadorChecklist,
        @JsonProperty("versao_checklist")
        @NotNull(message = "A versao do checklist deve ser informada.") Integer versaoChecklist,
        @JsonProperty("analise_realizada")
        @NotNull(message = "O indicador de analise realizada deve ser informado.") Boolean analiseRealizada,
        @JsonProperty("parecer_apontamentos")
        @NotNull(message = "Os pareceres dos apontamentos devem ser informados.")
        List<@Valid DossieProdutoValidacaoNegocialParecerApontamentoDto> parecerApontamentos,
        @Valid DossieProdutoValidacaoNegocialGarantiaDto garantia,
        @Valid DossieProdutoValidacaoNegocialProdutoDto produto,
        Boolean previo
) {
}
