package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResultadoApontamentoConformidadeResponse(
        Long identificadorApontamento,
        String nomeApontamento,
        ParecerConformidadeDto parecer,
        String justificativa,
        String evidencia,
        Double confianca) {
}
