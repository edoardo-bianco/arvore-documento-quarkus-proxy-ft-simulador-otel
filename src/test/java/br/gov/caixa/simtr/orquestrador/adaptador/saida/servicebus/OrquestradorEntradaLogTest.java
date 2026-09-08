package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(OrquestradorEntradaLogTest.LogJsonProfile.class)
class OrquestradorEntradaLogTest {

    private static final String EVENTO = "orquestrador.servicebus.entrada.falhou";
    private static final String SEGREDO = "SEGREDO_SERIALIZACAO_SENTINELA";
    private static final Path ARQUIVO = Path.of("target/logs/orquestrador-entrada-erros-test.json");

    @Inject
    ObjectMapper json;
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
    void registrarInicioDaCaptura() throws IOException {
        primeiraLinha = Files.readAllLines(ARQUIVO).size();
    }

    @Test
    void deveRegistrarUmaFalhaDeSerializacaoNoJsonPadrao() throws IOException {
        var serializador = mock(ObjectMapper.class);
        var original = new JsonGenerationException(SEGREDO, (com.fasterxml.jackson.core.JsonGenerator) null);
        when(serializador.writeValueAsString(any())).thenThrow(original);
        var tentativa = tentativa();

        assertThrows(RuntimeException.class,
                () -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, serializador));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        assertEquals("ERROR", registro.path("level").asText());
        assertTrue(registro.path("erros").isArray());
        assertEquals(json.readTree("[{\"mensagem\":\"Falha ao serializar a mensagem de entrada.\"}]"),
                registro.path("erros"));
        assertEquals("Falha de serializacao.", registro.path("detalhe").asText());
        assertEquals("q.prevalidacao.monitoramento-mtr.in", registro.path("recurso").asText());
        assertEquals("adaptador", registro.path("camada").asText());
        assertEquals("MonitoramentoEntradaServiceBusMapper", registro.path("componente").asText());
        assertEquals("paraMensagem", registro.path("operacao").asText());
        assertEquals(EVENTO, registro.path("message").asText());
        assertFalse(registro.has("codigo_http"));
        assertFalse(registro.has("exception"));
        assertFalse(registro.has("traceId"));
        assertFalse(registro.has("spanId"));
        assertFalse(registro.has("linha_json"));
        assertFalse(registro.has("coluna_json"));
        assertTrue(registro.path("stacktrace").asText().startsWith(JsonGenerationException.class.getName()));
        assertFalse(registro.toString().contains(SEGREDO));
    }

    @Test
    void devePropagarIdentidadeDaOcorrenciaSemExporAExcecaoOriginal() throws IOException {
        var serializador = mock(ObjectMapper.class);
        var original = new JsonGenerationException(SEGREDO, (com.fasterxml.jackson.core.JsonGenerator) null);
        when(serializador.writeValueAsString(any())).thenThrow(original);
        var tentativa = tentativa();

        var falha = assertThrows(RuntimeException.class,
                () -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, serializador));

        var tipada = assertInstanceOf(SerializacaoEntradaException.class, falha);
        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        assertEquals("ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU", tipada.codigoErro());
        assertEquals(tipada.codigoErro(), registro.path("codigo_erro").asText());
        assertEquals(tipada.idErro(), registro.path("id_erro").asText());
        String idErro = tipada.idErro();
        assertDoesNotThrow(() -> java.util.UUID.fromString(idErro));
        assertEquals("Falha ao serializar a mensagem de entrada.", falha.getMessage());
        assertNull(falha.getCause());
        assertEquals(0, falha.getSuppressed().length);
        assertFalse(falha.toString().contains(SEGREDO));
        verify(serializador).writeValueAsString(any());
    }

    @Test
    void sucessoNaoDeveRegistrarErro() throws IOException {
        var tentativa = tentativa();

        assertDoesNotThrow(() -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, json));

        assertTrue(registros().isEmpty());
    }

    @Test
    void deveLocalizarFalhaRealSemExporMensagemCausaSuppressedOuProcessor() throws IOException {
        var serializador = serializadorComFalha();
        var tentativa = tentativa();

        var falha = assertThrows(SerializacaoEntradaException.class,
                () -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, serializador));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        assertEquals(falha.idErro(), registro.path("id_erro").asText());
        assertTrue(registro.path("stacktrace").asText().contains("MonitoramentoEntradaServiceBusMapper.paraMensagem"));
        for (String segredo : List.of(SEGREDO, "SEGREDO_CAUSA", "SEGREDO_SUPPRESSED", "SEGREDO_PROCESSOR")) {
            assertFalse(registro.toString().contains(segredo));
            assertFalse(falha.toString().contains(segredo));
        }
        assertFalse(registro.has("exception"));
        assertNull(falha.getCause());
        assertEquals(0, falha.getSuppressed().length);
        assertInstanceOf(RuntimeException.class, falha);
    }

    @Test
    void deveVincularSomenteOTraceValidoDaOcorrencia() throws IOException {
        var serializador = serializadorComFalha();
        var tentativa = tentativa();
        var raiz = io.opentelemetry.context.Context.root().makeCurrent();
        try {
            var span = io.opentelemetry.api.trace.Span.wrap(io.opentelemetry.api.trace.SpanContext.create(
                    "0123456789abcdef0123456789abcdef", "0123456789abcdef",
                    io.opentelemetry.api.trace.TraceFlags.getSampled(),
                    io.opentelemetry.api.trace.TraceState.getDefault()));
            var escopo = span.makeCurrent();
            try {
                assertThrows(SerializacaoEntradaException.class,
                        () -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, serializador));
            } finally {
                escopo.close();
            }
            assertThrows(SerializacaoEntradaException.class,
                    () -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, serializador));

            var registros = registros();
            assertEquals(2, registros.size());
            assertEquals("0123456789abcdef0123456789abcdef", registros.getFirst().path("traceId").asText());
            assertEquals("0123456789abcdef", registros.getFirst().path("spanId").asText());
            assertFalse(registros.getLast().has("traceId"));
            assertFalse(registros.getLast().has("spanId"));
            assertNotEquals(registros.getFirst().path("id_erro"), registros.getLast().path("id_erro"));
        } finally {
            raiz.close();
        }
    }

    @Test
    void naoDeveReclassificarErroInesperadoComoFalhaDeSerializacao() throws IOException {
        var serializador = mock(ObjectMapper.class);
        var inesperada = new IllegalStateException("Falha inesperada da fixture.");
        when(serializador.writeValueAsString(any())).thenThrow(inesperada);
        var tentativa = tentativa();

        var falha = assertThrows(IllegalStateException.class,
                () -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, serializador));

        assertSame(inesperada, falha);
        assertTrue(registros().isEmpty());
    }

    private ObjectMapper serializadorComFalha() {
        var modulo = new com.fasterxml.jackson.databind.module.SimpleModule();
        modulo.addSerializer(br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.dto.MonitorarDossieMtrV1.class,
                new com.fasterxml.jackson.databind.JsonSerializer<>() {
                    @Override
                    public void serialize(
                            br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.dto.MonitorarDossieMtrV1 valor,
                            com.fasterxml.jackson.core.JsonGenerator gerador,
                            com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
                        gerador.writeStartObject();
                        gerador.writeStringField("senha", "SEGREDO_PROCESSOR");
                        var causa = new JsonGenerationException(SEGREDO,
                                new IllegalStateException("SEGREDO_CAUSA"), gerador);
                        causa.addSuppressed(new IllegalArgumentException("SEGREDO_SUPPRESSED"));
                        throw causa;
                    }
                });
        return json.copy().registerModule(modulo);
    }

    private static TentativaMonitoramento tentativa() {
        return new TentativaMonitoramento("MON-1", "ORQ-1", "pre-externa", "0007", 1,
                Instant.parse("2026-09-04T12:00:00.123Z"), Instant.parse("2026-09-05T12:00:00.123Z"),
                "politica-v7");
    }

    private List<JsonNode> registros() throws IOException {
        var linhas = Files.readAllLines(ARQUIVO);
        var registros = new ArrayList<JsonNode>();
        for (String linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registro = json.readTree(linha);
            if (EVENTO.equals(registro.path("evento").asText())) {
                registros.add(registro);
            }
        }
        return registros;
    }
}
