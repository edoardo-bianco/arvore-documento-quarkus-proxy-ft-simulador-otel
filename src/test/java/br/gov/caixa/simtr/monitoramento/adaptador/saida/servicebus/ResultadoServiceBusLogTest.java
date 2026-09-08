package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto.ResultadoMonitoramentoDossieMtrV1;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(ResultadoServiceBusLogTest.LogJsonProfile.class)
class ResultadoServiceBusLogTest {

    private static final Path ARQUIVO = Path.of("target/logs/resultado-erros-test.json");
    private static final String EVENTO_PRODUTOR = "monitoramento.servicebus.resultado.falhou";
    private static final String EVENTO_CONSUMIDOR = "orquestrador.servicebus.resultado.falhou";

    @Inject
    ObjectMapper json;
    @Inject
    Validator validator;
    @Inject
    MonitoramentoResultadoServiceBusMapper produtor;
    @Inject
    br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoServiceBusMapper consumidor;

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
    void produtorDeveRegistrarFormatoTipadoEPropagarIdentidade() throws IOException {
        var borda = new MonitoramentoResultadoServiceBusMapper(serializadorComFalha(), validator, "fila-saida-teste");
        var resultado = ResultadoFixture.resultado();

        var falha = assertThrows(MapeamentoResultadoException.class, () -> borda.paraMensagem(resultado));

        var registro = unicoRegistro();
        var campos = ((ObjectNode) registro).deepCopy().retain("level", "evento", "camada", "componente",
                "operacao", "recurso", "id_erro", "codigo_erro", "erros", "detalhe");
        assertEquals(json.readTree("""
                {"level":"ERROR","evento":"monitoramento.servicebus.resultado.falhou","camada":"adaptador",
                 "componente":"MonitoramentoResultadoServiceBusMapper","operacao":"paraMensagem",
                 "recurso":"fila-saida-teste","id_erro":"%s","codigo_erro":"MONITORAMENTO_RESULTADO_SERIALIZACAO_FALHOU",
                 "erros":[{"mensagem":"Falha ao serializar a mensagem de resultado."}],
                 "detalhe":"Falha de serializacao."}
                """.formatted(falha.idErro())), campos);
        assertEquals(falha.codigoErro(), registro.path("codigo_erro").asText());
        String id = falha.idErro();
        assertDoesNotThrow(() -> UUID.fromString(id));
        assertInstanceOf(RuntimeException.class, falha);
        assertEquals(EVENTO_PRODUTOR, registro.path("message").asText());
        for (String campo : List.of("codigo_http", "exception", "traceId", "spanId", "linha_json", "coluna_json")) {
            assertFalse(registro.has(campo), campo);
        }
    }

    @Test
    void serializacaoDeveLocalizarAFalhaSemExporMensagemCausaSuppressedOuProcessor() throws IOException {
        var borda = new MonitoramentoResultadoServiceBusMapper(serializadorComFalha(), validator, "fila-saida-teste");
        var resultado = ResultadoFixture.resultado();

        var falha = assertThrows(MapeamentoResultadoException.class, () -> borda.paraMensagem(resultado));

        var registro = unicoRegistro();
        assertTrue(registro.path("stacktrace").asText().startsWith(JsonGenerationException.class.getName()));
        assertTrue(registro.path("stacktrace").asText().contains("MonitoramentoResultadoServiceBusMapper.paraMensagem"));
        for (String segredo : List.of("SEGREDO_MENSAGEM", "SEGREDO_CAUSA", "SEGREDO_SUPPRESSED", "SEGREDO_PROCESSOR")) {
            assertFalse(registro.toString().contains(segredo));
            assertFalse(falha.toString().contains(segredo));
        }
        assertNull(falha.getCause());
        assertEquals(0, falha.getSuppressed().length);
    }

