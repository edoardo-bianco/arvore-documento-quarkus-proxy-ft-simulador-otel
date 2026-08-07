package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
        .IdsDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
        .ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise
        .SolicitacaoAnaliseConformidade;

public record ContextoAnaliseConformidadeFlow(
        String correlationId,
        String identificadorDocumento,
        Long identificadorChecklist,
        Integer versaoChecklist,
        ReferenciaDocumentoAnaliseConformidade checklistRef) {

    public ContextoAnaliseConformidadeFlow {
        if (correlationId == null
                || correlationId.isBlank()
                || identificadorDocumento == null
                || identificadorDocumento.isBlank()
                || identificadorChecklist == null
                || identificadorChecklist <= 0
                || versaoChecklist == null
                || versaoChecklist <= 0) {
            throw new IllegalArgumentException("Contexto referencial inválido");
        }
        if (checklistRef != null
                && !IdsDocumentoAnaliseConformidade.checklist(correlationId)
                        .equals(checklistRef.documentoRef())) {
            throw new IllegalArgumentException("Referência de checklist inválida");
        }
    }

    public static ContextoAnaliseConformidadeFlow inicial(
            SolicitacaoAnaliseConformidade solicitacao) {
        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação obrigatória");
        }
        return new ContextoAnaliseConformidadeFlow(
                solicitacao.correlationId(),
                solicitacao.identificadorDocumento(),
                solicitacao.identificadorChecklist(),
                solicitacao.versaoChecklist(),
                null);
    }

    public ContextoAnaliseConformidadeFlow comChecklist(
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return new ContextoAnaliseConformidadeFlow(
                correlationId,
                identificadorDocumento,
                identificadorChecklist,
                versaoChecklist,
                referencia);
    }
}
