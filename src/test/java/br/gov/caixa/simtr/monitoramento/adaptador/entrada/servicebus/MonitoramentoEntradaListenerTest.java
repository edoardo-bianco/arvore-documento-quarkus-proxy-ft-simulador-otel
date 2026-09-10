package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.ProcessarTentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.MonitoramentoReagendamentoAdapter;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ReagendarTentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@QuarkusTest
class MonitoramentoEntradaListenerTest {
    private static final Instant INICIO = Instant.parse("2026-09-09T12:00:00Z");
    private static final String CORPO = """
            {"schemaVersion":1,"monitoramentoId":"MON-1","orquestracaoId":"ORQ-1",
             "idDossiePreValidacao":"pre-1","idDossieMtr":"0007","tentativaAtual":1,
             "iniciadoEm":"2026-09-09T12:00:00Z","limiteEm":"2026-09-10T12:00:00Z",
             "politicaMonitoramentoVersao":"v-removida"}
            """;
    private static final TentativaMonitoramento TENTATIVA = new TentativaMonitoramento(
            "MON-1", "ORQ-1", "pre-1", "0007", 1, INICIO, INICIO.plus(Duration.ofDays(1)), "v-removida");
    private static final DecisaoProcessamento.PoliticaAplicada POLITICA =
            new DecisaoProcessamento.PoliticaAplicada("v-removida", "v1", true);

