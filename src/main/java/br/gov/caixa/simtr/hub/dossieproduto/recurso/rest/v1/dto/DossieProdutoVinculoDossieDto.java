package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoVinculoDossieDto(
        Long fase,
        @Valid DossieProdutoVinculoClienteDto cliente,
        @Valid DossieProdutoVinculoProdutoDto produto,
        @Valid DossieProdutoVinculoGarantiaDto garantia,
        @JsonProperty("respostas_formulario") List<@Valid DossieProdutoRespostaFormularioDto> respostasFormulario
) {
}