    @Test
    void consumidorDeveRegistrarParsingComLocalizacaoNumericaSemExporConteudo() throws IOException {
        String corpo = "{\"senha\":\"SEGREDO_PAYLOAD\",\n";
        var falha = assertThrows(
                br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                () -> ler(corpo));

        var registro = unicoRegistro();
        assertEquals(EVENTO_CONSUMIDOR, registro.path("evento").asText());
        assertEquals("ORQUESTRADOR_RESULTADO_JSON_INVALIDO", falha.codigoErro());
        assertEquals(falha.codigoErro(), registro.path("codigo_erro").asText());
        assertEquals(falha.idErro(), registro.path("id_erro").asText());
        assertTrue(registro.path("erros").isArray());
        assertEquals(2, registro.path("linha_json").asInt());
        assertEquals(1, registro.path("coluna_json").asInt());
        assertTrue(registro.path("linha_json").isIntegralNumber());
        assertTrue(registro.path("coluna_json").isIntegralNumber());
        assertTrue(registro.path("stacktrace").asText().contains("MonitoramentoResultadoServiceBusMapper.lerContrato"));
        assertFalse(registro.toString().contains("SEGREDO_PAYLOAD"));
        assertFalse(registro.has("exception"));
        assertFalse(registro.has("codigo_http"));
        assertNull(falha.getCause());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejeicaoDeContratoNaoDeveExporValorOuStack(boolean produtorAtivo) throws IOException {
        if (produtorAtivo) {
            var serializador = mock(ObjectMapper.class);
            var borda = new MonitoramentoResultadoServiceBusMapper(serializador, validator, "fila-saida-teste");
            assertThrows(MapeamentoResultadoException.class, () -> borda.paraMensagem(null));
            verifyNoInteractions(serializador);
        } else {
            String corpo = ResultadoFixture.JSON.replace("0009223372036854775807", "SEGREDO_CONTRATO");
            assertThrows(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                    () -> ler(corpo));
        }

        var registro = unicoRegistro();
        assertTrue(registro.path("codigo_erro").asText().endsWith("_CONTRATO_INVALIDO"));
        assertEquals(json.readTree("[{\"mensagem\":\"Contrato invalido na mensagem de resultado.\"}]"),
                registro.path("erros"));
        assertFalse(registro.has("stacktrace"));
        assertFalse(registro.has("exception"));
        assertFalse(registro.toString().contains("SEGREDO_CONTRATO"));
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void conclusivoSemConsultaDeveRegistrarRejeicaoTipada(boolean produtorAtivo) throws IOException {
        String corpo = ResultadoFixture.JSON.replace("\"tentativasRealizadas\":3", "\"tentativasRealizadas\":0");
        String idErro;
        if (produtorAtivo) {
            var resultado = json.readValue(corpo,
                    br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento.class);
            var falha = assertThrows(MapeamentoResultadoException.class, () -> produtor.paraMensagem(resultado));
            idErro = falha.idErro();
        } else {
            var falha = assertThrows(
                    br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                    () -> ler(corpo));
            idErro = falha.idErro();
        }

        var registro = unicoRegistro();
        assertEquals(idErro, registro.path("id_erro").asText());
        assertEquals(produtorAtivo ? "MONITORAMENTO_RESULTADO_CONTRATO_INVALIDO"
                : "ORQUESTRADOR_RESULTADO_CONTRATO_INVALIDO", registro.path("codigo_erro").asText());
        assertEquals(json.readTree("[{\"mensagem\":\"Contrato invalido na mensagem de resultado.\"}]"),
                registro.path("erros"));
        assertFalse(registro.has("stacktrace"));
        assertFalse(registro.has("exception"));
    }

    @Test
    void consumidorDeveRegistrarEnvelopeInvalidoUmaVez() throws IOException {
        var falha = assertThrows(
                br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                () -> consumidor.paraResultado(ResultadoFixture.JSON, "outro", "ORQ-1",
                        "RESULTADO_MONITORAMENTO_DOSSIE_MTR", "application/json"));

        var registro = unicoRegistro();
        assertEquals("ORQUESTRADOR_RESULTADO_ENVELOPE_INVALIDO", falha.codigoErro());
        assertEquals(falha.idErro(), registro.path("id_erro").asText());
        assertEquals(falha.codigoErro(), registro.path("codigo_erro").asText());
        assertFalse(registro.has("stacktrace"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deveVincularSomenteOTraceValidoSemAcumularContexto(boolean produtorAtivo) throws IOException {
        var raiz = io.opentelemetry.context.Context.root().makeCurrent();
        try {
            var span = io.opentelemetry.api.trace.Span.wrap(io.opentelemetry.api.trace.SpanContext.create(
                    "0123456789abcdef0123456789abcdef", "0123456789abcdef",
                    io.opentelemetry.api.trace.TraceFlags.getSampled(),
                    io.opentelemetry.api.trace.TraceState.getDefault()));
            var escopo = span.makeCurrent();
            try {
                provocarFalha(produtorAtivo);
            } finally {
                escopo.close();
            }
            provocarFalha(produtorAtivo);

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sucessoNasDuasBordasNaoDeveEmitirErro(boolean quarentena) throws IOException {
        var resultado = quarentena ? ResultadoFixture.quarentena(0) : ResultadoFixture.resultado();
        var mensagem = produtor.paraMensagem(resultado);

        assertDoesNotThrow(() -> ler(mensagem.getBody().toString()));

        assertTrue(registros().isEmpty());
    }

    @Test
    void produtorNaoDeveReclassificarErroInesperado() throws IOException {
        var serializador = mock(ObjectMapper.class);
        var inesperada = new IllegalStateException("Falha inesperada da fixture.");
        when(serializador.writeValueAsString(any())).thenThrow(inesperada);
        var borda = new MonitoramentoResultadoServiceBusMapper(serializador, validator, "fila-saida-teste");
        var resultado = ResultadoFixture.resultado();

        var falha = assertThrows(IllegalStateException.class, () -> borda.paraMensagem(resultado));

        assertSame(inesperada, falha);
        assertTrue(registros().isEmpty());
    }

    @Test
    void consumidorNaoDeveReclassificarErroInesperado() throws IOException {
        var desserializador = spy(json.copy());
        var inesperada = new IllegalStateException("Falha inesperada da fixture.");
        doThrow(inesperada).when(desserializador).treeToValue(any(JsonNode.class),
                eq(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.dto.ResultadoMonitoramentoDossieMtrV1.class));
        var borda = new br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoServiceBusMapper(
                desserializador, validator, "fila-saida-teste");

        var falha = assertThrows(IllegalStateException.class,
                () -> borda.paraResultado(ResultadoFixture.JSON, "MON-1:resultado:v1", "ORQ-1",
                        "RESULTADO_MONITORAMENTO_DOSSIE_MTR", "application/json"));

        assertSame(inesperada, falha);
        assertTrue(registros().isEmpty());
    }

    private void provocarFalha(boolean produtorAtivo) {
        if (produtorAtivo) {
            assertThrows(MapeamentoResultadoException.class, () -> produtor.paraMensagem(null));
        } else {
            assertThrows(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                    () -> ler(null));
        }
    }

    private ObjectMapper serializadorComFalha() {
        var modulo = new com.fasterxml.jackson.databind.module.SimpleModule();
        modulo.addSerializer(ResultadoMonitoramentoDossieMtrV1.class,
                new com.fasterxml.jackson.databind.JsonSerializer<>() {
                    @Override
                    public void serialize(ResultadoMonitoramentoDossieMtrV1 valor,
                            com.fasterxml.jackson.core.JsonGenerator gerador,
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

    private br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento ler(String corpo) {
        return consumidor.paraResultado(corpo, "MON-1:resultado:v1", "ORQ-1",
                "RESULTADO_MONITORAMENTO_DOSSIE_MTR", "application/json");
    }

    private JsonNode unicoRegistro() throws IOException {
        var encontrados = registros();
        assertEquals(1, encontrados.size());
        return encontrados.getFirst();
    }

    private List<JsonNode> registros() throws IOException {
        var linhas = Files.readAllLines(ARQUIVO);
        var registros = new ArrayList<JsonNode>();
        for (String linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registro = json.readTree(linha);
            if (List.of(EVENTO_PRODUTOR, EVENTO_CONSUMIDOR).contains(registro.path("evento").asText())) {
                registros.add(registro);
            }
        }
        return registros;
    }
}
