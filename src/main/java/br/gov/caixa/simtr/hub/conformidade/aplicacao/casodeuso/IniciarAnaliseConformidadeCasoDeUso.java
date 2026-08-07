package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.AnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.ContextoAnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import io.serverlessworkflow.impl.WorkflowInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class IniciarAnaliseConformidadeCasoDeUso implements IniciarAnaliseConformidade {

    private static final String MENSAGEM_FALHA_CONSULTA =
            "Não foi possível consultar o checklist para a análise";

    private final ArmazenarEstadoAnaliseConformidade estados;
    private final AnaliseConformidadeFlow flow;

    @Inject
    public IniciarAnaliseConformidadeCasoDeUso(
            ArmazenarEstadoAnaliseConformidade estados,
            AnaliseConformidadeFlow flow) {
        this.estados = estados;
        this.flow = flow;
    }

    @Override
    public Uni<VisaoAnaliseConformidade> executar(
            SolicitacaoAnaliseConformidade solicitacao) {
        return Uni.createFrom().deferred(() -> {
            if (solicitacao == null) {
                throw FalhaAnaliseConformidade.solicitacaoInvalida(
                        "A solicitação da análise é obrigatória");
            }

            WorkflowInstance instancia = flow.instance(
                    ContextoAnaliseConformidadeFlow.inicial(solicitacao));
            String instanceId = instancia.id();
            return estados.iniciar(instanceId, solicitacao)
                    .chain(() -> estados.consultar(instanceId))
                    .map(inicial -> inicial.orElseThrow(
                            FalhaAnaliseConformidade::instanciaNaoEncontrada))
                    .chain(inicial -> iniciarWorkflow(
                            instancia,
                            instanceId,
                            inicial));
        });
    }

    private Uni<VisaoAnaliseConformidade> iniciarWorkflow(
            WorkflowInstance instancia,
            String instanceId,
            VisaoAnaliseConformidade inicial) {
        try {
            instancia.start().whenComplete((resultado, falha) -> {
                if (falha != null) {
                    estados.falhar(instanceId, mensagemFalha(falha))
                            .subscribeAsCompletionStage();
                }
            });
            return Uni.createFrom().item(inicial);
        } catch (RuntimeException _) {
            return estados.falhar(instanceId, MENSAGEM_FALHA_CONSULTA)
                    .chain(() -> Uni.createFrom().failure(
                            FalhaAnaliseConformidade.indisponibilidadeTecnica()));
        }
    }

    private static String mensagemFalha(Throwable falha) {
        Throwable causa = desembrulhar(falha);
        if (causa instanceof FalhaAnaliseConformidade falhaAnalise
                && falhaAnalise.tipo() == FalhaAnaliseConformidade.Tipo.CHECKLIST_INVALIDO) {
            return falhaAnalise.getMessage();
        }
        return MENSAGEM_FALHA_CONSULTA;
    }

    private static Throwable desembrulhar(Throwable falha) {
        Throwable atual = falha;
        while ((atual instanceof CompletionException || atual instanceof ExecutionException)
                && atual.getCause() != null) {
            atual = atual.getCause();
        }
        return atual;
    }
}
