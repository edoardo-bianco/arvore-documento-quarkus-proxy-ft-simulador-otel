package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto;

public record ResultadoApontamentoAgente(
        Long identificadorApontamento,
        String nomeApontamento,
        ParecerConformidadeAgente parecer,
        String justificativa,
        String evidencia,
        Double confianca) {
}
