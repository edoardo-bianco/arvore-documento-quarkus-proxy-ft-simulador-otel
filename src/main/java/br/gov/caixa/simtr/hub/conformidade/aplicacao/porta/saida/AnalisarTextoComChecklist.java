package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;

public interface AnalisarTextoComChecklist {

    ResultadoAnaliseConformidade analisar(
            String identificadorMemoria,
            EntradaAnaliseAgente entrada);
}
