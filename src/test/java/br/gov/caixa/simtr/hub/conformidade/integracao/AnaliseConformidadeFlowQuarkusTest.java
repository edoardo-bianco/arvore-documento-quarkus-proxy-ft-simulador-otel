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
import br.gov.caixa.simtr.hub.conformidade.suporte.ValkeyQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceHandlers;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ValkeyQuarkusTestResource.class)
@QuarkusTestResource(
        value = CouchDbQuarkusTestResource.class,
        restrictToAnnotatedClass = true)
@TestProfile(AnaliseConformidadeFlowQuarkusTest.Perfil.class)
class AnaliseConformidadeFlowQuarkusTest {

    private static final String BASE_PATH = "/simtr-hub/v1/conformidade/analises";
    private static final String CHECK_DOCUMENTAL =
            "simtr-hub-conformidade-documental";
    private static final String CORRELATION_ID = "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String IDENTIFICADOR_DOCUMENTO = "DOC-2026-000123";
    private static final Duration LIMITE_CONVERGENCIA = Duration.ofSeconds(10);
    private static final long INTERVALO_CONSULTA_NANOS =
            Duration.ofMillis(10).toNanos();

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

    @Inject
    RedisDataSource redis;

    @Inject
    PersistenceInstanceHandlers persistenceHandlers;

    @BeforeEach
    void prepararAgente() {
        analisarTexto.preparar();
    }

