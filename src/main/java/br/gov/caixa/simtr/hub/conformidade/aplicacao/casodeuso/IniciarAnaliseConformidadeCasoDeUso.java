package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.AnaliseConformidadeFlow;
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
        return Uni.createFrom().item(() -> iniciar(solicitacao));
    }

    private VisaoAnaliseConformidade iniciar(SolicitacaoAnaliseConformidade solicitacao) {
        if (solicitacao == null) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "A solicitação da análise é obrigatória");
        }

        WorkflowInstance instancia = flow.instance(solicitacao);
        String instanceId = instancia.id();
        estados.iniciar(instanceId, solicitacao);
        VisaoAnaliseConformidade inicial = estados.consultar(instanceId)
                .orElseThrow(FalhaAnaliseConformidade::instanciaNaoEncontrada);

        try {
            instancia.start().whenComplete((resultado, falha) -> {
                if (falha != null) {
                    estados.falhar(instanceId, mensagemFalha(falha));
                }
            });
        } catch (RuntimeException _) {
            estados.falhar(instanceId, MENSAGEM_FALHA_CONSULTA);
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }

        return inicial;
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
