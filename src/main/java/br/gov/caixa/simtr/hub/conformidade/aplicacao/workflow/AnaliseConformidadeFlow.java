package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.agent;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consumed;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emitJson;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.withInstanceId;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.AnalisarTextoComChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.validacao.ValidadorAnaliseConformidade;
import io.quarkiverse.flow.Flow;
import io.smallrye.mutiny.Uni;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AnaliseConformidadeFlow extends Flow {

    private static final String EVENTO_REVISAO_SOLICITADA =
            "br.gov.caixa.simtr.conformidade.revisao.solicitada.v1";
    private static final String EVENTO_REVISAO_CONCLUIDA =
            "br.gov.caixa.simtr.conformidade.revisao.concluida.v1";
    private static final String EVENTO_ANALISE_CONCLUIDA =
            "br.gov.caixa.simtr.conformidade.analise.concluida.v1";
    private static final String EXTENSAO_FLOW_INSTANCE_ID = "flowinstanceid";

    private final ConsultarChecklistEtapa consultarChecklist;
    private final AnalisarTextoComChecklist analisarTexto;
    private final ArmazenarEstadoAnaliseConformidade estados;
    private final ValidadorAnaliseConformidade validador =
            new ValidadorAnaliseConformidade();

    @Inject
    public AnaliseConformidadeFlow(
            ConsultarChecklistEtapa consultarChecklist,
            AnalisarTextoComChecklist analisarTexto,
            ArmazenarEstadoAnaliseConformidade estados) {
        this.consultarChecklist = consultarChecklist;
        this.analisarTexto = analisarTexto;
        this.estados = estados;
    }

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow("analise-conformidade")
                .tasks(function(
                        "consultarChecklist",
                        consultarChecklist::executar,
                        SolicitacaoAnaliseConformidade.class),
                        agent(
                                "analisarConformidade",
                                this::analisar,
                                ContextoAnaliseConformidadeFlow.class),
                        function(
                                "prepararRevisaoHumana",
                                ContextoAnaliseConformidadeFlow::resultado,
                                ContextoAnaliseConformidadeFlow.class),
                        emitJson(
                                "emitirSolicitacaoRevisao",
                                EVENTO_REVISAO_SOLICITADA,
                                ResultadoAnaliseConformidade.class),
                        listen(
                                "aguardarRevisaoHumana",
                                toOne(consumed(EVENTO_REVISAO_CONCLUIDA)
                                        .extensionByInstanceId(EXTENSAO_FLOW_INSTANCE_ID))),
                        withInstanceId(
                                "consolidarRevisaoHumana",
                                this::consolidarRevisao,
                                RevisaoHumanaConformidade[].class),
                        emitJson(
                                "emitirAnaliseConcluida",
                                EVENTO_ANALISE_CONCLUIDA,
                                ResultadoAnaliseConformidade.class))
                .build();
    }

    private Uni<ContextoAnaliseConformidadeFlow> analisar(
            String identificadorInstancia,
            ContextoAnaliseConformidadeFlow contexto) {
        return Uni.createFrom().deferred(() -> {
            if (contexto == null || contexto.resultado() != null) {
                throw FalhaAnaliseConformidade.resultadoInvalido(
                        "O contexto do agente deve possuir checklist e não pode estar analisado");
            }
            String instanceId = identificadorRaiz(identificadorInstancia);
            return estados.registrarChecklist(instanceId, contexto.checklist())
                    .map(ignorado -> {
                        var entrada = new EntradaAnaliseAgente(
                                contexto.texto(),
                                contexto.checklist());
                        var resultado = analisarTexto.analisar(
                                instanceId,
                                entrada);
                        return new ContextoAnaliseConformidadeFlow(
                                contexto.correlationId(),
                                contexto.identificadorDocumento(),
                                contexto.texto(),
                                contexto.checklist(),
                                resultado);
                    });
        });
    }

    private Uni<ResultadoAnaliseConformidade> consolidarRevisao(
            String instanceId,
            RevisaoHumanaConformidade[] revisoes) {
        return Uni.createFrom().deferred(() -> {
            if (revisoes == null || revisoes.length != 1 || revisoes[0] == null) {
                throw FalhaAnaliseConformidade.revisaoInconsistente(
                        "O workflow exige exatamente uma revisão humana");
            }
            return estados.consultar(instanceId)
                    .map(atual -> atual.orElseThrow(
                            FalhaAnaliseConformidade::instanciaNaoEncontrada))
                    .map(atual -> validador.consolidarRevisao(
                            atual.resultadoPreliminar(),
                            revisoes[0]));
        });
    }

    private static String identificadorRaiz(String identificadorTarefa) {
        if (identificadorTarefa == null) {
            return null;
        }
        int inicioCaminhoTarefa = identificadorTarefa.indexOf("-do/");
        return inicioCaminhoTarefa >= 0
                ? identificadorTarefa.substring(0, inicioCaminhoTarefa)
                : identificadorTarefa;
    }
}
