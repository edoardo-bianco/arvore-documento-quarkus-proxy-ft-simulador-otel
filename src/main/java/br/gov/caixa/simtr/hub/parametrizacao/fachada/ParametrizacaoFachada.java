package br.gov.caixa.simtr.hub.parametrizacao.fachada;

import br.gov.caixa.simtr.hub.parametrizacao.dominio.checklist.ChecklistVo;
import br.gov.caixa.simtr.hub.parametrizacao.servico.ChecklistService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ParametrizacaoFachada {

    private final ChecklistService checklistService;

    @Inject
    public ParametrizacaoFachada(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    public Uni<ChecklistVo> consultarChecklistPorIdentificadorNegocialEVersao(Long identificador, Integer versao) {
        return checklistService.consultarPorIdentificadorNegocialEVersao(identificador, versao);
    }
}
