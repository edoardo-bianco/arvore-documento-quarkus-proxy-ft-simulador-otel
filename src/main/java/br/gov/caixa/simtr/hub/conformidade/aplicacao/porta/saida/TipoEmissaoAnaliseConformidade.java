package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import java.util.Arrays;

public enum TipoEmissaoAnaliseConformidade {
    REVISAO_SOLICITADA("br.gov.caixa.simtr.conformidade.revisao.solicitada.v1"),
    ANALISE_CONCLUIDA("br.gov.caixa.simtr.conformidade.analise.concluida.v1");

    private final String cloudEventType;

    TipoEmissaoAnaliseConformidade(String cloudEventType) {
        this.cloudEventType = cloudEventType;
    }

    public String cloudEventType() {
        return cloudEventType;
    }

    public static TipoEmissaoAnaliseConformidade deCloudEventType(String tipo) {
        return Arrays.stream(values())
                .filter(valor -> valor.cloudEventType.equals(tipo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de emissão desconhecido"));
    }
}
