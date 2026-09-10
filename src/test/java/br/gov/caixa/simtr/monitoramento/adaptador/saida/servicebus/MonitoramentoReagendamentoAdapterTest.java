package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.ServiceBusTransactionContext;
import com.azure.messaging.servicebus.models.CompleteOptions;
import jakarta.enterprise.inject.Instance;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

@io.quarkus.test.junit.QuarkusTest
class MonitoramentoReagendamentoAdapterTest {
    private static final Instant INICIO = Instant.parse("2026-09-09T12:00:00Z");
    private static final ReagendamentoMonitoramento PEDIDO = new ReagendamentoMonitoramento(
            new TentativaMonitoramento("MON", "ORQ", "pre", "0007", 4, INICIO,
                    INICIO.plus(Duration.ofHours(24)), "v-removida"), INICIO.plusSeconds(1800));
    private static final Duration ESPERA = Duration.ofSeconds(2);

    private final ServiceBusReceiverAsyncClient receiver = mock(ServiceBusReceiverAsyncClient.class);
    private final ServiceBusSenderAsyncClient sender = mock(ServiceBusSenderAsyncClient.class);
    private final ServiceBusReceivedMessage atual = mock(ServiceBusReceivedMessage.class);
    private final ServiceBusTransactionContext tx = mock(ServiceBusTransactionContext.class);
    private final MonitoramentoReagendamentoServiceBusMapper mapper = mock(MonitoramentoReagendamentoServiceBusMapper.class);
    private final ServiceBusMessage proxima = new ServiceBusMessage("corpo-controlado");
    private MonitoramentoReagendamentoAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        Instance<ServiceBusSenderAsyncClient> clientes = mock(Instance.class);
        when(clientes.get()).thenReturn(sender);
        adapter = new MonitoramentoReagendamentoAdapter(clientes, mapper);
        when(mapper.paraMensagem(PEDIDO.proximaTentativa())).thenReturn(proxima);
        when(receiver.createTransaction()).thenReturn(Mono.just(tx));
        when(sender.scheduleMessage(any(), any(OffsetDateTime.class), same(tx))).thenReturn(Mono.just(7L));
        when(receiver.complete(same(atual), any(CompleteOptions.class))).thenReturn(Mono.empty());
        when(receiver.commitTransaction(tx)).thenReturn(Mono.empty());
        when(receiver.rollbackTransaction(tx)).thenReturn(Mono.empty());
    }

    @Test
    void associaMesmaEntregaEConfirmaApenasUmaTransacaoMesmoComDuasAssinaturas() {
        var operacao = adapter.associar(receiver, atual).executar(PEDIDO);
        verifyNoInteractions(mapper, sender, receiver, atual);
        operacao.await().atMost(ESPERA);
        operacao.await().atMost(ESPERA);

        var ordem = inOrder(mapper, receiver, sender);
        ordem.verify(mapper).paraMensagem(PEDIDO.proximaTentativa());
        ordem.verify(receiver).createTransaction();
        ordem.verify(sender).scheduleMessage(proxima, OffsetDateTime.ofInstant(PEDIDO.agendadoEm(), ZoneOffset.UTC), tx);
        var opcoes = ArgumentCaptor.forClass(CompleteOptions.class);
        ordem.verify(receiver).complete(same(atual), opcoes.capture());
        assertSame(tx, opcoes.getValue().getTransactionContext());
        ordem.verify(receiver).commitTransaction(tx);
        verify(receiver, never()).rollbackTransaction(any());
        verify(receiver, never()).complete(any());
        verify(receiver, never()).abandon(any());
        verifyNoMoreInteractions(receiver, sender);
    }

    @Test
    void falhaDeMapeamentoNaoAbreTransacaoNemExpoeCausa() {
        when(mapper.paraMensagem(any())).thenThrow(new IllegalArgumentException("segredo-corpo"));
        var operacao = adapter.associar(receiver, atual).executar(PEDIDO);
        var espera = operacao.await();
        var falha = assertThrows(IllegalStateException.class, () -> espera.atMost(ESPERA));
        assertEquals("Reagendamento transacional nao confirmado.", falha.getMessage());
        assertNull(falha.getCause());
        verifyNoInteractions(receiver, sender);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falhaAoCriarTransacaoNaoTentaLiquidarOuDesfazerContextoInexistente(boolean sincrona) {
        when(receiver.createTransaction()).thenAnswer(_ -> falhar(sincrona));
        var operacao = adapter.associar(receiver, atual).executar(PEDIDO);
        var espera = operacao.await();
        assertThrows(IllegalStateException.class, () -> espera.atMost(ESPERA));
        verify(receiver).createTransaction();
        verifyNoMoreInteractions(receiver);
        verifyNoInteractions(sender);
    }

    @ParameterizedTest
    @CsvSource({"schedule,false", "schedule,true", "complete,false", "complete,true",
            "commit,false", "commit,true", "rollback,false", "rollback,true"})
    void falhasRespeitamFronteiraDoCommitSemSegundaLiquidacao(String etapa, boolean sincrona) {
        switch (etapa) {
            case "schedule" -> when(sender.scheduleMessage(any(), any(OffsetDateTime.class), same(tx)))
                    .thenAnswer(_ -> falhar(sincrona));
            case "complete" -> when(receiver.complete(same(atual), any(CompleteOptions.class)))
                    .thenAnswer(_ -> falhar(sincrona));
            case "commit" -> when(receiver.commitTransaction(tx)).thenAnswer(_ -> falhar(sincrona));
            case "rollback" -> {
                when(sender.scheduleMessage(any(), any(OffsetDateTime.class), same(tx)))
                        .thenReturn(Mono.error(new IllegalStateException("falha-schedule")));
                when(receiver.rollbackTransaction(tx)).thenAnswer(_ -> falhar(sincrona));
            }
            default -> throw new AssertionError(etapa);
        }
        var operacao = adapter.associar(receiver, atual).executar(PEDIDO);
        var espera = operacao.await();
        var falha = assertThrows(IllegalStateException.class, () -> espera.atMost(ESPERA));
        assertEquals("Reagendamento transacional nao confirmado.", falha.getMessage());
        assertNull(falha.getCause());
        if ("commit".equals(etapa)) {
            verify(receiver).commitTransaction(tx);
            verify(receiver, never()).rollbackTransaction(any());
        } else {
            verify(receiver).rollbackTransaction(tx);
            verify(receiver, never()).commitTransaction(any());
        }
        verify(sender).scheduleMessage(same(proxima), any(OffsetDateTime.class), same(tx));
        verify(receiver, never()).complete(any());
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any());
        verify(receiver, never()).close();
        verify(sender, never()).close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"schedule", "commit"})
    void cancelamentoAlcancaOperacaoPendenteSemOutraLiquidacao(String etapa) {
        var pendente = new java.util.concurrent.CompletableFuture<Void>();
        if ("schedule".equals(etapa)) {
            when(sender.scheduleMessage(any(), any(OffsetDateTime.class), same(tx)))
                    .thenReturn(Mono.fromFuture(pendente).thenReturn(7L));
        } else {
            when(receiver.commitTransaction(tx)).thenReturn(Mono.fromFuture(pendente));
        }
        var operacao = adapter.associar(receiver, atual).executar(PEDIDO);
        var inscricao = operacao.subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(inscricao.isDone());
        try {
            inscricao.cancel(true);
            assertTrue(pendente.isCancelled(), "Cancelamento deve alcancar o futuro do SDK.");
            assertThrows(java.util.concurrent.CancellationException.class, () -> operacao.await().atMost(ESPERA));
            verify(receiver).createTransaction();
            if ("schedule".equals(etapa)) {
                verify(receiver, never()).complete(any(), any(CompleteOptions.class));
                verify(receiver, never()).commitTransaction(any());
            } else {
                verify(receiver).complete(same(atual), any(CompleteOptions.class));
                verify(receiver).commitTransaction(tx);
            }
            verify(receiver, never()).rollbackTransaction(any());
            verify(receiver, never()).complete(any());
            verify(receiver, never()).abandon(any());
            verify(receiver, never()).close();
        } finally {
            pendente.cancel(true);
        }
    }

    private static <T> Mono<T> falhar(boolean sincrona) {
        var falha = new IllegalStateException("segredo-sdk");
        if (sincrona) throw falha;
        return Mono.error(falha);
    }
}
