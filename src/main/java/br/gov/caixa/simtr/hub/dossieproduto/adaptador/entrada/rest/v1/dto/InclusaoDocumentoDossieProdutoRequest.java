package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InclusaoDocumentoDossieProdutoRequest(
        Long id,
        @JsonProperty("path_storage") String pathStorage,
        @JsonProperty("codigo_ged") String codigoGed,
        @JsonProperty("object_store_ged") String objectStoreGed,
        @JsonProperty("tipo_documento") String tipoDocumento,
        @JsonProperty("vinculo_dossie")
        @Valid DossieProdutoDocumentoVinculoDossieDto vinculoDossie,
        List<@Valid DossieProdutoDocumentoAtributoDto> atributos,
        List<@Valid DossieProdutoDocumentoPropriedadeDto> propriedades
) {
}
