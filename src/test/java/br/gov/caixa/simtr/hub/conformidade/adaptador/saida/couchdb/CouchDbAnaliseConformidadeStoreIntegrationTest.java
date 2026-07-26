package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.contrato.ArmazenarEstadoAnaliseConformidadeContractTest;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@QuarkusTest
class CouchDbAnaliseConformidadeStoreIntegrationTest
        extends ArmazenarEstadoAnaliseConformidadeContractTest {

    private static final DockerImageName COUCHDB_IMAGE =
            DockerImageName.parse("couchdb:3.5.2");
    private static final String DATABASE = "conformidade";
    private static final String USERNAME = "teste";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String CORRELATION_ID = "809254d9-cbda-49ac-b4c8-3f1289c165c5";
    private static final String INSTANCE_ID = "instancia-couchdb-1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static GenericContainer<?> couchDb;
    private static URI baseUri;

    @BeforeAll
    static void iniciarCouchDb() throws Exception {
        couchDb = new GenericContainer<>(COUCHDB_IMAGE)
                .withEnv("COUCHDB_USER", USERNAME)
                .withEnv("COUCHDB_PASSWORD", PASSWORD)
                .withExposedPorts(5984);
        couchDb.start();
        baseUri = URI.create(
                "http://" + couchDb.getHost() + ":" + couchDb.getMappedPort(5984));
        enviar(baseUri.resolve("/" + DATABASE), "PUT", null, 201);
    }

    @AfterAll
    static void pararCouchDb() {
        if (couchDb != null) {
            couchDb.stop();
        }
    }

    @Test
    void persisteEntradaERecuperaProjecaoDepoisDeReconstruirAdapter() throws Exception {
        var solicitacao = new SolicitacaoAnaliseConformidade(
                CORRELATION_ID,
                "DOC-2026-000123",
                "Texto sensível persistido somente no CouchDB",
                1000012583L,
                1);
        var primeiroAdapter = novoAdapter();

        primeiroAdapter.iniciar(INSTANCE_ID, solicitacao);

        var adapterAposReinicio = novoAdapter();
        var visao = adapterAposReinicio.consultar(INSTANCE_ID).orElseThrow();
        assertEquals(CORRELATION_ID, visao.correlationId());
        assertEquals(INSTANCE_ID, visao.instanceId());
        assertEquals("DOC-2026-000123", visao.identificadorDocumento());
        assertEquals(1000012583L, visao.identificadorChecklist());
        assertEquals(1, visao.versaoChecklist());

        var entrada = documento(CouchDbIds.entrada(CORRELATION_ID));
        assertEquals("Texto sensível persistido somente no CouchDB", entrada.path("texto").asText());
        assertEquals("DOC-2026-000123", entrada.path("identificadorDocumento").asText());
        assertTrue(entrada.path("versaoSchema").canConvertToInt());
        assertEquals((short) 1, entrada.path("versaoSchema").shortValue());
    }

    @Test
    void retomaInicioQuandoAEntradaFoiPersistidaAntesDeUmaFalhaParcial() throws Exception {
        var correlationId = UUID.randomUUID().toString();
        var instanceId = "instancia-parcial-" + UUID.randomUUID();
        var solicitacao = new SolicitacaoAnaliseConformidade(
                correlationId,
                "DOC-PARCIAL-1",
                "Texto preservado durante a retomada",
                1000012583L,
                1);
        var entrada = OBJECT_MAPPER.createObjectNode()
                .put("correlationId", correlationId)
                .put("instanceId", instanceId)
                .put("identificadorDocumento", solicitacao.identificadorDocumento())
                .put("identificadorChecklist", solicitacao.identificadorChecklist())
                .put("versaoChecklist", solicitacao.versaoChecklist())
                .put("id", CouchDbIds.entrada(correlationId))
                .put("tipo", "entrada-analise")
                .put("versaoSchema", CouchDbAnaliseConformidadeStore.VERSAO_SCHEMA)
                .put("texto", solicitacao.texto());
        enviar(
                baseUri.resolve("/" + DATABASE + "/" + CouchDbIds.entrada(correlationId)),
                "PUT",
                OBJECT_MAPPER.writeValueAsString(entrada),
                201);

        novoAdapter().iniciar(instanceId, solicitacao);

        var visao = novoAdapter().consultar(instanceId).orElseThrow();
        assertEquals(correlationId, visao.correlationId());
        assertEquals(instanceId, visao.instanceId());
    }

    @Test
    void persisteChecklistComHashStringCanonicoNaProjecao() throws Exception {
        var correlationId = UUID.randomUUID().toString();
        var instanceId = "instancia-hash-" + UUID.randomUUID();
        var solicitacao = new SolicitacaoAnaliseConformidade(
                correlationId,
                "DOC-HASH-1",
                "Texto protegido",
                1000012583L,
                1);
        var checklist = checklist();
        var adapter = novoAdapter();
        adapter.iniciar(instanceId, solicitacao);

        adapter.registrarChecklist(instanceId, checklist);

        var snapshot = documento(CouchDbIds.checklist(correlationId));
        String hash = snapshot.path("hashConteudo").asText();
        byte[] conteudo = OBJECT_MAPPER.writeValueAsBytes(snapshot.path("checklist"));
        String esperado = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(conteudo));
        assertTrue(snapshot.path("hashConteudo").isTextual());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(esperado, hash);
        var projecao = documento(CouchDbIds.projecao(instanceId));
        assertEquals(CouchDbIds.checklist(correlationId), projecao.path("checklistRef").asText());
        assertEquals(hash, projecao.path("checklistHash").asText());
    }

    @Override
    protected ArmazenarEstadoAnaliseConformidade novoStore() {
        return novoAdapter();
    }

    private static ArmazenarEstadoAnaliseConformidade novoAdapter() {
        var client = new CouchDbClient(
                baseUri,
                OBJECT_MAPPER,
                USERNAME,
                PASSWORD);
        return new CouchDbAnaliseConformidadeStore(
                client,
                OBJECT_MAPPER,
                DATABASE);
    }

    private static com.fasterxml.jackson.databind.JsonNode documento(String documentId)
            throws Exception {
        var corpo = enviar(
                baseUri.resolve("/" + DATABASE + "/" + documentId),
                "GET",
                null,
                200);
        return OBJECT_MAPPER.readTree(corpo);
    }

    private static String enviar(URI uri, String method, String body, int expectedStatus)
            throws Exception {
        var publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        var request = HttpRequest.newBuilder(uri)
                .header("Authorization", autorizacao())
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        var response = HTTP_CLIENT.send(request, ofString());
        assertEquals(expectedStatus, response.statusCode(), response.body());
        return response.body();
    }

    private static String autorizacao() {
        var credenciais = USERNAME + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(
                credenciais.getBytes(StandardCharsets.UTF_8));
    }
}
