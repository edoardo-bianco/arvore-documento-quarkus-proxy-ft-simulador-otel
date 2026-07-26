package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import java.util.UUID;

public record SolicitacaoAnaliseConformidade(
        String correlationId,
        String identificadorDocumento,
        String texto,
        Long identificadorChecklist,
        Integer versaoChecklist) {

    public static final int TAMANHO_MAXIMO_TEXTO = 20_000;

    public SolicitacaoAnaliseConformidade {
        if (correlationId == null || correlationId.isBlank()) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "O identificador de correlação é obrigatório");
        }
        if (identificadorDocumento == null || identificadorDocumento.isBlank()) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "O identificador do documento é obrigatório");
        }
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

    public static SolicitacaoAnaliseConformidade nova(
            String identificadorDocumento,
            String texto,
            Long identificadorChecklist,
            Integer versaoChecklist) {
        return new SolicitacaoAnaliseConformidade(
                UUID.randomUUID().toString(),
                identificadorDocumento,
                texto,
                identificadorChecklist,
                versaoChecklist);
    }
}
