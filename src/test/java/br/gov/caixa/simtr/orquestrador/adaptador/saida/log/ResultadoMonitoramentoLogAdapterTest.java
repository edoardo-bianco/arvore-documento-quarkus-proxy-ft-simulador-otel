package br.gov.caixa.simtr.orquestrador.adaptador.saida.log;

import static org.junit.jupiter.api.Assertions.*;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.ReceberResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.RegistrarResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@TestProfile(ResultadoMonitoramentoLogAdapterTest.LogJsonProfile.class)
class ResultadoMonitoramentoLogAdapterTest {
    private static final String EVENTO = "orquestrador.monitoramento-dossie.resultado.registrado";
    private static final Path ARQUIVO = Path.of("target/logs/orquestrador-resultado-test.json");
    private static final Duration ESPERA = Duration.ofSeconds(3);

    @Inject RegistrarResultadoMonitoramento registro;
    @Inject ReceberResultadoMonitoramento recebimento;
    @Inject ObjectMapper json;
    private int primeiraLinha;

    public static final class LogJsonProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.log.file.enabled", "true",
                    "quarkus.log.file.path", ARQUIVO.toString(),
                    "quarkus.log.file.json.enabled", "true",
                    "quarkus.log.console.json.enabled", "true");
        }
    }

    @BeforeEach
    void iniciarCaptura() throws IOException {
        primeiraLinha = Files.readAllLines(ARQUIVO).size();
    }

    @Test
    void deveRegistrarSomenteNaAssinaturaComCamposPermitidosSemRepetir() throws IOException {
        var operacao = new ResultadoMonitoramentoLogAdapter().executar(resultado("MON-1", "ORQ-1"));
        assertTrue(registros().isEmpty());
        assertNull(operacao.await().atMost(ESPERA));
        assertNull(operacao.await().atMost(ESPERA));

        var registros = registros();
        assertEquals(1, registros.size());
        var log = registros.getFirst();
        assertEquals("INFO", log.path("level").asText());
        assertEquals(EVENTO, log.path("message").asText());
        assertEquals(ResultadoMonitoramentoLogAdapter.class.getName(), log.path("loggerName").asText());
        var campos = log.path("mdc");
        assertEquals("MON-1", campos.path("monitoramento_id").asText());
        assertEquals("ORQ-1", campos.path("orquestracao_id").asText());
        assertEquals("adaptador", campos.path("camada").asText());
        assertEquals("ResultadoMonitoramentoLogAdapter", campos.path("componente").asText());
        assertEquals("registrar", campos.path("operacao").asText());
        assertTrue(campos.path("monitoramento_id").isTextual());
        assertTrue(campos.path("orquestracao_id").isTextual());
        assertFalse(log.has("exception"));
        assertFalse(log.has("stacktrace"));
        assertFalse(log.has("idDossieMtr"));
        assertFalse(log.toString().contains("pre-dado-sensivel"));
        assertFalse(log.toString().contains("MOTIVO_SENTINELA"));
    }

    @Test
    void deveConectarAsDuasPortasPorCdiESubmeterUmRegistro() throws IOException {
        recebimento.executar(resultado("MON-CDI", "ORQ-CDI")).await().atMost(ESPERA);
        var registros = registros();
        assertEquals(1, registros.size());
        assertEquals("MON-CDI", registros.getFirst().path("mdc").path("monitoramento_id").asText());
    }

    @Test
    void devePreservarContextoSemDeixarIdsNoMdcDaProximaMensagem() throws IOException {
        var anterior = MDC.get("contexto_teste");
        var monAnterior = MDC.get("monitoramento_id");
        MDC.put("contexto_teste", "contexto-preservado");
        try {
            registro.executar(resultado("MON-CONTEXTO", "ORQ-CONTEXTO")).await().atMost(ESPERA);
            assertEquals("contexto-preservado", MDC.get("contexto_teste"));
            assertEquals(monAnterior, MDC.get("monitoramento_id"));
            MDC.remove("contexto_teste");
            registro.executar(resultado("MON-SEGUINTE", "ORQ-SEGUINTE")).await().atMost(ESPERA);

            var registros = registros();
            assertEquals(2, registros.size());
            assertEquals("contexto-preservado", registros.getFirst().path("mdc").path("contexto_teste").asText());
            assertFalse(registros.getLast().path("mdc").has("contexto_teste"));
            assertEquals("MON-SEGUINTE", registros.getLast().path("mdc").path("monitoramento_id").asText());
        } finally {
            if (anterior == null) {
                MDC.remove("contexto_teste");
            } else {
                MDC.put("contexto_teste", anterior);
            }
        }
    }

    @Test
    void deveIsolarIdsEmInvocacoesConcorrentes() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tarefas = java.util.stream.IntStream.range(0, 16).mapToObj(numero ->
                    executor.submit(() -> registro.executar(resultado("MON-" + numero, "ORQ-" + numero))
                            .await().atMost(ESPERA))).toList();
            for (var tarefa : tarefas) {
                tarefa.get(3, TimeUnit.SECONDS);
            }
        }
        var registros = registros();
        assertEquals(16, registros.size());
        var ids = new java.util.HashSet<String>();
        for (var log : registros) {
            var mon = log.path("mdc").path("monitoramento_id").asText();
            assertTrue(ids.add(mon));
            assertEquals(mon.replace("MON-", "ORQ-"), log.path("mdc").path("orquestracao_id").asText());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void deveFalharAntesDaSubmissaoQuandoFaltaResultadoOuIdentidade(int caso) throws IOException {
        var resultado = switch (caso) {
            case 0 -> null;
            case 1 -> resultado(null, "ORQ-1");
            default -> resultado("MON-1", null);
        };
        var espera = registro.executar(resultado).await();
        assertThrows(NullPointerException.class, () -> espera.atMost(ESPERA));
        assertTrue(registros().isEmpty());
    }

    private List<JsonNode> registros() throws IOException {
        var linhas = Files.readAllLines(ARQUIVO);
        var registros = new ArrayList<JsonNode>();
        for (String linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registroJson = json.readTree(linha);
            if (EVENTO.equals(registroJson.path("mdc").path("evento").asText())) {
                registros.add(registroJson);
            }
        }
        return registros;
    }

    private static ResultadoMonitoramento resultado(String monitoramentoId, String orquestracaoId) {
        return new ResultadoMonitoramento(monitoramentoId, orquestracaoId, "pre-dado-sensivel", "0007",
                "CONCLUSIVO", "FINALIZADO_CONFORME", "CONFORME", "MOTIVO_SENTINELA", 2,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(5), 42L);
    }
}
