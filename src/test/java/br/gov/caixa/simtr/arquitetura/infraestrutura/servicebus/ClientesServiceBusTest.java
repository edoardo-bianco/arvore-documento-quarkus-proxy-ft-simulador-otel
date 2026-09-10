package br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** SDK simulado: nenhum cliente real, Docker, emulador ou fila externa e utilizado. */
@QuarkusTest
class ClientesServiceBusTest {

    @ParameterizedTest
    @EnumSource(AmqpTransportType.class)
    void deveCriarUmaVezClientesDasDuasFilasComTransporteConfigurado(AmqpTransportType transporte) {
        var fixture = new ClientesFixture();
        var clientes = fixture.criar(transporte);
        assertSame(fixture.senderEntrada, clientes.senderEntrada());
        assertSame(fixture.senderSaida, clientes.senderSaida());
        assertSame(fixture.receiverEntrada, clientes.receiverEntrada());
        assertSame(fixture.receiverSaida, clientes.receiverSaida());
        verify(fixture.builder).transportType(transporte);
        verify(fixture.builder, times(2)).sender();
        verify(fixture.builder, times(2)).receiver();
        verify(fixture.builder.receiver().queueName("entrada").receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete()).prefetchCount(0);
        verify(fixture.builder.receiver().queueName("saida").receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete(), never()).prefetchCount(anyInt());
        verifyNoInteractions(fixture.senderEntrada, fixture.senderSaida,
                fixture.receiverEntrada, fixture.receiverSaida);
        clientes.fechar();
    }

    @Test
    void deveFecharTodosNaOrdemInversaUmaUnicaVez() {
        var fixture = new ClientesFixture();
        var clientes = fixture.criar(AmqpTransportType.AMQP);
        clientes.encerrar(new ShutdownEvent());
        clientes.fechar();
        var ordem = inOrder(fixture.receiverSaida, fixture.receiverEntrada,
                fixture.senderSaida, fixture.senderEntrada);
        ordem.verify(fixture.receiverSaida).close();
        ordem.verify(fixture.receiverEntrada).close();
        ordem.verify(fixture.senderSaida).close();
        ordem.verify(fixture.senderEntrada).close();
        ordem.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void deveExigirConnectionStringSemCriarClientes(String connectionString) {
        var builder = mock(ServiceBusClientBuilder.class);
        var falha = assertThrows(IllegalStateException.class, () ->
                new ClientesServiceBus(builder, "entrada", "saida", AmqpTransportType.AMQP, connectionString));
        assertEquals("Connection string Service Bus obrigatoria.", falha.getMessage());
        verifyNoInteractions(builder);
    }

    @Test
    void deveLiberarClientesAbertosSeInicializacaoFalhar() {
        var fixture = new ClientesFixture();
        when(fixture.builder.receiver().queueName("saida").receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete().buildAsyncClient()).thenThrow(new IllegalStateException("dado-restrito"));
        var falha = assertThrows(IllegalStateException.class, () -> fixture.criar(AmqpTransportType.AMQP));
        assertEquals("Falha ao inicializar clientes Service Bus.", falha.getMessage());
        assertNull(falha.getCause());
        verify(fixture.receiverEntrada).close();
        verify(fixture.senderSaida).close();
        verify(fixture.senderEntrada).close();
    }

    @Test
    void deveContinuarFechandoDepoisDeFalhaSemExporCausa() {
        var fixture = new ClientesFixture();
        var clientes = fixture.criar(AmqpTransportType.AMQP);
        doThrow(new IllegalStateException("dado-restrito")).when(fixture.receiverSaida).close();
        var falha = assertThrows(IllegalStateException.class, clientes::fechar);
        assertEquals("Falha ao fechar clientes Service Bus.", falha.getMessage());
        assertNull(falha.getCause());
        verify(fixture.receiverEntrada).close();
        verify(fixture.senderSaida).close();
        verify(fixture.senderEntrada).close();
        clientes.fechar();
    }

    @Test
    void deveManterSomenteDiagnosticoSeguroDeFalhaParcialEFechamento() {
        var fixture = new ClientesFixture();
        when(fixture.builder.sender().queueName("saida").buildAsyncClient())
                .thenThrow(new IllegalStateException("causa-restrita"));
        doThrow(new IllegalStateException("fechamento-restrito")).when(fixture.senderEntrada).close();
        var falha = assertThrows(IllegalStateException.class, () -> fixture.criar(AmqpTransportType.AMQP));
        assertNull(falha.getCause());
        assertEquals(1, falha.getSuppressed().length);
        assertEquals("Falha ao fechar clientes Service Bus.", falha.getSuppressed()[0].getMessage());
        assertNull(falha.getSuppressed()[0].getCause());
    }

    private static final class ClientesFixture {
        private final ServiceBusClientBuilder builder = mock(ServiceBusClientBuilder.class, RETURNS_DEEP_STUBS);
        private final ServiceBusSenderAsyncClient senderEntrada =
                builder.sender().queueName("entrada").buildAsyncClient();
        private final ServiceBusSenderAsyncClient senderSaida =
                builder.sender().queueName("saida").buildAsyncClient();
        private final ServiceBusReceiverAsyncClient receiverEntrada = builder.receiver().queueName("entrada")
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete().prefetchCount(0).buildAsyncClient();
        private final ServiceBusReceiverAsyncClient receiverSaida = builder.receiver().queueName("saida")
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete().buildAsyncClient();

        private ClientesFixture() {
            clearInvocations(builder.receiver().queueName("entrada")
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete());
            clearInvocations(builder);
        }

        private ClientesServiceBus criar(AmqpTransportType transporte) {
            return new ClientesServiceBus(builder, "entrada", "saida", transporte, "configuracao-sintetica");
        }
    }
}
