package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ConsultaDossieProdutoMtrContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long IDENTIFICADOR = 4_324_680L;
    private static final String CAMINHO_PUBLICO = "/simtr-hub/v1/dossie-produto/{id}";
    private static final String CAMINHO_MTR =
            DossieProdutoMtrStubTestResource.CAMINHO_DOCUMENTO + "/" + IDENTIFICADOR;
    private static final String HTTP_GET = "GET";
    private static final String CORPO_VAZIO = "";
    private static final String MEDIA_TYPE_JSON = "application/json";
    private static final String API_KEY = "test-apikey";
    private static final String TOKEN = "stub-access-token";
    private static final String AUTHORIZATION = "Bearer " + TOKEN;
    private static final String TRACEPARENT_PATTERN =
            "00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}";
    private static final String CONFIG_SIMULADOR =
            "simtr-hub.simulador.dossie-produto.habilitado";
    private static final String SPAN_API = "simtr-hub.api.dossie-produto.consultar";
    private static final String SPAN_APLICACAO =
            "simtr-hub.service.dossie-produto.consultar";
    private static final String SPAN_CLIENT = "mtr.dossie-produto.consultar";
    private static final String API_DOSSIE_PRODUTO_V2 = "dossie-produto-v2";
    private static final String ORIGEM_MTR = "mtr";
    private static final String RESULTADO_SUCESSO = "sucesso";
    private static final String RESULTADO_ERRO = "erro";
    private static final String ATRIBUTO_DOSSIE_ID = "dossie_produto.id";
    private static final String ATRIBUTO_CLIENTES_QUANTIDADE =
            "dossie_produto.clientes.quantidade";
    private static final String ATRIBUTO_UNIDADES_QUANTIDADE =
            "dossie_produto.unidades_tratamento.quantidade";
    private static final String ATRIBUTO_PRODUTOS_QUANTIDADE =
            "dossie_produto.produtos_contratados.quantidade";
    private static final String CAMPO_DOSSIE_ID = "dossie_produto_id";
    private static final String CAMPO_RESULTADO = "resultado";
    private static final String CAMPO_ERRO_TIPO = "erro_tipo";
    private static final String CPF_SENTINELA = "CPF_SENTINELA_00000000000";
    private static final String CNPJ_SENTINELA = "CNPJ_SENTINELA_00000000000000";
    private static final String NOME_SENTINELA = "NOME_SENTINELA_CLIENTE";
    private static final String MATRICULA_SENTINELA = "MATRICULA_SENTINELA";
    private static final String PAYLOAD_SENTINELA = "PAYLOAD_SENTINELA_NAO_REGISTRAR";
    private static final String ERRO_SENTINELA = "ERRO_SENTINELA_NAO_REGISTRAR";
    private static final String PREFIXO_EVENTO_API = "simtr-hub.dossie-produto.consulta.";
    private static final String PREFIXO_EVENTO_MTR = "mtr.dossie-produto.consulta.";
    private static final String EVENTO_API_RECEBIDA =
            PREFIXO_EVENTO_API + "requisicao.recebida";
    private static final String EVENTO_SERVICE_INICIADA =
            PREFIXO_EVENTO_API + "service.iniciada";
    private static final String EVENTO_MTR_INICIADA =
            PREFIXO_EVENTO_MTR + "chamada.iniciada";
    private static final String EVENTO_MTR_CONCLUIDA =
            PREFIXO_EVENTO_MTR + "chamada.concluida";
    private static final String EVENTO_SERVICE_CONCLUIDA =
            PREFIXO_EVENTO_API + "service.concluida";
    private static final String EVENTO_API_RESPOSTA =
            PREFIXO_EVENTO_API + "resposta.enviada";
    private static final String EVENTO_MTR_FALHOU =
            PREFIXO_EVENTO_MTR + "chamada.falhou";
    private static final String EVENTO_SERVICE_FALHOU =
            PREFIXO_EVENTO_API + "service.falhou";
    private static final String EVENTO_API_FALHOU =
            PREFIXO_EVENTO_API + "requisicao.falhou";
    private static final String RESPOSTA_MTR = """
            {
              "id": 4324680,
              "chave_correlacao_canal": 1000012592,
              "instancia_jbpm": null,
              "numero_negocio": null,
              "canal_criacao": "PAYLOAD_SENTINELA_NAO_REGISTRAR",
              "unidade_criacao": 5402,
              "data_criacao": null,
              "clientes": [{
                "cpf": "CPF_SENTINELA_00000000000",
                "cnpj": "CNPJ_SENTINELA_00000000000000",
                "nome": "NOME_SENTINELA_CLIENTE",
                "razao_social": null,
                "tipo_vinculo": "Proponente",
                "identificador_negocial_vinculo": 40610702,
                "principal": true
              }],
              "processo": {
                "id": 5032,
                "nome": "Concessão Habitacional",
                "identificador_negocial": 1000016487,
                "macroprocesso": "HABITAÇÃO",
                "tratamento_seletivo": true,
                "complementacao_seletiva": true
              },
              "fase_atual": {
                "id": 5033,
                "nome": "Recepção de dados e documentos",
                "identificador_negocial": 1000016488,
                "data": "23/07/2026 10:24:00"
              },
              "situacao_atual": {
                "id": 1,
                "nome": "Rascunho",
                "data": "23/07/2026 10:24:00",
                "matricula": "MATRICULA_SENTINELA"
              },
              "unidades_tratamento": [],
              "produtos_contratados": [{
                "id": null,
                "codigo_operacao": null,
                "codigo_modalidade": null,
                "nome": null
              }]
            }
            """;
    private static final String ERRO_MTR_404 = """
            {
              "codigo_http": 404,
              "recurso": "simtr-dossie-produto",
              "id_erro": "stub-consulta-404",
              "codigo_erro": "MTR-DOSSIE-CONSULTA-404",
              "erros": [{"mensagem": "ERRO_SENTINELA_NAO_REGISTRAR"}],
              "detalhe": "falha de negocio controlada"
            }
            """;

    @ConfigProperty(name = CONFIG_SIMULADOR)
    boolean simuladorHabilitado;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;

    @BeforeEach
    void resetarStub() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        exporter.reset();
        DossieProdutoMtrStubTestResource.reset();
        rootLogger = Logger.getLogger("");
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
    }

    @AfterEach
    void removerCapturaDeLogs() {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    void percorreApiPublicaAteStubMtrPreservandoWireHeadersERespostaCompleta()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        JsonNode resposta = getDossie().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertFalse(simuladorHabilitado);
        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_MTR), resposta);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requisicoes =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());
        assertWireConsulta(requisicoes.getFirst());
    }

    @Test
    void preservaErro404CompletoSemRetry() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(404, ERRO_MTR_404);

        JsonNode resposta = getDossie().then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(ERRO_MTR_404), resposta);
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
        assertEventosFalhaMtr();
        assertSemDadosSensiveis(List.of());
    }

    @Test
    void retryRecuperaErro500RepetindoGetIdentico() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(
                500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-dossie-produto\"}"
        );
        DossieProdutoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        JsonNode resposta = getDossie().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_MTR), resposta);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requisicoes =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requisicoes.size());
        requisicoes.forEach(ConsultaDossieProdutoMtrContractTest::assertWireConsulta);
    }

    @Test
    void propagaContextoEntreApiAplicacaoEClientSemDadosSensiveis() {
        DossieProdutoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        getDossie().then().statusCode(200);
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);

        SpanData api = span(SPAN_API);
        SpanData aplicacao = span(SPAN_APLICACAO);
        SpanData client = span(SPAN_CLIENT);
        assertParentage(api, aplicacao, client);
        assertSpanApi(api);
        assertSpanAplicacao(aplicacao);
        assertSpanClient(client);
        assertEventosSucessoMtr();
        assertSemDadosSensiveis(List.of(api, aplicacao, client));
    }

    private static Response getDossie() {
        return given()
                .accept(ContentType.JSON)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR);
    }

    private static void assertWireConsulta(
            DossieProdutoMtrStubTestResource.CapturedRequest requisicao
    ) {
        assertEquals(HTTP_GET, requisicao.method());
        assertEquals(CAMINHO_MTR, requisicao.path());
        assertEquals(CORPO_VAZIO, requisicao.body());
        assertNull(requisicao.contentType());
        assertTrue(requisicao.accept().contains(MEDIA_TYPE_JSON));
        assertEquals(API_KEY, requisicao.apikey());
        assertEquals(AUTHORIZATION, requisicao.authorization());
        assertNotNull(requisicao.traceparent());
        assertTrue(requisicao.traceparent().matches(TRACEPARENT_PATTERN));
    }

    private SpanData span(String nome) {
        return exporter.getFinishedSpanItems().stream()
                .filter(item -> nome.equals(item.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertParentage(SpanData api, SpanData aplicacao, SpanData client) {
        assertEquals(SpanKind.SERVER, api.getKind());
        assertEquals(SpanKind.INTERNAL, aplicacao.getKind());
        assertEquals(SpanKind.CLIENT, client.getKind());
        assertEquals(api.getTraceId(), aplicacao.getTraceId());
        assertEquals(api.getTraceId(), client.getTraceId());
        assertEquals(api.getSpanId(), aplicacao.getParentSpanId());
        assertEquals(aplicacao.getSpanId(), client.getParentSpanId());
    }

    private static void assertSpanApi(SpanData api) {
        assertEquals(CAMINHO_PUBLICO, atributo(api, "http.route"));
        assertEquals(API_DOSSIE_PRODUTO_V2, atributo(api, "simtr_hub.api"));
        assertEquals(IDENTIFICADOR, atributo(api, ATRIBUTO_DOSSIE_ID));
        assertEquals(1L, atributo(api, ATRIBUTO_CLIENTES_QUANTIDADE));
        assertEquals(0L, atributo(api, ATRIBUTO_UNIDADES_QUANTIDADE));
        assertEquals(1L, atributo(api, ATRIBUTO_PRODUTOS_QUANTIDADE));
    }

    private static void assertSpanAplicacao(SpanData aplicacao) {
        assertEquals(false,
                atributo(aplicacao, "simtr_hub.simulador_dossie_produto_habilitado"));
        assertEquals(ORIGEM_MTR, atributo(aplicacao, "simtr_hub.origem_dados"));
        assertEquals(IDENTIFICADOR, atributo(aplicacao, ATRIBUTO_DOSSIE_ID));
        assertEquals(1L, atributo(aplicacao, ATRIBUTO_CLIENTES_QUANTIDADE));
        assertEquals(0L, atributo(aplicacao, ATRIBUTO_UNIDADES_QUANTIDADE));
        assertEquals(1L, atributo(aplicacao, ATRIBUTO_PRODUTOS_QUANTIDADE));
    }

    private static void assertSpanClient(SpanData client) {
        assertEquals("simtr-dossie-produto", atributo(client, "mtr.servico"));
        assertEquals(API_DOSSIE_PRODUTO_V2, atributo(client, "mtr.api"));
        assertEquals(HTTP_GET, atributo(client, "http.request.method"));
        assertEquals("/simtr/dossie-produto/v2/dossie-produto/{id}",
                atributo(client, "url.path"));
        assertEquals(IDENTIFICADOR, atributo(client, ATRIBUTO_DOSSIE_ID));
        assertEquals(1L, atributo(client, ATRIBUTO_CLIENTES_QUANTIDADE));
        assertEquals(0L, atributo(client, ATRIBUTO_UNIDADES_QUANTIDADE));
        assertEquals(1L, atributo(client, ATRIBUTO_PRODUTOS_QUANTIDADE));
    }

    private void assertSemDadosSensiveis(List<SpanData> spans) {
        String sinais = spans.stream()
                .map(span -> span.getAttributes() + span.getEvents().toString())
                .collect(Collectors.joining(" ")) + handler.logs();
        assertFalse(sinais.contains(CPF_SENTINELA));
        assertFalse(sinais.contains(CNPJ_SENTINELA));
        assertFalse(sinais.contains(NOME_SENTINELA));
        assertFalse(sinais.contains(MATRICULA_SENTINELA));
        assertFalse(sinais.contains(PAYLOAD_SENTINELA));
        assertFalse(sinais.contains(ERRO_SENTINELA));
        assertFalse(sinais.contains(API_KEY));
        assertFalse(sinais.contains(TOKEN));
    }

    private void assertEventosSucessoMtr() {
        Map<String, LogObservado> eventos = eventosConsulta();
        assertEquals(Set.of(
                EVENTO_API_RECEBIDA,
                EVENTO_SERVICE_INICIADA,
                EVENTO_MTR_INICIADA,
                EVENTO_MTR_CONCLUIDA,
                EVENTO_SERVICE_CONCLUIDA,
                EVENTO_API_RESPOSTA
        ), eventos.keySet());
        assertCamposComuns(eventos);
        assertOrigemMtr(eventos.get(EVENTO_SERVICE_INICIADA));
        assertOrigemMtr(eventos.get(EVENTO_SERVICE_CONCLUIDA));
        assertSucessoEContagens(eventos.get(EVENTO_MTR_CONCLUIDA));
        assertSucessoEContagens(eventos.get(EVENTO_SERVICE_CONCLUIDA));
        assertSucessoEContagens(eventos.get(EVENTO_API_RESPOSTA));
    }

    private void assertEventosFalhaMtr() {
        Map<String, LogObservado> eventos = eventosConsulta();
        assertEquals(Set.of(
                EVENTO_API_RECEBIDA,
                EVENTO_SERVICE_INICIADA,
                EVENTO_MTR_INICIADA,
                EVENTO_MTR_FALHOU,
                EVENTO_SERVICE_FALHOU,
                EVENTO_API_FALHOU
        ), eventos.keySet());
        assertCamposComuns(eventos);
        assertOrigemMtr(eventos.get(EVENTO_SERVICE_INICIADA));
        LogObservado serviceFalhou = eventos.get(EVENTO_SERVICE_FALHOU);
        assertOrigemMtr(serviceFalhou);
        assertEquals(RESULTADO_ERRO,
                eventos.get(EVENTO_MTR_FALHOU).mdc().get(CAMPO_RESULTADO));
        assertEquals("Negocio", eventos.get(EVENTO_MTR_FALHOU).mdc().get(CAMPO_ERRO_TIPO));
        assertEquals(RESULTADO_ERRO, serviceFalhou.mdc().get(CAMPO_RESULTADO));
        assertEquals("FalhaConsultaDossieProduto",
                serviceFalhou.mdc().get(CAMPO_ERRO_TIPO));
        assertEquals(RESULTADO_ERRO,
                eventos.get(EVENTO_API_FALHOU).mdc().get(CAMPO_RESULTADO));
        assertEquals("MtrBusinessErrorException",
                eventos.get(EVENTO_API_FALHOU).mdc().get(CAMPO_ERRO_TIPO));
    }

    private Map<String, LogObservado> eventosConsulta() {
        return handler.logs().stream()
                .filter(log -> log.evento().startsWith(PREFIXO_EVENTO_API)
                        || log.evento().startsWith(PREFIXO_EVENTO_MTR))
                .collect(Collectors.toMap(LogObservado::evento, log -> log));
    }

    private static void assertCamposComuns(Map<String, LogObservado> eventos) {
        eventos.forEach((evento, log) -> assertEquals(
                String.valueOf(IDENTIFICADOR),
                log.mdc().get(CAMPO_DOSSIE_ID),
                evento
        ));
    }

    private static void assertOrigemMtr(LogObservado log) {
        assertEquals(ORIGEM_MTR, log.mdc().get("origem"));
        assertEquals("false", log.mdc().get("simulador_habilitado"));
    }

    private static void assertSucessoEContagens(LogObservado log) {
        assertEquals(RESULTADO_SUCESSO, log.mdc().get(CAMPO_RESULTADO));
        assertEquals("1", log.mdc().get("clientes_quantidade"));
        assertEquals("0", log.mdc().get("unidades_tratamento_quantidade"));
        assertEquals("1", log.mdc().get("produtos_contratados_quantidade"));
    }

    private static Object atributo(SpanData span, String nome) {
        return span.getAttributes().asMap().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(nome))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord instanceof ExtLogRecord extLogRecord
                    && logRecord.getMessage() != null) {
                logs.add(new LogObservado(
                        logRecord.getMessage(), extLogRecord.getMdcCopy()
                ));
            }
        }

        @Override
        public void flush() {
            // No-op: não há buffer no handler de teste.
        }

        @Override
        public void close() {
            logs.clear();
        }

        List<LogObservado> logs() {
            return new ArrayList<>(logs);
        }
    }

    private record LogObservado(String evento, Map<String, String> mdc) {
    }
}