    @Test
    void readinessDistingueBackendDocumentalRedisELeaseSemDadosSensiveis() {
        var resposta = given()
                .get("/q/health/ready")
                .then()
                .extract()
                .response();

        assertEquals(200, resposta.statusCode(), resposta.asPrettyString());
        List<Map<String, Object>> checks = resposta.jsonPath()
                .getList("checks");
        Map<String, Object> documental = checks.stream()
                .filter(check -> CHECK_DOCUMENTAL.equals(check.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(resposta.asPrettyString()));
        assertEquals("UP", documental.get("status"));
        assertEquals(
                "couchdb",
                ((Map<?, ?>) documental.get("data")).get("backend"));
        assertTrue(checks.stream().anyMatch(check -> String.valueOf(check.get("name"))
                .toLowerCase(Locale.ROOT)
                .contains("redis")), resposta.asPrettyString());
        Map<String, Object> lease = checks.stream()
                .filter(check -> "Lease Acquisition".equals(check.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(resposta.asPrettyString()));
        assertEquals("UP", lease.get("status"));
        assertTrue(((Map<?, ?>) lease.get("data")).containsKey("leaseEnabled"));
        assertTrue(((Map<?, ?>) lease.get("data")).containsKey("leaseAcquired"));

        String corpo = resposta.asString().toLowerCase(Locale.ROOT);
        assertFalse(corpo.contains("password"));
        assertFalse(corpo.contains("credential"));
        assertFalse(corpo.contains("endpoint"));
        assertFalse(corpo.contains("texto"));
        assertFalse(corpo.contains("revisao"));
        assertFalse(corpo.contains("evidencia"));
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
        aguardarStatusFlow(instancia, WorkflowStatus.WAITING);
        VisaoAnaliseConformidade aguardando = aguardarStatus(
                instanceId,
                StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        assertEquals(OrigemResultado.AGENTE, aguardando.resultadoPreliminar().origem());
        assertEquals(10L, aguardando.resultadoPreliminar()
                .apontamentos().get(0).identificadorApontamento());
        assertEquals(instanceId, analisarTexto.identificadorMemoria());
        assertEquals("Texto documental", analisarTexto.entradaRecebida().texto());
        assertEquals(1, analisarTexto.invocacoes());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void checkpointRedisEhReferencialERestauraInstanciaEmEspera() throws Exception {
        String textoProtegido = "CONTEUDO_DOCUMENTO_NAO_PODE_IR_REDIS";
        consultarChecklist.preparar(Uni.createFrom().item(checklist()));
        var solicitacao = new SolicitacaoAnaliseConformidade(
                java.util.UUID.randomUUID().toString(),
                IDENTIFICADOR_DOCUMENTO,
                textoProtegido,
                1000012583L,
                1);

        VisaoAnaliseConformidade inicial = iniciar.executar(solicitacao)
                .await().indefinitely();
        WorkflowInstance instanciaEmMemoria = instanciaAtiva(inicial.instanceId());
        aguardarStatus(inicial.instanceId(), StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        aguardarStatusFlow(instanciaEmMemoria, WorkflowStatus.WAITING);

        List<String> chaves = redis.key(String.class).keys("*");
        assertTrue(
                chaves.stream().anyMatch(chave -> chave.contains(inicial.instanceId())),
                "O checkpoint da instância em espera deve existir no Valkey");
        String checkpoint = conteudoHashesRedis(chaves);
        for (String conteudoNegocial : List.of(
                textoProtegido,
                "Checklist documental",
                "Verificar documento",
                "Resumo preliminar controlado",
                "Justificativa controlada")) {
            assertFalse(
                    checkpoint.contains(conteudoNegocial),
                    "O checkpoint Redis não pode conter payload negocial");
        }

        simularDescarteDoEstadoVolatil(flow.definition(), instanciaEmMemoria);
        assertTrue(flow.definition().activeInstance(inicial.instanceId()).isEmpty());
        WorkflowInstance restaurada = persistenceHandlers.reader()
                .find(flow.definition(), inicial.instanceId())
                .orElseThrow();
        assertNotSame(instanciaEmMemoria, restaurada);
        assertEquals(inicial.instanceId(), restaurada.id());
        restaurada.start();
        aguardarStatusFlow(restaurada, WorkflowStatus.WAITING);
        assertEquals(
                textoProtegido,
                estados.carregarSolicitacao(inicial.instanceId())
                        .await().indefinitely().texto());
        assertEquals(
                "couchdb",
                org.eclipse.microprofile.config.ConfigProvider.getConfig()
                        .getValue("conformidade.persistencia.backend", String.class));
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

        aguardarStatusFlow(flowSegunda, WorkflowStatus.COMPLETED);
        aguardarStatus(segunda, StatusAnaliseConformidade.CONCLUIDA);
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
        long limite = System.nanoTime() + LIMITE_CONVERGENCIA.toNanos();
        while (System.nanoTime() < limite) {
            var encontrada = flow.definition().activeInstance(instanceId);
            if (encontrada.isPresent()) {
                return encontrada.get();
            }
            LockSupport.parkNanos(INTERVALO_CONSULTA_NANOS);
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
        long limite = System.nanoTime() + LIMITE_CONVERGENCIA.toNanos();
        while (System.nanoTime() < limite) {
            if (instancia.status() == statusEsperado) {
                return;
            }
            LockSupport.parkNanos(INTERVALO_CONSULTA_NANOS);
        }
        if (instancia.status() == WorkflowStatus.FAULTED) {
            try {
                instancia.start().join();
            } catch (java.util.concurrent.CompletionException falha) {
                Throwable causa = falha.getCause();
                throw new AssertionError(
                        "A instância Flow falhou em " + causa.getClass().getSimpleName()
                                + ": " + causa.getMessage());
            }
        }
        throw new AssertionError(
                "A instância Flow não transitou para " + statusEsperado
                        + "; status atual=" + instancia.status());
    }

    private VisaoAnaliseConformidade aguardarStatus(
            String instanceId,
            StatusAnaliseConformidade statusEsperado) {
        long limite = System.nanoTime() + LIMITE_CONVERGENCIA.toNanos();
        VisaoAnaliseConformidade ultima = null;
        while (System.nanoTime() < limite) {
            ultima = estados.consultar(instanceId)
                    .await()
                    .indefinitely()
                    .orElseThrow();
            if (ultima.status() == statusEsperado) {
                return ultima;
            }
            LockSupport.parkNanos(INTERVALO_CONSULTA_NANOS);
        }
        throw new AssertionError(
                "A projeção não transitou para " + statusEsperado
                        + "; status atual=" + (ultima == null ? "ausente" : ultima.status())
                        + "; erro=" + (ultima == null ? null : ultima.mensagemErro()));
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

    private String conteudoHashesRedis(List<String> chaves) {
        var hashes = redis.hash(String.class, String.class, byte[].class);
        var tipos = redis.key(String.class);
        var conteudo = new StringBuilder();
        for (String chave : chaves) {
            if (tipos.type(chave) == RedisValueType.HASH) {
                hashes.hgetall(chave).values().forEach(valor -> conteudo.append(
                        new String(valor, StandardCharsets.ISO_8859_1)));
            }
        }
        return conteudo.toString();
    }

    @SuppressWarnings("java:S3011")
    private static void simularDescarteDoEstadoVolatil(
            io.serverlessworkflow.impl.WorkflowDefinition definicao,
            WorkflowInstance instancia) throws ReflectiveOperationException {
        var remover = definicao.getClass().getDeclaredMethod(
                "removeInstance", WorkflowInstance.class);
        remover.setAccessible(true);
        remover.invoke(definicao, instancia);
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
