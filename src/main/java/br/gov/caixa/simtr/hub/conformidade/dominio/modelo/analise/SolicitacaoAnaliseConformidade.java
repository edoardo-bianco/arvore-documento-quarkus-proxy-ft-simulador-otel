package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;

public record SolicitacaoAnaliseConformidade(
        String texto,
        Long identificadorChecklist,
        Integer versaoChecklist) {

    public static final int TAMANHO_MAXIMO_TEXTO = 20_000;

    public SolicitacaoAnaliseConformidade {
        if (texto == null || texto.isBlank() || texto.length() > TAMANHO_MAXIMO_TEXTO) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "O texto deve possuir entre 1 e 20000 caracteres");
        }
        if (identificadorChecklist == null || identificadorChecklist <= 0) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "O identificador do checklist deve ser positivo");
        }
        if (versaoChecklist == null || versaoChecklist <= 0) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "A versão do checklist deve ser positiva");
        }
    }
}
