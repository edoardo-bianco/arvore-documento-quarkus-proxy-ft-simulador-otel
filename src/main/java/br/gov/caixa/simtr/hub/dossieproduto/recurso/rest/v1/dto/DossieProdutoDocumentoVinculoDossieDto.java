package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoVinculoDossieDto(
        @Valid DossieProdutoDocumentoClienteDto cliente,
        @JsonProperty("elemento_conteudo") Long elementoConteudo,
        @Valid DossieProdutoDocumentoGarantiaDto garantia
) {
}
