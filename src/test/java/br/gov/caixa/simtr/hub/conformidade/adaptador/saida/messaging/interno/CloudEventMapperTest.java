package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_ANALISE_CONCLUIDA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_CONCLUIDA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_SOLICITADA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_FLOW_INSTANCE_ID;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_FLOW_TASK_ID;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_CORRELATION_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CloudEventMapperTest {

    private static final JsonFormat FORMATO = new JsonFormat();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CORRELATION_ID =
            "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String INSTANCE_ID = "01J3FLOWTESTE00000000000000";

    private final CloudEventMapper mapper = new CloudEventMapper(OBJECT_MAPPER);

    @Test
    void serializaRevisaoConcluidaComoCloudEventV1EstruturadoECorrelacionado() throws Exception {
        JsonNode referencia = referenciaJson("revisao-123");

        byte[] envelope = mapper.serializarRevisaoConcluida(
                "instancia-123", referencia);

        JsonNode estruturado = OBJECT_MAPPER.readTree(envelope);
        CloudEvent evento = FORMATO.deserialize(envelope);
        assertAll(
                () -> assertEquals("1.0", estruturado.path("specversion").asText()),
                () -> assertTrue(estruturado.path("data").isObject()),
                () -> assertEquals(SpecVersion.V1, evento.getSpecVersion()),
                () -> assertFalse(evento.getId().isBlank()),
                () -> assertEquals(CloudEventMapper.SOURCE, evento.getSource()),
                () -> assertEquals(EVENTO_REVISAO_CONCLUIDA, evento.getType()),
                () -> assertNotNull(evento.getTime()),
                () -> assertEquals("application/json", evento.getDataContentType()),
                () -> assertEquals("instancia-123", evento.getExtension(EXTENSAO_FLOW_INSTANCE_ID)),
                () -> assertEquals(
                        "revisao-123",
                        OBJECT_MAPPER.readTree(evento.getData().toBytes())
                                .path("documentoRef")
                                .asText()),
                () -> assertEquals(
                        3,
                        OBJECT_MAPPER.readTree(evento.getData().toBytes()).size()));
    }

    @Test
    void rejeitaRevisaoConcluidaComPayloadNegocialAdicional() {
        var referenciaComConteudo = referenciaJson("revisao-123").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) referenciaComConteudo)
                .put("observacao", "conteúdo que não pode entrar no checkpoint");

        assertThrows(
                CloudEventInvalidoException.class,
                () -> mapper.serializarRevisaoConcluida(
                        "instancia-123", referenciaComConteudo));
    }

    @Test
    void preservaCorrelacaoETarefaAoLerEventoDeSaida() {
        byte[] envelope = FORMATO.serialize(evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-456",
                "correlacao-456",
                "tarefa-emitir-revisao",
                dadosReferenciais("resultado-preliminar-456")));

        var evento = mapper.lerEventoSaida(envelope);

        assertAll(
                () -> assertEquals("instancia-456", evento.instanceId()),
                () -> assertEquals("correlacao-456", evento.correlationId()),
                () -> assertEquals("tarefa-emitir-revisao", evento.taskId()),
                () -> assertEquals(
                        EVENTO_REVISAO_SOLICITADA,
                        evento.tipo().cloudEventType()),
                () -> assertEquals(
                        "resultado-preliminar-456",
                        evento.documento().documentoRef()));
    }

    @Test
    void leEmissaoReferencialSemPayloadNegocial() {
        CloudEvent cloudEvent = evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-referencial",
                "correlacao-referencial",
                "tarefa-referencial",
                "{\"documentoRef\":\"resultado-preliminar-123\","
                        + "\"hashConteudo\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"versaoSchema\":1}");

        var emissao = mapper.lerEmissaoReferencial(cloudEvent);

        assertAll(
                () -> assertEquals("instancia-referencial", emissao.instanceId()),
                () -> assertEquals("correlacao-referencial", emissao.correlationId()),
                () -> assertEquals("tarefa-referencial", emissao.taskId()),
                () -> assertEquals(
                        "resultado-preliminar-123",
                        emissao.documento().documentoRef()),
                () -> assertEquals((short) 1, emissao.documento().versaoSchema()),
                () -> assertFalse(
                        OBJECT_MAPPER.valueToTree(emissao.documento()).has("parecer")));
    }

    @Test
    void rejeitaEmissaoComCampoNegocialAdicional() {
        CloudEvent cloudEvent = evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-referencial",
                "correlacao-referencial",
                null,
                "{\"documentoRef\":\"resultado-preliminar-123\","
                        + "\"hashConteudo\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"versaoSchema\":1,"
                        + "\"parecer\":\"CONFORME\"}");

        assertThrows(
                CloudEventInvalidoException.class,
                () -> mapper.lerEmissaoReferencial(cloudEvent));
    }

    @Test
    void rejeitaEmissaoComHashOuVersaoInvalidos() {
        CloudEvent hashInvalido = evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-referencial",
                "correlacao-referencial",
                null,
                "{\"documentoRef\":\"resultado-preliminar-123\","
                        + "\"hashConteudo\":\"ABC123\",\"versaoSchema\":1}");
        CloudEvent versaoInvalida = evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-referencial",
                "correlacao-referencial",
                null,
                "{\"documentoRef\":\"resultado-preliminar-123\","
                        + "\"hashConteudo\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"versaoSchema\":2}");
        CloudEvent versaoTruncada = evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-referencial",
                "correlacao-referencial",
                null,
                "{\"documentoRef\":\"resultado-preliminar-123\","
                        + "\"hashConteudo\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"versaoSchema\":65537}");

        assertAll(
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.lerEmissaoReferencial(hashInvalido)),
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.lerEmissaoReferencial(versaoInvalida)),
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.lerEmissaoReferencial(versaoTruncada)));
    }

    @Test
    void aceitaOsDoisTiposProduzidosPeloWorkflow() {
        var revisao = mapper.lerEventoSaida(eventoEstruturado(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-revisao",
                null,
                dadosReferenciais("resultado-preliminar-revisao")));
        var conclusao = mapper.lerEventoSaida(eventoEstruturado(
                EVENTO_ANALISE_CONCLUIDA,
                "instancia-conclusao",
                null,
                dadosReferenciais("resultado-final-conclusao")));

        assertAll(
                () -> assertEquals(EVENTO_REVISAO_SOLICITADA, revisao.tipo().cloudEventType()),
                () -> assertEquals(EVENTO_ANALISE_CONCLUIDA, conclusao.tipo().cloudEventType()));
    }

    @Test
    void rejeitaTipoDesconhecidoDeModoControlado() {
        byte[] envelope = eventoEstruturado(
                "br.gov.caixa.simtr.conformidade.evento.desconhecido.v1",
                "instancia-123",
                null,
                "{\"valor\":1}");

        assertThrows(CloudEventInvalidoException.class, () -> mapper.lerEventoSaida(envelope));
    }

    @Test
    void rejeitaCorrelacaoAusenteDeModoControlado() {
        byte[] envelope = FORMATO.serialize(evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-123",
                null,
                null,
                dadosReferenciais("resultado-preliminar-123")));

        assertThrows(CloudEventInvalidoException.class, () -> mapper.lerEventoSaida(envelope));
    }

    @Test
    void rejeitaPayloadQueNaoSejaObjetoJsonDeModoControlado() {
        byte[] envelope = eventoEstruturado(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-123",
                null,
                "\"conteudo-invalido\"");

        assertThrows(CloudEventInvalidoException.class, () -> mapper.lerEventoSaida(envelope));
    }

    @Test
    void rejeitaEnvelopeMalformadoSemExporSeuConteudo() {
        byte[] envelope = "{segredo".getBytes(StandardCharsets.UTF_8);

        CloudEventInvalidoException falha = assertThrows(
                CloudEventInvalidoException.class,
                () -> mapper.lerEventoSaida(envelope));

        assertFalse(falha.getMessage().contains("segredo"));
    }

    @Test
    void rejeitaInstanceIdVazioAntesDePublicar() {
        JsonNode referencia = referenciaJson("revisao-123");

        assertThrows(
                CloudEventInvalidoException.class,
                () -> mapper.serializarRevisaoConcluida(" ", referencia));
    }

    @Test
    void mapeiaDocumentoDeRevisaoPersistidoComIdentidadeEReferenciaIntegras() throws Exception {
        JsonNode documento = documentoRevisao();

        CloudEvent evento = mapper.mapearRevisaoPersistida(documento);
        JsonNode referencia = OBJECT_MAPPER.readTree(evento.getData().toBytes());

        assertAll(
                () -> assertEquals(
                        "conformidade:" + documento.path("id").asText(),
                        evento.getId()),
                () -> assertEquals(EVENTO_REVISAO_CONCLUIDA, evento.getType()),
                () -> assertEquals(
                        INSTANCE_ID,
                        evento.getExtension(EXTENSAO_FLOW_INSTANCE_ID)),
                () -> assertEquals(
                        CORRELATION_ID,
                        evento.getExtension(EXTENSAO_CORRELATION_ID)),
                () -> assertEquals(documento.path("id"), referencia.path("documentoRef")),
                () -> assertEquals(documento.path("hashConteudo"), referencia.path("hashConteudo")),
                () -> assertEquals(3, referencia.size()));
    }

    @Test
    void rejeitaDocumentoPersistidoComTipoVersaoOuRevisaoInvalidos() {
        JsonNode tipoInvalido = documentoRevisao().put("tipo", "resultado-final");
        JsonNode versaoInvalida = documentoRevisao().put("versaoSchema", 2);
        JsonNode revisaoInvalida = documentoRevisao().put("revisao", "conteudo-invalido");

        assertAll(
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.mapearRevisaoPersistida(tipoInvalido)),
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.mapearRevisaoPersistida(versaoInvalida)),
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.mapearRevisaoPersistida(revisaoInvalida)));
    }

    @Test
    void rejeitaEmissaoComTipoReservadoAEntradaOuSourceDivergente() {
        CloudEvent tipoDeEntrada = evento(
                EVENTO_REVISAO_CONCLUIDA,
                "instancia-referencial",
                "correlacao-referencial",
                null,
                dadosReferenciais("revisao-123"));
        CloudEvent sourceDivergente = evento(
                URI.create("urn:outro-sistema:conformidade"),
                EVENTO_REVISAO_SOLICITADA,
                "instancia-referencial",
                "correlacao-referencial",
                null,
                dadosReferenciais("resultado-preliminar-123"));

        assertAll(
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.lerEmissaoReferencial(tipoDeEntrada)),
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.lerEmissaoReferencial(sourceDivergente)));
    }

    @Test
    void rejeitaEnvelopeAusenteOuDataComJsonMalformado() {
        byte[] envelopeAusente = new byte[0];
        CloudEvent jsonMalformado = evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-referencial",
                "correlacao-referencial",
                null,
                "{");

        assertAll(
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.lerEventoSaida(envelopeAusente)),
                () -> assertThrows(
                        CloudEventInvalidoException.class,
                        () -> mapper.lerEmissaoReferencial(jsonMalformado)));
    }

    static byte[] eventoEstruturado(
            String tipo,
            String instanceId,
            String taskId,
            String dadosJson) {
        var builder = CloudEventBuilder.v1()
                .withId("evento-teste-" + tipo)
                .withSource(CloudEventMapper.SOURCE)
                .withType(tipo)
                .withTime(OffsetDateTime.parse("2026-07-24T12:00:00Z"))
                .withDataContentType("application/json")
                .withData(dadosJson.getBytes(StandardCharsets.UTF_8));
        if (instanceId != null) {
            builder.withExtension(EXTENSAO_FLOW_INSTANCE_ID, instanceId);
            builder.withExtension(EXTENSAO_CORRELATION_ID, "correlacao-teste");
        }
        if (taskId != null) {
            builder.withExtension(EXTENSAO_FLOW_TASK_ID, taskId);
        }
        return FORMATO.serialize(builder.build());
    }

    static CloudEvent evento(
            String tipo,
            String instanceId,
            String correlationId,
            String taskId,
            String dadosJson) {
        return evento(
                CloudEventMapper.SOURCE,
                tipo,
                instanceId,
                correlationId,
                taskId,
                dadosJson);
    }

    static CloudEvent evento(
            URI source,
            String tipo,
            String instanceId,
            String correlationId,
            String taskId,
            String dadosJson) {
        var builder = CloudEventBuilder.v1()
                .withId("evento-teste-" + tipo)
                .withSource(source)
                .withType(tipo)
                .withTime(OffsetDateTime.parse("2026-08-02T12:00:00Z"))
                .withDataContentType("application/json")
                .withData(dadosJson.getBytes(StandardCharsets.UTF_8));
        if (instanceId != null) {
            builder.withExtension(EXTENSAO_FLOW_INSTANCE_ID, instanceId);
        }
        if (correlationId != null) {
            builder.withExtension(EXTENSAO_CORRELATION_ID, correlationId);
        }
        if (taskId != null) {
            builder.withExtension(EXTENSAO_FLOW_TASK_ID, taskId);
        }
        return builder.build();
    }

    static String dadosReferenciais(String documentoRef) {
        return "{\"documentoRef\":\"" + documentoRef
                + "\",\"hashConteudo\":\"" + "a".repeat(64)
                + "\",\"versaoSchema\":1}";
    }

    private static JsonNode referenciaJson(String documentoRef) {
        return OBJECT_MAPPER.createObjectNode()
                .put("documentoRef", documentoRef)
                .put("hashConteudo", "a".repeat(64))
                .put("versaoSchema", 1);
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode documentoRevisao() {
        var revisao = new RevisaoHumanaConformidade(
                "Aprovado pelo revisor",
                java.util.List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.CONFORME,
                        "Confirmado",
                        "Trecho",
                        0.9d)));
        var referencia = new ReferenciasDocumentoAnaliseConformidade(OBJECT_MAPPER)
                .revisao(CORRELATION_ID, revisao);
        return OBJECT_MAPPER.createObjectNode()
                .put("id", referencia.documentoRef())
                .put("tipo", "revisao-humana")
                .put("instanceId", INSTANCE_ID)
                .put("correlationId", CORRELATION_ID)
                .put("versaoSchema", referencia.versaoSchema())
                .put("hashConteudo", referencia.hashConteudo())
                .set("revisao", OBJECT_MAPPER.valueToTree(revisao));
    }
}
