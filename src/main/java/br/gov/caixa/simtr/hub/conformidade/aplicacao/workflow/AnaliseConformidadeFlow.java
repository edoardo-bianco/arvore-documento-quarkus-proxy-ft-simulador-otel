package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.agent;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consumed;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emitJson;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.withInstanceId;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
        .ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.AnalisarTextoComChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
        .ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
        .ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.validacao
        .ValidadorAnaliseConformidade;
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
    private final ReferenciasDocumentoAnaliseConformidade referencias;
    private final ValidadorAnaliseConformidade validador =
            new ValidadorAnaliseConformidade();

    @Inject
    public AnaliseConformidadeFlow(
            ConsultarChecklistEtapa consultarChecklist,
            AnalisarTextoComChecklist analisarTexto,
            ArmazenarEstadoAnaliseConformidade estados,
            ReferenciasDocumentoAnaliseConformidade referencias) {
        this.consultarChecklist = consultarChecklist;
        this.analisarTexto = analisarTexto;
        this.estados = estados;
        this.referencias = referencias;
    }

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow("analise-conformidade")
                .tasks(withInstanceId(
                                "consultarChecklist",
                                consultarChecklist::executar,
                                ContextoAnaliseConformidadeFlow.class),
                        agent(
                                "analisarConformidade",
                                this::analisar,
                                ContextoAnaliseConformidadeFlow.class),
                        emitJson(
                                "emitirSolicitacaoRevisao",
                                EVENTO_REVISAO_SOLICITADA,
                                ReferenciaDocumentoAnaliseConformidade.class),
                        listen(
                                "aguardarRevisaoHumana",
                                toOne(consumed(EVENTO_REVISAO_CONCLUIDA)
                                        .correlate(
                                                EXTENSAO_FLOW_INSTANCE_ID,
                                                correlacao -> correlacao
                                                        .from(".flowinstanceid")
                                                        .expect("$workflow.id")))),
                        withInstanceId(
                                "consolidarRevisaoHumana",
                                this::consolidarRevisao,
                                ReferenciaDocumentoAnaliseConformidade[].class),
                        emitJson(
                                "emitirAnaliseConcluida",
                                EVENTO_ANALISE_CONCLUIDA,
                                ReferenciaDocumentoAnaliseConformidade.class))
                .build();
    }

    private Uni<ReferenciaDocumentoAnaliseConformidade> analisar(
            String identificadorInstancia,
            ContextoAnaliseConformidadeFlow contexto) {
        return Uni.createFrom().deferred(() -> {
            if (contexto == null || contexto.checklistRef() == null) {
                throw FalhaAnaliseConformidade.resultadoInvalido(
                        "O contexto do agente deve possuir referência do checklist");
            }
            String instanceId = identificadorRaiz(identificadorInstancia);
            return estados.carregarSolicitacao(instanceId)
                    .chain(solicitacao -> estados.carregarChecklist(
                                    instanceId,
                                    contexto.checklistRef())
                            .map(checklist -> analisarTexto.analisar(
                                    instanceId,
                                    new EntradaAnaliseAgente(
                                            solicitacao.texto(),
                                            checklist))))
                    .chain(resultado -> {
                        var referencia = referencias.resultadoPreliminar(
                                contexto.correlationId(), resultado);
                        return estados.prepararResultadoPreliminar(
                                        instanceId,
                                        resultado,
                                        referencia)
                                .replaceWith(referencia);
                    });
        });
    }

    private Uni<ReferenciaDocumentoAnaliseConformidade> consolidarRevisao(
            String identificadorInstancia,
            ReferenciaDocumentoAnaliseConformidade[] revisoes) {
        return Uni.createFrom().deferred(() -> {
            if (revisoes == null || revisoes.length != 1 || revisoes[0] == null) {
                throw FalhaAnaliseConformidade.revisaoInconsistente(
                        "O workflow exige exatamente uma referência de revisão humana");
            }
            String instanceId = identificadorRaiz(identificadorInstancia);
            return estados.consultar(instanceId)
                    .map(atual -> atual.orElseThrow(
                            FalhaAnaliseConformidade::instanciaNaoEncontrada))
                    .chain(atual -> estados.carregarRevisao(instanceId, revisoes[0])
                            .map(revisao -> validador.consolidarRevisao(
                                    atual.resultadoPreliminar(),
                                    revisao))
                            .chain(resultado -> {
                                var referencia = referencias.resultadoFinal(
                                        atual.correlationId(), resultado);
                                return estados.prepararResultadoFinal(
                                                instanceId,
                                                resultado,
                                                referencia)
                                        .replaceWith(referencia);
                            }));
        });
    }

    static String identificadorRaiz(String identificadorTarefa) {
        if (identificadorTarefa == null) {
            return null;
        }
        int inicioCaminhoTarefa = identificadorTarefa.indexOf("-do/");
        return inicioCaminhoTarefa >= 0
                ? identificadorTarefa.substring(0, inicioCaminhoTarefa)
                : identificadorTarefa;
    }
}
