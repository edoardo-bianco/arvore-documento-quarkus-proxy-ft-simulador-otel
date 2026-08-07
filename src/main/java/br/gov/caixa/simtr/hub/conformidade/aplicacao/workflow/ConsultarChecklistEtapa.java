package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
        .ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
        .ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise
        .SolicitacaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ConsultarChecklistEtapa {

    private final ConsultarChecklist consultarChecklist;
    private final ArmazenarEstadoAnaliseConformidade estados;
    private final ReferenciasDocumentoAnaliseConformidade referencias;

    @Inject
    public ConsultarChecklistEtapa(
            ConsultarChecklist consultarChecklist,
            ArmazenarEstadoAnaliseConformidade estados,
            ReferenciasDocumentoAnaliseConformidade referencias) {
        this.consultarChecklist = consultarChecklist;
        this.estados = estados;
        this.referencias = referencias;
    }

    public Uni<ContextoAnaliseConformidadeFlow> executar(
            String identificadorInstancia,
            ContextoAnaliseConformidadeFlow contexto) {
        if (contexto == null) {
            return Uni.createFrom().failure(
                    FalhaAnaliseConformidade.solicitacaoInvalida(
                            "O contexto referencial da análise é obrigatório"));
        }
        String instanceId = AnaliseConformidadeFlow.identificadorRaiz(
                identificadorInstancia);
        return estados.carregarSolicitacao(instanceId)
                .invoke(solicitacao -> validarIdentidades(contexto, solicitacao))
                .chain(solicitacao -> consultarChecklist.executar(
                        new ComandoConsultaChecklist(
                                solicitacao.identificadorChecklist(),
                                solicitacao.versaoChecklist())))
                .onItem().ifNull().failWith(() ->
                        FalhaAnaliseConformidade.checklistInvalido(
                                "Checklist não localizado para a análise"))
                .map(ConsultarChecklistEtapa::congelarEValidar)
                .chain(checklist -> {
                    var referencia = referencias.checklist(
                            contexto.correlationId(), checklist);
                    return estados.registrarChecklist(instanceId, checklist)
                            .replaceWith(contexto.comChecklist(referencia));
                });
    }

    private static void validarIdentidades(
            ContextoAnaliseConformidadeFlow contexto,
            SolicitacaoAnaliseConformidade solicitacao) {
        if (!contexto.correlationId().equals(solicitacao.correlationId())
                || !contexto.identificadorDocumento().equals(
                        solicitacao.identificadorDocumento())
                || !contexto.identificadorChecklist().equals(
                        solicitacao.identificadorChecklist())
                || !contexto.versaoChecklist().equals(
                        solicitacao.versaoChecklist())) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }

    private static Checklist congelarEValidar(Checklist checklist) {
        if (checklist.apontamentos() == null || checklist.apontamentos().isEmpty()) {
            throw FalhaAnaliseConformidade.checklistInvalido(
                    "O checklist da análise não possui apontamentos");
        }
        return new Checklist(
                checklist.nome(),
                checklist.identificadorNegocial(),
                checklist.versao(),
                checklist.dataHoraCriacao(),
                checklist.dataHoraUltimaAlteracao(),
                checklist.verificacaoPrevia(),
                checklist.orientacaoOperador(),
                List.copyOf(checklist.apontamentos()));
    }
}
