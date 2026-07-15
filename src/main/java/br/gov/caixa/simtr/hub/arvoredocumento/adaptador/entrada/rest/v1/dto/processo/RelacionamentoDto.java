package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelacionamentoDto(
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        String nome,
        @JsonProperty("tipo_pessoa") String tipoPessoa,
        Boolean principal,
        Boolean obrigatorio,
        Boolean relacionado,
        Boolean sequencia,
        @JsonProperty("campos_formulario") List<CampoFormularioDto> camposFormulario,
        List<DocumentoDto> documentos
) {
}
