package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto.MonitorarDossieMtrV1;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(MonitoramentoReagendamentoLogTest.LogJsonProfile.class)
class MonitoramentoReagendamentoLogTest {

    private static final String EVENTO = "monitoramento.servicebus.reagendamento.falhou";
    private static final Path ARQUIVO = Path.of("target/logs/monitoramento-reagendamento-erros-test.json");

    @Inject
    ObjectMapper json;

    @Inject
    Validator validator;

    @Inject
    MonitoramentoReagendamentoServiceBusMapper mapper;

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
    void deveEmitirUmErroJsonTipadoEPropagarIdentidadeRuntime() throws IOException {
        var borda = new MonitoramentoReagendamentoServiceBusMapper(serializadorComFalha(), validator, "fila-teste");
        var tentativa = ReagendamentoFixture.tentativa();

        var falha = assertThrows(MapeamentoReagendamentoException.class, () -> borda.paraMensagem(tentativa));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        var padrao = ((ObjectNode) registro).deepCopy().retain("level", "evento", "camada", "componente",
                "operacao", "recurso", "id_erro", "codigo_erro", "erros", "detalhe");
        assertEquals(json.readTree("""
                {"level":"ERROR","evento":"monitoramento.servicebus.reagendamento.falhou",
                 "camada":"adaptador","componente":"MonitoramentoReagendamentoServiceBusMapper",
                 "operacao":"paraMensagem","recurso":"fila-teste","id_erro":"%s",
                 "codigo_erro":"MONITORAMENTO_REAGENDAMENTO_SERIALIZACAO_FALHOU",
                 "erros":[{"mensagem":"Falha ao serializar a mensagem de reagendamento."}],
                 "detalhe":"Falha de serializacao."}
                """.formatted(falha.idErro())), padrao);
        assertEquals(falha.codigoErro(), registro.path("codigo_erro").asText());
        String idErro = falha.idErro();
        assertDoesNotThrow(() -> UUID.fromString(idErro));
        assertInstanceOf(RuntimeException.class, falha);
        assertEquals("Falha ao serializar a mensagem de reagendamento.", falha.getMessage());
        assertEquals(EVENTO, registro.path("message").asText());
        for (String campo : List.of("codigo_http", "exception", "traceId", "spanId", "linha_json", "coluna_json")) {
            assertFalse(registro.has(campo), campo);
        }
    }

    @Test
    void deveLocalizarAFalhaRealSemExporPayloadMensagemCausaOuProcessor() throws IOException {
        var borda = new MonitoramentoReagendamentoServiceBusMapper(serializadorComFalha(), validator, "fila-teste");
        var tentativa = ReagendamentoFixture.tentativa();

        var falha = assertThrows(MapeamentoReagendamentoException.class, () -> borda.paraMensagem(tentativa));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        var stack = registro.path("stacktrace").asText();
        assertTrue(stack.startsWith(JsonGenerationException.class.getName()));
        assertTrue(stack.contains("MonitoramentoReagendamentoServiceBusMapper.paraMensagem"));
        for (String segredo : List.of("SEGREDO_MENSAGEM", "SEGREDO_CAUSA", "SEGREDO_SUPPRESSED", "SEGREDO_PROCESSOR")) {
            assertFalse(registro.toString().contains(segredo));
            assertFalse(falha.toString().contains(segredo));
        }
        assertNull(falha.getCause());
        assertEquals(0, falha.getSuppressed().length);
        assertFalse(registro.has("exception"));
    }

    @Test
    void deveRegistrarRejeicaoSemValorRejeitadoStackOuTentativaDeSerializar() throws IOException {
        var serializador = mock(ObjectMapper.class);
        var borda = new MonitoramentoReagendamentoServiceBusMapper(serializador, validator, "fila-teste");
        var original = ReagendamentoFixture.tentativa();
        var invalida = new TentativaMonitoramento(original.monitoramentoId(), original.orquestracaoId(),
                original.idDossiePreValidacao(), "SEGREDO_CONTRATO", original.tentativaAtual(),
                original.iniciadoEm(), original.limiteEm(), original.politicaMonitoramentoVersao());

        var falha = assertThrows(MapeamentoReagendamentoException.class, () -> borda.paraMensagem(invalida));

        var registros = registros();
        assertEquals(1, registros.size());
        var registro = registros.getFirst();
        assertEquals("MONITORAMENTO_REAGENDAMENTO_CONTRATO_INVALIDO", falha.codigoErro());
        assertEquals(falha.codigoErro(), registro.path("codigo_erro").asText());
        assertEquals(falha.idErro(), registro.path("id_erro").asText());
        assertEquals(json.readTree("[{\"mensagem\":\"Contrato invalido na mensagem de reagendamento.\"}]"),
                registro.path("erros"));
        assertFalse(registro.has("stacktrace"));
        assertFalse(registro.has("exception"));
        assertFalse(registro.toString().contains("SEGREDO_CONTRATO"));
        assertNull(falha.getCause());
        verifyNoInteractions(serializador);
    }

    @Test
    void sucessoNaoDeveRegistrarErro() throws IOException {
        var tentativa = ReagendamentoFixture.tentativa();

        assertDoesNotThrow(() -> mapper.paraMensagem(tentativa));

        assertTrue(registros().isEmpty());
    }

    @Test
    void deveVincularSomenteOTraceValidoDaOcorrencia() throws IOException {
        var borda = new MonitoramentoReagendamentoServiceBusMapper(serializadorComFalha(), validator, "fila-teste");
        var tentativa = ReagendamentoFixture.tentativa();
        var raiz = io.opentelemetry.context.Context.root().makeCurrent();
        try {
            var span = io.opentelemetry.api.trace.Span.wrap(io.opentelemetry.api.trace.SpanContext.create(
                    "0123456789abcdef0123456789abcdef", "0123456789abcdef",
                    io.opentelemetry.api.trace.TraceFlags.getSampled(),
                    io.opentelemetry.api.trace.TraceState.getDefault()));
            var escopo = span.makeCurrent();
            try {
                assertThrows(MapeamentoReagendamentoException.class, () -> borda.paraMensagem(tentativa));
            } finally {
                escopo.close();
            }
            assertThrows(MapeamentoReagendamentoException.class, () -> borda.paraMensagem(tentativa));

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
        var borda = new MonitoramentoReagendamentoServiceBusMapper(serializador, validator, "fila-teste");
        var tentativa = ReagendamentoFixture.tentativa();

        var falha = assertThrows(IllegalStateException.class, () -> borda.paraMensagem(tentativa));

        assertSame(inesperada, falha);
        assertTrue(registros().isEmpty());
    }

    private ObjectMapper serializadorComFalha() {
        var modulo = new com.fasterxml.jackson.databind.module.SimpleModule();
        modulo.addSerializer(MonitorarDossieMtrV1.class, new com.fasterxml.jackson.databind.JsonSerializer<>() {
            @Override
            public void serialize(MonitorarDossieMtrV1 valor, com.fasterxml.jackson.core.JsonGenerator gerador,
                    com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
                gerador.writeStartObject();
                gerador.writeStringField("senha", "SEGREDO_PROCESSOR");
                var causa = new JsonGenerationException("SEGREDO_MENSAGEM",
                        new IllegalStateException("SEGREDO_CAUSA"), gerador);
                causa.addSuppressed(new IllegalArgumentException("SEGREDO_SUPPRESSED"));
                throw causa;
            }
        });
        return json.copy().registerModule(modulo);
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
