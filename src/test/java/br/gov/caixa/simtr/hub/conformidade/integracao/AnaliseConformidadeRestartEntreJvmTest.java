package br.gov.caixa.simtr.hub.conformidade.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.AnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@QuarkusTest
@TestProfile(AnaliseConformidadeFlowQuarkusTest.Perfil.class)
@EnabledIfSystemProperty(named = "restart.proof.enabled", matches = "true")
class AnaliseConformidadeRestartEntreJvmTest {

    private static final String BASE_PATH = "/simtr-hub/v1/conformidade/analises";
    private static final String IDENTIFICADOR_DOCUMENTO = "DOC-RESTART-JVM";
    private static final String PROPRIEDADE_ARQUIVO_ESTADO = "restart.proof.state-file";
    private static final Duration LIMITE = Duration.ofSeconds(15);
    private static final long INTERVALO_CONSULTA_NANOS =
            Duration.ofMillis(25).toNanos();

    @Inject
    IniciarAnaliseConformidade iniciar;

    @Inject
    ArmazenarEstadoAnaliseConformidade estados;

    @Inject
    AnaliseConformidadeFlow flow;

    @Inject
    RedisDataSource redis;

    @Inject
    AnaliseConformidadeFlowQuarkusTest.ConsultarChecklistControlado consultarChecklist;

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void criaInstanciaEmEsperaNaPrimeiraJvm() throws IOException {
        consultarChecklist.preparar(Uni.createFrom().item(checklist()));
        var solicitacao = new SolicitacaoAnaliseConformidade(
                UUID.randomUUID().toString(),
                IDENTIFICADOR_DOCUMENTO,
                "Texto persistido para a prova entre JVMs",
                1000012583L,
                1);

        VisaoAnaliseConformidade inicial = iniciar.executar(solicitacao)
                .await()
                .indefinitely();
        VisaoAnaliseConformidade aguardando = aguardarStatus(
                inicial.instanceId(),
                StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        WorkflowInstance instancia = aguardarInstancia(inicial.instanceId());

        aguardarStatusFlow(instancia, WorkflowStatus.WAITING);
        assertEquals(WorkflowStatus.WAITING, instancia.status());
        assertTrue(
                redis.key(String.class).keys("*").stream()
                        .anyMatch(chave -> chave.contains(inicial.instanceId())),
                "O checkpoint WAITING deve existir no Valkey antes de encerrar a JVM");
        Files.writeString(
                arquivoEstado(),
                aguardando.instanceId(),
                StandardCharsets.UTF_8);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void restauraERetomaNaSegundaJvm() throws IOException {
        String instanceId = Files.readString(
                        arquivoEstado(),
                        StandardCharsets.UTF_8)
                .trim();
        WorkflowInstance restaurada = aguardarInstancia(instanceId);
        VisaoAnaliseConformidade aguardando = aguardarStatus(
                instanceId,
                StatusAnaliseConformidade.AGUARDANDO_REVISAO);

        assertEquals(WorkflowStatus.WAITING, restaurada.status());
        assertEquals(IDENTIFICADOR_DOCUMENTO, aguardando.identificadorDocumento());

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "observacao": "Revisão após restart real.",
                          "apontamentos": [{
                            "identificadorApontamento": 10,
                            "nomeApontamento": "Documento identificado",
                            "parecer": "CONFORME",
                            "justificativa": "Checkpoint restaurado em outra JVM",
                            "evidencia": "CouchDB e Valkey preservados",
                            "confianca": 0.9
                          }]
                        }
                        """)
                .when()
                .put(BASE_PATH + "/{instanceId}/revisao", instanceId)
                .then()
                .statusCode(202);

        VisaoAnaliseConformidade concluida = aguardarStatus(
                instanceId,
                StatusAnaliseConformidade.CONCLUIDA);
        aguardarStatusFlow(restaurada, WorkflowStatus.COMPLETED);
        assertEquals(OrigemResultado.REVISAO_HUMANA, concluida.resultadoFinal().origem());
    }

    private Path arquivoEstado() throws IOException {
        String configurado = System.getProperty(PROPRIEDADE_ARQUIVO_ESTADO);
        if (configurado == null || configurado.isBlank()) {
            throw new IllegalStateException("Arquivo de estado da prova de restart não configurado");
        }
        Path raizPermitida = Path.of("target", "restart-proof")
                .toAbsolutePath()
                .normalize();
        Path arquivo = Path.of(configurado).toAbsolutePath().normalize();
        if (!arquivo.startsWith(raizPermitida)) {
            throw new IllegalArgumentException("Arquivo da prova fora de target/restart-proof");
        }
        Files.createDirectories(raizPermitida);
        return arquivo;
    }

    private WorkflowInstance aguardarInstancia(String instanceId) {
        long limite = System.nanoTime() + LIMITE.toNanos();
        while (System.nanoTime() < limite) {
            var instancia = flow.definition().activeInstance(instanceId);
            if (instancia.isPresent()) {
                return instancia.get();
            }
            pausar();
        }
        throw new AssertionError("A instância Flow não foi restaurada nesta JVM");
    }

    private VisaoAnaliseConformidade aguardarStatus(
            String instanceId,
            StatusAnaliseConformidade esperado) {
        long limite = System.nanoTime() + LIMITE.toNanos();
        VisaoAnaliseConformidade ultima = null;
        while (System.nanoTime() < limite) {
            ultima = estados.consultar(instanceId)
                    .await()
                    .indefinitely()
                    .orElse(null);
            if (ultima != null && ultima.status() == esperado) {
                return ultima;
            }
            pausar();
        }
        throw new AssertionError(
                "A projeção não transitou para " + esperado
                        + "; status=" + (ultima == null ? "ausente" : ultima.status()));
    }

    private static void aguardarStatusFlow(
            WorkflowInstance instancia,
            WorkflowStatus esperado) {
        long limite = System.nanoTime() + LIMITE.toNanos();
        while (System.nanoTime() < limite) {
            if (instancia.status() == esperado) {
                return;
            }
            pausar();
        }
        throw new AssertionError(
                "A instância Flow não transitou para " + esperado
                        + "; status=" + instancia.status());
    }

    private static void pausar() {
        LockSupport.parkNanos(INTERVALO_CONSULTA_NANOS);
        if (Thread.currentThread().isInterrupted()) {
            throw new AssertionError("Espera da prova de restart interrompida");
        }
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
}
