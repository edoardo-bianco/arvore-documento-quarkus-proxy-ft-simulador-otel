package br.gov.caixa.simtr.orquestrador.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ClientesServiceBus;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Integracao real opt-in: mvn -Pservicebus-integration test. */
@QuarkusTest
@Tag("servicebus-integration")
@TestProfile(ServiceBusEmuladorTestProfile.class)
class MonitoramentoEntradaEmuladorTest {

    private static final String PATH = "/simtr-hub/v1/monitoramentos-dossie";
    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Inject
    ClientesServiceBus clientes;
    @Inject
    @FilaEntrada
    ServiceBusSenderAsyncClient senderEntrada;
    @Inject
    @FilaSaida
    ServiceBusSenderAsyncClient senderSaida;
    @Inject
    @FilaEntrada
    ServiceBusReceiverAsyncClient receiverEntrada;
    @Inject
    @FilaSaida
    ServiceBusReceiverAsyncClient receiverSaida;
    @Inject
    ObjectMapper json;

    @Test
    void devePublicarUmaMensagemV1PeloFluxoRestCompleto() throws Exception {
        var preValidacao = "pre-" + UUID.randomUUID();
        var antes = Instant.now();
        var resposta = given().contentType(ContentType.JSON)
                .body(java.util.Map.of("idDossiePreValidacao", preValidacao, "idDossieMtr", "0007"))
                .when().post(PATH).then().statusCode(202).extract().jsonPath();
        var depois = Instant.now();
        String monitoramentoId = resposta.getString("monitoramentoId");
        String orquestracaoId = resposta.getString("orquestracaoId");
        assertNotNull(UUID.fromString(monitoramentoId));
        assertNotNull(UUID.fromString(orquestracaoId));
        var recebida = receiverEntrada.receiveMessages().next().block(ESPERA);
        assertNotNull(recebida);
        try {
            assertEquals(monitoramentoId + ":tentativa:1", recebida.getMessageId());
            assertEquals(orquestracaoId, recebida.getCorrelationId());
            assertEquals("MONITORAR_DOSSIE_MTR", recebida.getSubject());
            assertEquals("application/json", recebida.getContentType());
            var corpo = json.readTree(recebida.getBody().toString());
            assertEquals(9, corpo.size());
            assertEquals(1, corpo.path("schemaVersion").asInt());
            assertEquals(monitoramentoId, corpo.path("monitoramentoId").asText());
            assertEquals(orquestracaoId, corpo.path("orquestracaoId").asText());
            assertEquals(preValidacao, corpo.path("idDossiePreValidacao").asText());
            assertEquals("0007", corpo.path("idDossieMtr").asText());
            assertEquals(1, corpo.path("tentativaAtual").asInt());
            assertEquals("v1", corpo.path("politicaMonitoramentoVersao").asText());
            var iniciadoEm = Instant.parse(corpo.path("iniciadoEm").asText());
            assertFalse(iniciadoEm.isBefore(antes));
            assertFalse(iniciadoEm.isAfter(depois));
            assertEquals(iniciadoEm.plus(Duration.ofHours(24)),
                    Instant.parse(corpo.path("limiteEm").asText()));
        } finally {
            receiverEntrada.complete(recebida).block(ESPERA);
        }
        assertTrue(receiverEntrada.peekMessages(1).collectList().block(ESPERA).isEmpty());
    }

    @Test
    void deveResolverClientesQualificadosDaMesmaFabrica() {
        assertSame(clientes.senderEntrada(), senderEntrada);
        assertSame(clientes.senderSaida(), senderSaida);
        assertSame(clientes.receiverEntrada(), receiverEntrada);
        assertSame(clientes.receiverSaida(), receiverSaida);
    }

    @Test
    void requestInvalidoNaoDevePublicar() {
        given().contentType(ContentType.JSON).body(java.util.Map.of("idDossiePreValidacao", "pre", "idDossieMtr", "0"))
                .when().post(PATH).then().statusCode(400);
        assertTrue(receiverEntrada.peekMessages(1).collectList().block(ESPERA).isEmpty());
    }
}
