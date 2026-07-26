package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_ANALISE_CONCLUIDA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_CONCLUIDA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_SOLICITADA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_FLOW_INSTANCE_ID;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_FLOW_TASK_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CloudEventMapperTest {

    private static final JsonFormat FORMATO = new JsonFormat();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CloudEventMapper mapper = new CloudEventMapper(OBJECT_MAPPER);

    @Test
    void serializaRevisaoConcluidaComoCloudEventV1EstruturadoECorrelacionado() throws Exception {
        JsonNode revisao = OBJECT_MAPPER.createObjectNode()
                .put("observacao", "Revisão humana concluída");

        byte[] envelope = mapper.serializarRevisaoConcluida("instancia-123", revisao);

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
                        "Revisão humana concluída",
                        OBJECT_MAPPER.readTree(evento.getData().toBytes())
                                .path("observacao")
                                .asText()));
    }

    @Test
    void preservaCorrelacaoETarefaAoLerEventoDeSaida() {
        byte[] envelope = eventoEstruturado(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-456",
                "tarefa-emitir-revisao",
                "{\"parecer\":\"INCONCLUSIVO\"}");

        EventoFlowOutRecebido evento = mapper.lerEventoSaida(envelope);

        assertAll(
                () -> assertEquals("instancia-456", evento.instanceId()),
                () -> assertEquals("tarefa-emitir-revisao", evento.taskId()),
                () -> assertEquals(EVENTO_REVISAO_SOLICITADA, evento.tipo()),
                () -> assertEquals("INCONCLUSIVO", evento.dados().path("parecer").asText()));
    }

    @Test
    void aceitaOsDoisTiposProduzidosPeloWorkflow() {
        EventoFlowOutRecebido revisao = mapper.lerEventoSaida(eventoEstruturado(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-revisao",
                null,
                "{\"estado\":\"AGUARDANDO_REVISAO\"}"));
        EventoFlowOutRecebido conclusao = mapper.lerEventoSaida(eventoEstruturado(
                EVENTO_ANALISE_CONCLUIDA,
                "instancia-conclusao",
                null,
                "{\"estado\":\"CONCLUIDA\"}"));

        assertAll(
                () -> assertEquals(EVENTO_REVISAO_SOLICITADA, revisao.tipo()),
                () -> assertEquals(EVENTO_ANALISE_CONCLUIDA, conclusao.tipo()));
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
        byte[] envelope = eventoEstruturado(
                EVENTO_REVISAO_SOLICITADA,
                null,
                null,
                "{\"valor\":1}");

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
        JsonNode revisao = OBJECT_MAPPER.createObjectNode().put("observacao", "ok");

        assertThrows(
                CloudEventInvalidoException.class,
                () -> mapper.serializarRevisaoConcluida(" ", revisao));
    }

    static byte[] eventoEstruturado(
            String tipo,
            String instanceId,
            String taskId,
            String dadosJson) {
        var builder = CloudEventBuilder.v1()
                .withId("evento-teste-" + tipo)
                .withSource(URI.create("urn:simtr-hub:conformidade:workflow"))
                .withType(tipo)
                .withTime(OffsetDateTime.parse("2026-07-24T12:00:00Z"))
                .withDataContentType("application/json")
                .withData(dadosJson.getBytes(StandardCharsets.UTF_8));
        if (instanceId != null) {
            builder.withExtension(EXTENSAO_FLOW_INSTANCE_ID, instanceId);
        }
        if (taskId != null) {
            builder.withExtension(EXTENSAO_FLOW_TASK_ID, taskId);
        }
        return FORMATO.serialize(builder.build());
    }
}
