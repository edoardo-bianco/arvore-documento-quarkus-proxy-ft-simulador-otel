package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_SOLICITADA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_FLOW_INSTANCE_ID;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_FLOW_TASK_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CanaisInternosMessagingQuarkusTest {

    @Inject
    RevisaoHumanaCloudEventPublisher revisaoPublisher;

    @Inject
    FlowMessagingConsumerSemConnector flowInConsumer;

    @Inject
    FlowDomainEventsPublisherSemConnector flowOutPublisher;

    @Inject
    EventosFlowOutEmMemoria eventos;

    @Inject
    ArmazenarEstadoAnaliseConformidade estados;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void limparEventos() {
        eventos.limpar();
    }

    @Test
    void fechaFlowInComCloudEventEstruturadoSemConnector() throws Exception {
        var revisao = new RevisaoHumanaConformidade(
                "Aprovado pelo revisor",
                java.util.List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.CONFORME,
                        "Confirmado",
                        "Trecho",
                        0.9d)));

        revisaoPublisher.publicar("instancia-flow-in", revisao)
                .subscribeAsCompletionStage()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        var config = ConfigProvider.getConfig();
        assertAll(
                () -> assertNotNull(flowInConsumer),
                () -> assertTrue(config
                        .getOptionalValue("mp.messaging.incoming.flow-in.connector", String.class)
                        .isEmpty()));
    }

    @Test
    void fechaFlowOutERegistraEventoAntesDaConfirmacaoSemConnector() throws Exception {
        var resultado = new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist documental",
                "Resultado preliminar",
                java.util.List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.INCONCLUSIVO,
                        "Requer revisão",
                        null,
                        0.9d)),
                OrigemResultado.AGENTE);
        estados.iniciar("instancia-flow-out");
        var evento = CloudEventBuilder.v1()
                .withId("evento-flow-out")
                .withSource(URI.create("urn:simtr-hub:conformidade:workflow"))
                .withType(EVENTO_REVISAO_SOLICITADA)
                .withTime(OffsetDateTime.parse("2026-07-24T12:00:00Z"))
                .withDataContentType("application/json")
                .withData(objectMapper.writeValueAsBytes(resultado))
                .withExtension(EXTENSAO_FLOW_INSTANCE_ID, "instancia-flow-out")
                .withExtension(EXTENSAO_FLOW_TASK_ID, "emitir-revisao")
                .build();

        flowOutPublisher.publish(evento).get(10, TimeUnit.SECONDS);

        var recebidos = eventos.eventos("instancia-flow-out");
        var config = ConfigProvider.getConfig();
        assertAll(
                () -> assertEquals(1, recebidos.size()),
                () -> assertEquals(EVENTO_REVISAO_SOLICITADA, recebidos.getFirst().tipo()),
                () -> assertEquals("instancia-flow-out", recebidos.getFirst().instanceId()),
                () -> assertEquals("emitir-revisao", recebidos.getFirst().taskId()),
                () -> assertEquals(
                        1000012583L,
                        recebidos.getFirst().dados()
                                .path("identificadorChecklist")
                                .asLong()),
                () -> assertEquals(
                        StatusAnaliseConformidade.AGUARDANDO_REVISAO,
                        estados.consultar("instancia-flow-out").orElseThrow().status()),
                () -> assertNotNull(flowOutPublisher),
                () -> assertTrue(config
                        .getOptionalValue("mp.messaging.outgoing.flow-out.connector", String.class)
                        .isEmpty()));
    }
}
