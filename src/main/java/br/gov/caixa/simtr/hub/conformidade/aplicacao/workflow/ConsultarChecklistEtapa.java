package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ConsultarChecklistEtapa {

    private final ConsultarChecklist consultarChecklist;

    @Inject
    public ConsultarChecklistEtapa(ConsultarChecklist consultarChecklist) {
        this.consultarChecklist = consultarChecklist;
    }

    public Uni<ContextoAnaliseConformidadeFlow> executar(
            SolicitacaoAnaliseConformidade solicitacao) {
        if (solicitacao == null) {
            return Uni.createFrom().failure(
                    FalhaAnaliseConformidade.solicitacaoInvalida(
                            "A solicitação da análise é obrigatória"));
        }

        var comando = new ComandoConsultaChecklist(
                solicitacao.identificadorChecklist(),
                solicitacao.versaoChecklist());
        return consultarChecklist.executar(comando)
                .onItem().ifNull().failWith(() ->
                        FalhaAnaliseConformidade.checklistInvalido(
                                "Checklist não localizado para a análise"))
                .map(checklist -> contexto(solicitacao, checklist));
    }

    private static ContextoAnaliseConformidadeFlow contexto(
            SolicitacaoAnaliseConformidade solicitacao,
            Checklist checklist) {
        if (checklist.apontamentos() == null || checklist.apontamentos().isEmpty()) {
            throw FalhaAnaliseConformidade.checklistInvalido(
                    "O checklist da análise não possui apontamentos");
        }
        return new ContextoAnaliseConformidadeFlow(
                solicitacao.correlationId(),
                solicitacao.identificadorDocumento(),
                solicitacao.texto(),
                congelar(checklist),
                null);
    }

    private static Checklist congelar(Checklist checklist) {
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
