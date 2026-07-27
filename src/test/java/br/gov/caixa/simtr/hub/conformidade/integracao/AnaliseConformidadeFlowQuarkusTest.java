package br.gov.caixa.simtr.hub.conformidade.integracao;

import br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.ChecklistObservabilidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.AnalisarTextoComChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.AnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.suporte.CouchDbQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@QuarkusTestResource(
        value = CouchDbQuarkusTestResource.class,
        restrictToAnnotatedClass = true)
@TestProfile(AnaliseConformidadeFlowQuarkusTest.Perfil.class)
class AnaliseConformidadeFlowQuarkusTest {

    private static final String BASE_PATH = "/simtr-hub/v1/conformidade/analises";
    private static final String CORRELATION_ID = "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String IDENTIFICADOR_DOCUMENTO = "DOC-2026-000123";

    @Inject
    IniciarAnaliseConformidade iniciar;

    @Inject
    ArmazenarEstadoAnaliseConformidade estados;

    @Inject
    AnaliseConformidadeFlow flow;

    @Inject
    ConsultarChecklistControlado consultarChecklist;

    @Inject
    AnalisarTextoControlado analisarTexto;

    @BeforeEach
    void prepararAgente() {
        analisarTexto.preparar();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void postIniciaFlowERetornaEnquantoConsultaPermanecePendente() {
        var controlado = new CompletableFuture<Checklist>();
        consultarChecklist.preparar(
                Uni.createFrom().completionStage(controlado));

        var resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "identificadorDocumento": "DOC-2026-000123",
                          "texto": "Texto documental",
                          "identificadorChecklist": 1000012583,
                          "versaoChecklist": 1
                        }
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(202)
                .extract()
                .response();
        String instanceId = resposta.path("instanceId");
        VisaoAnaliseConformidade inicial = estados.consultar(instanceId)
                .await()
                .indefinitely()
                .orElseThrow();

        assertEquals(resposta.path("correlationId"), inicial.correlationId());
        assertEquals(IDENTIFICADOR_DOCUMENTO, inicial.identificadorDocumento());
        assertEquals(StatusAnaliseConformidade.EM_PROCESSAMENTO, inicial.status());
        assertEquals("EM_PROCESSAMENTO", resposta.path("status"));
        assertEquals(BASE_PATH + "/" + instanceId, resposta.header("Location"));
        assertFalse(controlado.isDone());
        WorkflowInstance instancia = instanciaAtiva(instanceId);
        assertEquals(instanceId, instancia.id());

        ComandoConsultaChecklist comando = consultarChecklist.comandoRecebido();
        assertEquals(1000012583L, comando.identificadorNegocial());
        assertEquals(1, comando.versao());

        controlado.complete(checklist());
        VisaoAnaliseConformidade aguardando = aguardarStatus(
                instanceId,
                StatusAnaliseConformidade.AGUARDANDO_REVISAO);

        aguardarStatusFlow(instancia, WorkflowStatus.WAITING);
        assertEquals(OrigemResultado.AGENTE, aguardando.resultadoPreliminar().origem());
        assertEquals(10L, aguardando.resultadoPreliminar()
                .apontamentos().get(0).identificadorApontamento());
        assertEquals(instanceId, analisarTexto.identificadorMemoria());
        assertEquals("Texto documental", analisarTexto.entradaRecebida().texto());
        assertEquals(1, analisarTexto.invocacoes());
    }

