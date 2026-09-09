package br.gov.caixa.simtr.monitoramento.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.PublicarResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Prova opt-in de publicacao; a assinatura de recebimento permanece aberta ate o Complete. */
@QuarkusTest
@Tag("servicebus-integration")
@TestProfile(ServiceBusEmuladorTestProfile.class)
class PublicarResultadoMonitoramentoEmuladorTest {

    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Inject
    PublicarResultadoMonitoramento publicacao;
    @Inject
    @FilaSaida
    ServiceBusReceiverAsyncClient receiver;
    @Inject
    br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoServiceBusMapper consumidor;
    @Inject
    ObjectMapper json;

    @ParameterizedTest
    @ValueSource(strings = {"CONCLUSIVO", "QUARENTENA"})
    void devePublicarNaSaidaPreservandoContratoECorrelacao(String tipo) {
        var instante = Instant.now();
        boolean conclusivo = "CONCLUSIVO".equals(tipo);
        var esperado = new ResultadoMonitoramento(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                "pre-sintetica", "0007", tipo, conclusivo ? "PENDENTE_INFORMACAO" : null,
                conclusivo ? "NAO_CONFORME" : "QUARENTENA",
                conclusivo ? "SITUACAO_CONCLUSIVA_MTR" : "PRAZO_MAXIMO",
                conclusivo ? 1 : 0, instante, instante, 42L);

        publicacao.executar(esperado).await().atMost(ESPERA);
        var recebida = receiver.receiveMessages()
                .concatMap(mensagem -> receiver.complete(mensagem).thenReturn(mensagem), 1)
                .next().block(ESPERA);

        assertNotNull(recebida);
        var resultado = consumidor.paraResultado(recebida.getBody().toString(), recebida.getMessageId(),
                recebida.getCorrelationId(), recebida.getSubject(), recebida.getContentType());
        assertEquals(json.valueToTree(esperado), json.valueToTree(resultado));
    }
}
