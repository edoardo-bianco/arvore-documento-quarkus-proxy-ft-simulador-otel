package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto;

import java.util.List;

public record ResultadoAnaliseAgente(
        String resumo,
        List<ResultadoApontamentoAgente> apontamentos) {
}