    @Inject
    MonitoramentoEntradaServiceBusMapper mapper;
    @Inject
    MonitoramentoEntradaListener listenerCdi;
    private final ServiceBusReceiverAsyncClient receiver = mock(ServiceBusReceiverAsyncClient.class);
    private final ProcessarTentativaMonitoramento processamento = mock(ProcessarTentativaMonitoramento.class);
    private final MonitoramentoReagendamentoAdapter reagendamentos = mock(MonitoramentoReagendamentoAdapter.class);
    private final ReagendarTentativaMonitoramento reagendar = mock(ReagendarTentativaMonitoramento.class);
    private Instance<ServiceBusReceiverAsyncClient> clientes;
    private MonitoramentoEntradaListener listener;
    private ServiceBusReceivedMessage mensagem;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        clientes = mock(Instance.class);
        when(clientes.get()).thenReturn(receiver);
        mensagem = mensagem();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem));
        when(receiver.complete(any())).thenReturn(Mono.empty());
        when(receiver.abandon(any())).thenReturn(Mono.empty());
        when(receiver.deadLetter(any(), any(DeadLetterOptions.class))).thenReturn(Mono.empty());
        when(processamento.executar(TENTATIVA, Long.MIN_VALUE))
                .thenReturn(Uni.createFrom().item(new DecisaoProcessamento.Ignorar()));
        when(reagendamentos.associar(any(), any())).thenReturn(reagendar);
        when(reagendar.executar(any())).thenReturn(Uni.createFrom().voidItem());
        listener = new MonitoramentoEntradaListener(clientes, mapper, processamento, reagendamentos);
    }

    @Test
    void deveResolverCdiSemInicioAutomaticoOuClienteConfigurado() {
        assertNotNull(listenerCdi);
        var falha = assertThrows(IllegalStateException.class, listenerCdi::iniciar);
        assertEquals("Falha ao iniciar consumo de monitoramento.", falha.getMessage());
        assertNull(falha.getCause());
    }

    @Test
    void deveIniciarSomenteExplicitamenteEConcluirNoOp() {
        verifyNoInteractions(clientes, receiver, processamento);
        listener.iniciar();
        verify(processamento).executar(TENTATIVA, Long.MIN_VALUE);
        verify(receiver).complete(mensagem);
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
        verify(receiver, never()).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveAguardarResultadoAntesDeCompleteEPropagarCancelamento(boolean cancelar) {
        var resposta = new CompletableFuture<DecisaoProcessamento>();
        when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().completionStage(resposta));
        listener.iniciar();
        verify(receiver, never()).complete(any());
        if (cancelar) {
            listener.encerrar(new ShutdownEvent());
            assertTrue(resposta.isCancelled());
        } else {
            resposta.complete(publicado());
            verify(receiver).complete(mensagem);
        }
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
    }

    @Test
    void deveSolicitarProximaEntregaSomenteDepoisDoSettlementDaPrimeira() {
        var segunda = mensagem();
        var primeiraConfirmacao = new CompletableFuture<Void>();
        var entregues = new AtomicInteger();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda).doOnNext(_ -> entregues.incrementAndGet()));
        when(receiver.complete(mensagem)).thenReturn(Mono.fromFuture(primeiraConfirmacao));
        listener.iniciar();
        assertEquals(1, entregues.get());
        verify(processamento).executar(TENTATIVA, Long.MIN_VALUE);
        primeiraConfirmacao.complete(null);
        assertEquals(2, entregues.get());
        verify(processamento, times(2)).executar(TENTATIVA, Long.MIN_VALUE);
        verify(receiver).complete(segunda);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{}", "schema", "envelope", "sem-corpo"})
    void deveEnviarSomenteContratoInvalidoDoMapperParaDlq(String defeito) {
        switch (defeito) {
            case "schema" -> when(mensagem.getBody()).thenReturn(BinaryData.fromString(CORPO.replace(":1,", ":2,")));
            case "envelope" -> when(mensagem.getSubject()).thenReturn("segredo-envelope");
            case "sem-corpo" -> when(mensagem.getBody()).thenReturn(null);
            default -> when(mensagem.getBody()).thenReturn(BinaryData.fromString(defeito));
        }
        listener.iniciar();
        var options = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(receiver).deadLetter(eq(mensagem), options.capture());
        assertEquals("MONITORAMENTO_ENTRADA_INVALIDA", options.getValue().getDeadLetterReason());
        assertEquals("Mensagem nao atende ao contrato de entrada.", options.getValue().getDeadLetterErrorDescription());
        assertNull(options.getValue().getPropertiesToModify());
        verifyNoInteractions(processamento);
        verify(receiver, never()).complete(any());
        verify(receiver, never()).abandon(any());
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void deveAbandonarFalhaDaPortaSemConfundirComContratoDoMapper(boolean sincrona, boolean contrato) {
        RuntimeException falha = contrato
                ? new ContratoMonitoramentoInvalidoException("segredo-id", "segredo-codigo")
                : new IllegalStateException("segredo-causa");
        when(processamento.executar(any(), anyLong())).thenAnswer(_ -> {
            if (sincrona) throw falha;
            return Uni.createFrom().failure(falha);
        });
        listener.iniciar();
        verify(receiver).abandon(mensagem);
        verify(receiver, never()).complete(any());
        verify(receiver, never()).deadLetter(any(), any());
    }

    @Test
    void deveAbandonarFalhaTecnicaAoLerEntrega() {
        when(mensagem.getBody()).thenThrow(new IllegalStateException("segredo-corpo"));
        listener.iniciar();
        verify(receiver).abandon(mensagem);
        verifyNoInteractions(processamento);
        verify(receiver, never()).deadLetter(any(), any());
    }

    @Test
    void deveAbandonarDecisaoAusenteSemCompleteFicticio() {
        when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().nullItem());
        listener.iniciar();
        verify(receiver).abandon(mensagem);
        verify(receiver, never()).complete(any());
    }

    @ParameterizedTest
    @CsvSource({"complete,false", "complete,true", "abandon,false", "abandon,true", "deadLetter,false", "deadLetter,true"})
    void falhaDeSettlementDeveEncerrarSemExecutarOutro(String acao, boolean sincrona) {
        var segunda = mensagem();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda));
        var falha = new IllegalStateException("segredo-settlement");
        switch (acao) {
            case "complete" -> when(receiver.complete(mensagem)).thenAnswer(_ -> falhar(falha, sincrona));
            case "abandon" -> {
                when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().failure(falha));
                when(receiver.abandon(mensagem)).thenAnswer(_ -> falhar(falha, sincrona));
            }
            case "deadLetter" -> {
                when(mensagem.getBody()).thenReturn(BinaryData.fromString("{"));
                when(receiver.deadLetter(eq(mensagem), any())).thenAnswer(_ -> falhar(falha, sincrona));
            }
            default -> throw new AssertionError(acao);
        }
        listener.iniciar();
        if ("complete".equals(acao)) verify(receiver).complete(mensagem);
        else verify(receiver, never()).complete(any());
        if ("abandon".equals(acao)) verify(receiver).abandon(mensagem);
        else verify(receiver, never()).abandon(any());
        if ("deadLetter".equals(acao)) verify(receiver).deadLetter(eq(mensagem), any());
        else verify(receiver, never()).deadLetter(any(), any());
        verifyNoInteractions(segunda);
        assertThrows(IllegalStateException.class, listener::iniciar);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "deadLetter"})
    void deveContinuarDepoisDeSettlementDeFalhaConfirmado(String acao) {
        var segunda = mensagem();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda));
        if ("abandon".equals(acao)) {
            when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().failure(new IllegalStateException()),
                    Uni.createFrom().item(new DecisaoProcessamento.Ignorar()));
        } else {
            when(mensagem.getBody()).thenReturn(BinaryData.fromString("{"));
        }
        listener.iniciar();
        verify(receiver).complete(segunda);
        verify(receiver, never()).complete(mensagem);
    }

    @Test
    void deveAguardarCommitDoReagendamentoAntesDaSegundaEntrega() {
        var segunda = mensagem();
        var confirmacao = new CompletableFuture<Void>();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda));
        when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().item(pendente()),
                Uni.createFrom().item(new DecisaoProcessamento.Ignorar()));
        when(reagendar.executar(any())).thenReturn(Uni.createFrom().completionStage(confirmacao));
        listener.iniciar();
        verify(reagendamentos).associar(receiver, mensagem);
        verify(reagendar).executar(ReagendamentoMonitoramento.aPartirDe(pendente()));
        verifyNoInteractions(segunda);
        verify(receiver, never()).complete(any());
        confirmacao.complete(null);
        verify(receiver).complete(segunda);
        verify(receiver, never()).complete(mensagem);
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falhaDeReagendamentoDevePararSemSettlementOuReinicio(boolean sincrona) {
        var segunda = mensagem();
        var cancelado = new AtomicBoolean();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda).doOnCancel(() -> cancelado.set(true)));
        when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().item(pendente()));
        when(reagendar.executar(any())).thenAnswer(_ -> {
            if (sincrona) throw new IllegalStateException("segredo-transacao");
            return Uni.createFrom().failure(new IllegalStateException("segredo-transacao"));
        });
        listener.iniciar();
        assertTrue(cancelado.get());
        verify(reagendamentos).associar(receiver, mensagem);
        verify(reagendar).executar(ReagendamentoMonitoramento.aPartirDe(pendente()));
        verifyNoInteractions(segunda);
        verify(receiver, never()).complete(any());
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
        assertThrows(IllegalStateException.class, listener::iniciar);
    }

    @Test
    void falhaAoAssociarEntregaNaoDeveCairNoAbandonDeProcessamento() {
        when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().item(pendente()));
        when(reagendamentos.associar(receiver, mensagem)).thenThrow(new IllegalStateException("segredo-associacao"));
        listener.iniciar();
        verifyNoInteractions(reagendar);
        verify(receiver, never()).complete(any());
        verify(receiver, never()).abandon(any());
        assertThrows(IllegalStateException.class, listener::iniciar);
    }

    @Test
    void shutdownDuranteReagendamentoNaoDeveLiquidarNovamenteNemFecharCliente() {
        var confirmacao = new CompletableFuture<Void>();
        when(processamento.executar(any(), anyLong())).thenReturn(Uni.createFrom().item(pendente()));
        when(reagendar.executar(any())).thenReturn(Uni.createFrom().completionStage(confirmacao));
        listener.iniciar();
        listener.encerrar(new ShutdownEvent());
        assertTrue(confirmacao.isCancelled());
        verify(receiver, never()).complete(any());
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).close();
    }

    private static DecisaoProcessamento.ReagendamentoPendente pendente() {
        return new DecisaoProcessamento.ReagendamentoPendente(
                TENTATIVA, 2, Duration.ofMinutes(30), INICIO, POLITICA);
    }

    @Test
    void deveCancelarNoShutdownSemFecharClienteERecusarNovoInicio() {
        var cancelado = new AtomicInteger();
        when(receiver.receiveMessages()).thenReturn(Flux.<ServiceBusReceivedMessage>never()
                .doOnCancel(cancelado::incrementAndGet));
        listener.iniciar();
        assertThrows(IllegalStateException.class, listener::iniciar);
        listener.encerrar(new ShutdownEvent());
        listener.fechar();
        assertEquals(1, cancelado.get());
        assertThrows(IllegalStateException.class, listener::iniciar);
        verify(receiver).receiveMessages();
        verify(receiver, never()).close();
    }

    @Test
    void encerramentoAntesDoInicioNaoDeveAssinarFonte() {
        listener.fechar();
        assertThrows(IllegalStateException.class, listener::iniciar);
        verifyNoInteractions(clientes, receiver);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void erroDaFonteDeveEncerrarSemNovaAssinatura(boolean sincrono) {
        if (sincrono) when(receiver.receiveMessages()).thenThrow(new IllegalStateException("segredo-fonte"));
        else when(receiver.receiveMessages()).thenReturn(Flux.error(new IllegalStateException("segredo-fonte")));
        listener.iniciar();
        assertThrows(IllegalStateException.class, listener::iniciar);
        verify(receiver).receiveMessages();
        verifyNoInteractions(processamento);
        verify(receiver, never()).abandon(any());
    }

    @Test
    void deveCancelarAssinaturaMesmoComShutdownDuranteInicio() throws Exception {
        var entrou = new java.util.concurrent.CountDownLatch(1);
        var liberar = new java.util.concurrent.CountDownLatch(1);
        var cancelamentos = new AtomicInteger();
        when(receiver.receiveMessages()).thenReturn(Flux.defer(() -> {
            entrou.countDown();
            try {
                if (!liberar.await(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    return Flux.error(new IllegalStateException("Teste nao liberou assinatura."));
                }
            } catch (InterruptedException falha) {
                Thread.currentThread().interrupt();
                return Flux.error(falha);
            }
            return Flux.<ServiceBusReceivedMessage>never().doOnCancel(cancelamentos::incrementAndGet);
        }));
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var inicio = executor.submit(listener::iniciar);
            assertTrue(entrou.await(3, java.util.concurrent.TimeUnit.SECONDS));
            var fechamento = executor.submit(listener::fechar);
            liberar.countDown();
            inicio.get(3, java.util.concurrent.TimeUnit.SECONDS);
            fechamento.get(3, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            liberar.countDown();
        }
        assertEquals(1, cancelamentos.get());
        assertThrows(IllegalStateException.class, listener::iniciar);
        verify(receiver, never()).close();
    }

    @Test
    void deveRegistrarShutdownCdiAntesDoFechamentoDaFabrica() throws ReflectiveOperationException {
        var observer = io.quarkus.arc.Arc.container().beanManager()
                .resolveObserverMethods(new ShutdownEvent()).stream()
                .filter(item -> item.getBeanClass().equals(MonitoramentoEntradaListener.class))
                .findFirst().orElseThrow();
        var prioridadeFactory = br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ClientesServiceBus.class
                .getDeclaredMethod("encerrar", ShutdownEvent.class).getParameters()[0]
                .getAnnotation(jakarta.annotation.Priority.class).value();
        assertTrue(observer.getPriority() < prioridadeFactory);
    }

    @Test
    void deveCriarOpcoesDeDlqIndependentesPorEntrega() {
        var segunda = mensagem();
        when(mensagem.getBody()).thenReturn(null);
        when(segunda.getBody()).thenReturn(null);
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda));
        listener.iniciar();
        var opcoes = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(receiver, times(2)).deadLetter(any(), opcoes.capture());
        assertNotSame(opcoes.getAllValues().getFirst(), opcoes.getAllValues().getLast());
        verifyNoInteractions(processamento);
    }

    @Test
    void deveCancelarCompletePendenteNoShutdownSemOutraLiquidacao() {
        var cancelamentos = new AtomicInteger();
        when(receiver.complete(mensagem)).thenReturn(Mono.<Void>never().doOnCancel(cancelamentos::incrementAndGet));
        listener.iniciar();
        verify(receiver).complete(mensagem);
        listener.encerrar(new ShutdownEvent());
        listener.fechar();
        assertEquals(1, cancelamentos.get());
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
        verify(receiver, never()).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveRejeitarReinicioAposConclusaoSincronaDaFonte(boolean vazia) {
        when(receiver.receiveMessages()).thenReturn(vazia ? Flux.empty() : Flux.just(mensagem));
        listener.iniciar();
        assertThrows(IllegalStateException.class, listener::iniciar);
        verify(receiver).receiveMessages();
        verify(receiver, never()).close();
    }

    private static Mono<Void> falhar(RuntimeException falha, boolean sincrona) {
        if (sincrona) throw falha;
        return Mono.error(falha);
    }

    private static DecisaoProcessamento publicado() {
        return new DecisaoProcessamento.ResultadoPublicado(new ResultadoMonitoramento(
                "MON-1", "ORQ-1", "pre-1", "0007", "CONCLUSIVO", "FINALIZADO_CONFORME",
                "CONFORME", "SITUACAO_CONCLUSIVA_MTR", 1, INICIO, INICIO, Long.MIN_VALUE), POLITICA);
    }

    private static ServiceBusReceivedMessage mensagem() {
        var entrega = mock(ServiceBusReceivedMessage.class);
        when(entrega.getBody()).thenReturn(BinaryData.fromString(CORPO));
        when(entrega.getMessageId()).thenReturn("MON-1:tentativa:1");
        when(entrega.getCorrelationId()).thenReturn("ORQ-1");
        when(entrega.getSubject()).thenReturn("MONITORAR_DOSSIE_MTR");
        when(entrega.getContentType()).thenReturn("application/json");
        when(entrega.getSequenceNumber()).thenReturn(Long.MIN_VALUE);
        return entrega;
    }
}