    @Test
    void putPublicaRevisaoCorrelacionadaERetomaAteConclusao() {
        consultarChecklist.preparar(Uni.createFrom().item(checklist()));

        String instanceId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "identificadorDocumento": "DOC-2026-000123",
                          "texto": "Texto documental",
                          "identificadorChecklist": 1000012583,
                          "versaoChecklist": 1
                        }
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(202)
                .extract()
                .path("instanceId");
        WorkflowInstance instancia = instanciaAtiva(instanceId);
        aguardarStatus(instanceId, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        aguardarStatusFlow(instancia, WorkflowStatus.WAITING);

        given()
                .accept(ContentType.JSON)
                .when()
                .get(BASE_PATH + "/{instanceId}", instanceId)
                .then()
                .statusCode(200)
                .body("status", equalTo("AGUARDANDO_REVISAO"));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "observacao": "Revisão realizada pelo operador.",
                          "apontamentos": [{
                            "identificadorApontamento": 10,
                            "nomeApontamento": "Documento identificado",
                            "parecer": "INCONFORME",
                            "justificativa": "Revisado pelo operador",
                            "evidencia": "Evidência humana",
                            "confianca": 0.9
                          }]
                        }
                        """)
                .when()
                .put(BASE_PATH + "/{instanceId}/revisao", instanceId)
                .then()
                .statusCode(202);

        VisaoAnaliseConformidade concluida =
                aguardarStatus(instanceId, StatusAnaliseConformidade.CONCLUIDA);
        aguardarStatusFlow(instancia, WorkflowStatus.COMPLETED);

        assertEquals(OrigemResultado.REVISAO_HUMANA, concluida.resultadoFinal().origem());
        assertEquals(IDENTIFICADOR_DOCUMENTO, concluida.identificadorDocumento());
        assertEquals(1000012583L, concluida.identificadorChecklist());
        assertEquals(1, concluida.versaoChecklist());
        assertEquals(
                ParecerConformidade.INCONFORME,
                concluida.resultadoFinal().apontamentos().getFirst().parecer());

        given()
                .accept(ContentType.JSON)
                .when()
                .get(BASE_PATH + "/{instanceId}", instanceId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONCLUIDA"))
                .body(
                        "resultadoFinal.apontamentos[0].parecer",
                        equalTo("INCONFORME"));
    }

    @Test
    void revisaoRetomaSomenteAInstanciaCorrelacionada() {
        String primeira = iniciarAnaliseCompleta();
        String segunda = iniciarAnaliseCompleta();
        WorkflowInstance flowPrimeira = instanciaAtiva(primeira);
        WorkflowInstance flowSegunda = instanciaAtiva(segunda);
        aguardarStatus(primeira, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        aguardarStatus(segunda, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        aguardarStatusFlow(flowPrimeira, WorkflowStatus.WAITING);
        aguardarStatusFlow(flowSegunda, WorkflowStatus.WAITING);

        enviarRevisao(primeira, 0.9d).statusCode(202);

        aguardarStatus(primeira, StatusAnaliseConformidade.CONCLUIDA);
        aguardarStatusFlow(flowPrimeira, WorkflowStatus.COMPLETED);
        assertEquals(
                StatusAnaliseConformidade.AGUARDANDO_REVISAO,
                estados.consultar(segunda)
                        .await()
                        .indefinitely()
                        .orElseThrow()
                        .status());
        assertEquals(WorkflowStatus.WAITING, flowSegunda.status());

        enviarRevisao(segunda, 0.9d).statusCode(202);

        aguardarStatus(segunda, StatusAnaliseConformidade.CONCLUIDA);
        aguardarStatusFlow(flowSegunda, WorkflowStatus.COMPLETED);
    }

    @Test
    void revisaoInconsistenteNaoRetomaEDuplicadaRetornaConflito() {
        String instanceId = iniciarAnaliseCompleta();
        WorkflowInstance instancia = instanciaAtiva(instanceId);
        aguardarStatus(instanceId, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        aguardarStatusFlow(instancia, WorkflowStatus.WAITING);

        enviarRevisao(instanceId, 0.8d).statusCode(422);

        assertEquals(
                StatusAnaliseConformidade.AGUARDANDO_REVISAO,
                estados.consultar(instanceId)
                        .await()
                        .indefinitely()
                        .orElseThrow()
                        .status());
        assertEquals(WorkflowStatus.WAITING, instancia.status());

        enviarRevisao(instanceId, 0.9d).statusCode(202);
        aguardarStatus(instanceId, StatusAnaliseConformidade.CONCLUIDA);
        aguardarStatusFlow(instancia, WorkflowStatus.COMPLETED);
        enviarRevisao(instanceId, 0.9d).statusCode(409);
    }

    @Test
    void checklistNuloFalhaAProjecaoSemNovaTentativa() {
        consultarChecklist.preparar(Uni.createFrom().nullItem());

        VisaoAnaliseConformidade inicial = iniciar.executar(solicitacaoDireta())
                .await().indefinitely();
        VisaoAnaliseConformidade falhou = aguardarFalha(inicial.instanceId());

        assertEquals(StatusAnaliseConformidade.EM_PROCESSAMENTO, inicial.status());
        assertEquals(
                "Checklist não localizado para a análise",
                falhou.mensagemErro());
        assertEquals(1, consultarChecklist.invocacoes());
    }

    @Test
    void falhaTecnicaFicaSanitizadaSemNovaTentativa() {
        consultarChecklist.preparar(Uni.createFrom().failure(
                new IllegalStateException("detalhe interno sensível")));

        VisaoAnaliseConformidade inicial = iniciar.executar(solicitacaoDireta())
                .await().indefinitely();
        VisaoAnaliseConformidade falhou = aguardarFalha(inicial.instanceId());

        assertEquals(StatusAnaliseConformidade.EM_PROCESSAMENTO, inicial.status());
        assertEquals(
                "Não foi possível consultar o checklist para a análise",
                falhou.mensagemErro());
        assertFalse(falhou.mensagemErro().contains("detalhe interno"));
        assertEquals(1, consultarChecklist.invocacoes());
    }

    private WorkflowInstance instanciaAtiva(String instanceId) {
        long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < limite) {
            var encontrada = flow.definition().activeInstance(instanceId);
            if (encontrada.isPresent()) {
                return encontrada.get();
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("Instância Flow não ficou ativa");
    }

    private String iniciarAnaliseCompleta() {
        consultarChecklist.preparar(Uni.createFrom().item(checklist()));
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "identificadorDocumento": "DOC-2026-000123",
                          "texto": "Texto documental",
                          "identificadorChecklist": 1000012583,
                          "versaoChecklist": 1
                        }
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(202)
                .extract()
                .path("instanceId");
    }

    private static ValidatableResponse enviarRevisao(
            String instanceId,
            double confianca) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "observacao": "Revisão realizada pelo operador.",
                          "apontamentos": [{
                            "identificadorApontamento": 10,
                            "nomeApontamento": "Documento identificado",
                            "parecer": "INCONFORME",
                            "justificativa": "Revisado pelo operador",
                            "evidencia": "Evidência humana",
                            "confianca": %s
                          }]
                        }
                        """.formatted(confianca))
                .when()
                .put(BASE_PATH + "/{instanceId}/revisao", instanceId)
                .then();
    }

    private VisaoAnaliseConformidade aguardarFalha(String instanceId) {
        return aguardarStatus(instanceId, StatusAnaliseConformidade.FALHOU);
    }

    private static void aguardarStatusFlow(
            WorkflowInstance instancia,
            WorkflowStatus statusEsperado) {
        long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < limite) {
            if (instancia.status() == statusEsperado) {
                return;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("A instância Flow não transitou para " + statusEsperado);
    }

    private VisaoAnaliseConformidade aguardarStatus(
            String instanceId,
            StatusAnaliseConformidade statusEsperado) {
        long limite = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < limite) {
            VisaoAnaliseConformidade atual = estados.consultar(instanceId)
                    .await()
                    .indefinitely()
                    .orElseThrow();
            if (atual.status() == statusEsperado) {
                return atual;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("A projeção não transitou para " + statusEsperado);
    }

    private static Checklist checklist() {
        return new Checklist(
                "Checklist documental",
                1000012583L,
                1,
                "2026-07-24T10:00:00-03:00",
                "2026-07-24T10:00:00-03:00",
                false,
                "Orientação",
                List.of(new ApontamentoChecklist(
                        10L,
                        "Documento identificado",
                        "Verificar documento",
                        "Conferir conteúdo",
                        false,
                        1)));
    }

    private static SolicitacaoAnaliseConformidade solicitacaoDireta() {
        return new SolicitacaoAnaliseConformidade(
                java.util.UUID.randomUUID().toString(),
                IDENTIFICADOR_DOCUMENTO,
                "Texto documental",
                1000012583L,
                1);
    }

    public static final class Perfil implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(
                    ConsultarChecklistControlado.class,
                    AnalisarTextoControlado.class);
        }
    }

    @Alternative
    @ApplicationScoped
    public static class ConsultarChecklistControlado
            extends ChecklistObservabilidade {

        private final AtomicInteger invocacoes = new AtomicInteger();
        private volatile Uni<Checklist> resultado;
        private volatile ComandoConsultaChecklist comandoRecebido;

        public ConsultarChecklistControlado() {
            super(
                    comando -> Uni.createFrom().failure(
                            new IllegalStateException("Fake não preparado")),
                    false);
        }

        public void preparar(Uni<Checklist> novoResultado) {
            resultado = novoResultado;
            comandoRecebido = null;
            invocacoes.set(0);
        }

        @Override
        public Uni<Checklist> executar(ComandoConsultaChecklist comando) {
            comandoRecebido = comando;
            invocacoes.incrementAndGet();
            return resultado;
        }

        public ComandoConsultaChecklist comandoRecebido() {
            return comandoRecebido;
        }

        public int invocacoes() {
            return invocacoes.get();
        }
    }

    @Alternative
    @ApplicationScoped
    public static class AnalisarTextoControlado
            implements AnalisarTextoComChecklist {

        private final AtomicInteger invocacoes = new AtomicInteger();
        private volatile String identificadorMemoria;
        private volatile EntradaAnaliseAgente entradaRecebida;

        public void preparar() {
            invocacoes.set(0);
            identificadorMemoria = null;
            entradaRecebida = null;
        }

        @Override
        public ResultadoAnaliseConformidade analisar(
                String novoIdentificadorMemoria,
                EntradaAnaliseAgente novaEntrada) {
            invocacoes.incrementAndGet();
            identificadorMemoria = novoIdentificadorMemoria;
            entradaRecebida = novaEntrada;
            ApontamentoChecklist apontamento = novaEntrada.checklist().apontamentos().get(0);
            return new ResultadoAnaliseConformidade(
                    novaEntrada.checklist().identificadorNegocial(),
                    novaEntrada.checklist().versao(),
                    novaEntrada.checklist().nome(),
                    "Resumo preliminar controlado",
                    List.of(new ResultadoApontamentoConformidade(
                            apontamento.identificadorNegocial(),
                            apontamento.nome(),
                            ParecerConformidade.CONFORME,
                            "Justificativa controlada",
                            "Evidência controlada",
                            0.9d)),
                    OrigemResultado.AGENTE);
        }

        public String identificadorMemoria() {
            return identificadorMemoria;
        }

        public EntradaAnaliseAgente entradaRecebida() {
            return entradaRecebida;
        }

        public int invocacoes() {
            return invocacoes.get();
        }
    }
}
