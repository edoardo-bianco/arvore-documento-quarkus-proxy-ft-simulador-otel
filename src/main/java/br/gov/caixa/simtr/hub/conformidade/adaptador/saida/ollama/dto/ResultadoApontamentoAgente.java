package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ResultadoApontamentoAgente(
        Long identificadorApontamento,
        String nomeApontamento,
        ParecerConformidadeAgente parecer,
        String justificativa,
        String evidencia,
        @JsonAlias("confiança")
        Double confianca) {
}
