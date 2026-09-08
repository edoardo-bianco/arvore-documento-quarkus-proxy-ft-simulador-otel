package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(MonitoramentoEntradaLogTest.LogJsonProfile.class)
class MonitoramentoEntradaLogTest {

    private static final String EVENTO = "monitoramento.servicebus.entrada.falhou";
    private static final Path ARQUIVO = Path.of("target/logs/monitoramento-entrada-erros-test.json");
    private static final String CORPO_VALIDO = """
            {"schemaVersion":1,"monitoramentoId":"MON-1","orquestracaoId":"ORQ-1",
             "idDossiePreValidacao":"pre-externa","idDossieMtr":"007","tentativaAtual":3,
             "iniciadoEm":"2026-09-04T12:00:00.123Z","limiteEm":"2026-09-05T12:00:00.123Z",
             "politicaMonitoramentoVersao":"politica-v7"}
            """;

    @Inject
    MonitoramentoEntradaServiceBusMapper mapper;
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
    void deveRegistrarErroDeParsingUmaVezComJsonPadraoSemExporPayload() throws IOException {
        String corpo = "{\"senha\":\"nao-expor-segredo\",\n";
        var falha = assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(corpo));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        assertEquals("ERROR", registro.path("level").asText());
        assertEquals("MONITORAMENTO_ENTRADA_JSON_INVALIDO", registro.path("codigo_erro").asText());
        assertTrue(registro.path("erros").isArray());
        assertTrue(registro.path("erros").get(0).path("mensagem").isTextual());
        assertFalse(registro.has("codigo_http"));
        assertFalse(registro.toString().contains("nao-expor-segredo"));
        assertEquals("Contrato de monitoramento invalido.", falha.getMessage());
        assertNull(falha.getCause());
        assertEquals(falha.idErro(), registro.path("id_erro").asText());
        assertEquals(falha.codigoErro(), registro.path("codigo_erro").asText());
        String idErro = falha.idErro();
        assertDoesNotThrow(() -> java.util.UUID.fromString(idErro));
        assertEquals("q.prevalidacao.monitoramento-mtr.in", registro.path("recurso").asText());
        assertEquals("MonitoramentoEntradaServiceBusMapper", registro.path("componente").asText());
        assertEquals("lerContrato", registro.path("operacao").asText());
        assertTrue(registro.path("linha_json").isIntegralNumber());
        assertTrue(registro.path("coluna_json").isIntegralNumber());
        assertEquals(2, registro.path("linha_json").asInt());
        assertEquals(1, registro.path("coluna_json").asInt());
        assertTrue(registro.path("stacktrace").asText()
                .contains("MonitoramentoEntradaServiceBusMapper.lerContrato"));
        assertFalse(registro.has("traceId"));
        assertFalse(registro.has("spanId"));
    }

    @Test
    void sucessoNaoDeveRegistrarErro() throws IOException {
        assertDoesNotThrow(() -> ler(CORPO_VALIDO));
        assertTrue(registros().isEmpty());
    }


    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"{}", "[]", "null", " ", ""})
    void deveRegistrarUmaFalhaDeEstruturaSemStackTecnico(String corpo) throws IOException {
        var falha = assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(corpo));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        assertEquals("MONITORAMENTO_ENTRADA_CONTRATO_INVALIDO", falha.codigoErro());
        assertEquals(falha.codigoErro(), registro.path("codigo_erro").asText());
        assertEquals(falha.idErro(), registro.path("id_erro").asText());
        assertFalse(registro.has("stacktrace"));
        assertFalse(registro.has("linha_json"));
        assertFalse(registro.has("coluna_json"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"schemaVersion", "tentativaAtual", "limiteEm", "monitoramentoId"})
    void deveRegistrarViolacaoDoContratoSemExporValor(String campo) throws IOException {
        var corpo = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(CORPO_VALIDO);
        switch (campo) {
            case "schemaVersion" -> corpo.put(campo, 2);
            case "tentativaAtual" -> corpo.put(campo, 0);
            case "limiteEm" -> corpo.put(campo, "2026-09-04T00:00:00Z");
            case "monitoramentoId" -> corpo.put(campo, "");
            default -> throw new AssertionError("Campo da fixture desconhecido.");
        }
        var mensagem = corpo.toString();

        var falha = assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(mensagem));

        assertEquals("MONITORAMENTO_ENTRADA_CONTRATO_INVALIDO", falha.codigoErro());
        assertEquals(1, registros().size());
        assertEquals("paraTentativa", registros().getFirst().path("operacao").asText());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void deveClassificarEnvelopeInvalidoSemRegistrarPropriedadeRejeitada(int indice) throws IOException {
        var propriedades = new String[]{"MON-1:tentativa:3", "ORQ-1", "MONITORAR_DOSSIE_MTR", "application/json"};
        propriedades[indice] = "segredo-envelope-rejeitado";

        var falha = assertThrows(ContratoMonitoramentoInvalidoException.class,
                () -> mapper.paraTentativa(CORPO_VALIDO, propriedades[0], propriedades[1],
                        propriedades[2], propriedades[3]));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        assertEquals("MONITORAMENTO_ENTRADA_ENVELOPE_INVALIDO", falha.codigoErro());
        assertEquals(falha.idErro(), registro.path("id_erro").asText());
        assertFalse(registro.toString().contains("segredo-envelope-rejeitado"));
        assertFalse(registro.has("stacktrace"));
    }

    @Test
    void deveOmitirMensagemOriginalQueContemTokenRejeitado() throws IOException {
        String segredo = "SEGREDO_PARSER_SENTINELA";
        var original = assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                () -> json.readTree(segredo));
        assertTrue(original.getOriginalMessage().contains(segredo));

        assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(segredo));

        var registro = registros().getFirst();
        assertFalse(registro.toString().contains(segredo));
        assertFalse(registro.has("exception"));
        assertTrue(registro.path("stacktrace").asText().contains("JsonParseException"));
    }

    @Test
    void devePreservarTraceValidoEContextoSemContaminarProximaOcorrencia() throws IOException {
        var contextoOriginal = org.jboss.logging.MDC.get("contextoTeste");
        var raiz = io.opentelemetry.context.Context.root().makeCurrent();
        org.jboss.logging.MDC.put("contextoTeste", "preservado");
        try {
            var span = io.opentelemetry.api.trace.Span.wrap(io.opentelemetry.api.trace.SpanContext.create(
                    "0123456789abcdef0123456789abcdef", "0123456789abcdef",
                    io.opentelemetry.api.trace.TraceFlags.getSampled(),
                    io.opentelemetry.api.trace.TraceState.getDefault()));
            var escopo = span.makeCurrent();
            try {
                assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler("{"));
            } finally {
                escopo.close();
            }
            assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler("{"));

            var registros = registros();
            assertEquals(2, registros.size());
            assertEquals("0123456789abcdef0123456789abcdef", registros.getFirst().path("traceId").asText());
            assertEquals("0123456789abcdef", registros.getFirst().path("spanId").asText());
            assertFalse(registros.getLast().has("traceId"));
            assertFalse(registros.getLast().has("spanId"));
            assertNotEquals(registros.getFirst().path("id_erro"), registros.getLast().path("id_erro"));
            assertEquals("preservado", org.jboss.logging.MDC.get("contextoTeste"));
        } finally {
            raiz.close();
            if (contextoOriginal == null) {
                org.jboss.logging.MDC.remove("contextoTeste");
            } else {
                org.jboss.logging.MDC.put("contextoTeste", contextoOriginal);
            }
        }
    }

    private void ler(String corpo) {
        mapper.paraTentativa(corpo, "MON-1:tentativa:3", "ORQ-1",
                "MONITORAR_DOSSIE_MTR", "application/json");
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
