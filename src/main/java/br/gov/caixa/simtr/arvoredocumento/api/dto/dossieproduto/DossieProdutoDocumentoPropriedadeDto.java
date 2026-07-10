package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoPropriedadeDto(
        @NotNull(message = "A chave da propriedade do documento deve ser informada.") String chave,
        @NotNull(message = "O valor da propriedade do documento deve ser informado.") String valor,
        String objeto
) {
}
