package br.gov.caixa.simtr.hub.parametrizacao.fachada;

import br.gov.caixa.simtr.hub.parametrizacao.dominio.checklist.ChecklistVo;
import br.gov.caixa.simtr.hub.parametrizacao.dominio.processo.ProcessoVo;
import br.gov.caixa.simtr.hub.parametrizacao.servico.ChecklistService;
import br.gov.caixa.simtr.hub.parametrizacao.servico.ProcessoService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ParametrizacaoFachada {

    private final ProcessoService processoService;
    private final ChecklistService checklistService;

    @Inject
    public ParametrizacaoFachada(ProcessoService processoService,
                                 ChecklistService checklistService) {
        this.processoService = processoService;
        this.checklistService = checklistService;
    }

    public Uni<ProcessoVo> consultarProcessoPorIdentificadorNegocial(Long identificador) {
        return processoService.consultarPorIdentificadorNegocial(identificador);
    }

    public Uni<ChecklistVo> consultarChecklistPorIdentificadorNegocialEVersao(Long identificador, Integer versao) {
        return checklistService.consultarPorIdentificadorNegocialEVersao(identificador, versao);
    }
}
