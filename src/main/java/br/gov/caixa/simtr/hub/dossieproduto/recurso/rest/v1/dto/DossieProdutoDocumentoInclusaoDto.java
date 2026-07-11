package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoInclusaoDto(
        Long id,
        @JsonProperty("path_storage") String pathStorage,
        @JsonProperty("codigo_ged") String codigoGed,
        @JsonProperty("object_store_ged") String objectStoreGed,
        @JsonProperty("tipo_documento") String tipoDocumento,
        @JsonProperty("vinculo_dossie") @Valid DossieProdutoDocumentoVinculoDossieDto vinculoDossie,
        @Valid List<DossieProdutoDocumentoAtributoDto> atributos,
        @Valid List<DossieProdutoDocumentoPropriedadeDto> propriedades
) {
}
