package br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.ReceberResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@QuarkusTest
@TestProfile(MonitoramentoResultadoListenerTest.LogProfile.class)
class MonitoramentoResultadoListenerTest {
    private static final Path ARQUIVO = Path.of("target/logs/listener-resultado-test.json");
    private static final String CORPO = """
            {"schemaVersion":1,"monitoramentoId":"MON-1","orquestracaoId":"ORQ-1",
             "idDossiePreValidacao":"pre-1","idDossieMtr":"0007","resultadoMonitoramento":"CONCLUSIVO",
             "situacaoMtr":"FINALIZADO_CONFORME","situacaoPreValidacao":"CONFORME",
             "motivo":"SITUACAO_CONCLUSIVA_MTR","tentativasRealizadas":2,
             "iniciadoEm":"2026-09-10T12:00:00Z","concluidoEm":"2026-09-10T12:01:00Z",
             "inputSequenceNumber":42}
            """;
    private static final ResultadoMonitoramento RESULTADO = new ResultadoMonitoramento(
            "MON-1", "ORQ-1", "pre-1", "0007", "CONCLUSIVO", "FINALIZADO_CONFORME", "CONFORME",
            "SITUACAO_CONCLUSIVA_MTR", 2, Instant.parse("2026-09-10T12:00:00Z"),
            Instant.parse("2026-09-10T12:01:00Z"), 42L);

    @Inject MonitoramentoResultadoServiceBusMapper mapper;
    @Inject MonitoramentoResultadoListener listenerCdi;
    @Inject ObjectMapper json;
    private final ServiceBusReceiverAsyncClient receiver = mock(ServiceBusReceiverAsyncClient.class);
    private final ReceberResultadoMonitoramento recebimento = mock(ReceberResultadoMonitoramento.class);
    private Instance<ServiceBusReceiverAsyncClient> clientes;
    private MonitoramentoResultadoListener listener;
    private ServiceBusReceivedMessage mensagem;

    public static class LogProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.log.file.enabled", "true", "quarkus.log.file.path", ARQUIVO.toString(),
                    "quarkus.log.file.json.enabled", "true", "quarkus.log.console.json.enabled", "true");
        }
    }

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
        when(recebimento.executar(any())).thenReturn(Uni.createFrom().voidItem());
        listener = new MonitoramentoResultadoListener(clientes, mapper, recebimento);
    }

    @AfterEach
    void encerrarTeste() {
        listener.fechar();
    }

    @Test
    void deveResolverCdiSemIniciarConsumoNoPerfilPadrao() {
        var falha = assertThrows(IllegalStateException.class, listenerCdi::iniciar);
        assertEquals("Falha ao iniciar consumo de resultados.", falha.getMessage());
        assertNull(falha.getCause());
    }

    @Test
    void deveManterStartupDesabilitadoSemAcessarCliente() {
        listener.iniciarNoStartup(new StartupEvent(), false);
        verifyNoInteractions(clientes, receiver, recebimento);
    }

    @Test
    void deveAtivarUmaVezNoStartupEConcluirResultadoValido() {
        var evento = new StartupEvent();
        listener.iniciarNoStartup(evento, true);
        verify(recebimento).executar(RESULTADO);
        verify(receiver).complete(mensagem);
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
        assertThrows(IllegalStateException.class, () -> listener.iniciarNoStartup(evento, true));
        verify(receiver).receiveMessages();
        verify(receiver, never()).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveAguardarPortaAntesDeCompleteOuCancelarNoShutdown(boolean cancelar) {
        var resposta = new CompletableFuture<Void>();
        when(recebimento.executar(any())).thenReturn(Uni.createFrom().completionStage(resposta));
        listener.iniciar();
        verify(receiver, never()).complete(any());
        if (cancelar) {
            listener.encerrar(new ShutdownEvent());
            assertTrue(resposta.isCancelled());
        } else {
            resposta.complete(null);
            verify(receiver).complete(mensagem);
        }
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
    }

    @Test
    void deveSolicitarSegundaEntregaSomenteAposSettlementDaPrimeira() {
        var segunda = mensagem();
        var confirmacao = new CompletableFuture<Void>();
        var entregues = new AtomicInteger();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda)
                .doOnNext(_ -> entregues.incrementAndGet()));
        when(receiver.complete(mensagem)).thenReturn(Mono.fromFuture(confirmacao));
        listener.iniciar();
        assertEquals(1, entregues.get());
        verify(recebimento).executar(RESULTADO);
        confirmacao.complete(null);
        assertEquals(2, entregues.get());
        verify(recebimento, times(2)).executar(RESULTADO);
        verify(receiver).complete(segunda);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{}", "schema", "envelope", "sem-corpo"})
    void deveEnviarContratoInvalidoParaDlqSemChamarPorta(String defeito) {
        switch (defeito) {
            case "schema" -> when(mensagem.getBody()).thenReturn(BinaryData.fromString(CORPO.replace(":1,", ":2,")));
            case "envelope" -> when(mensagem.getSubject()).thenReturn("segredo-envelope");
            case "sem-corpo" -> when(mensagem.getBody()).thenReturn(null);
            default -> when(mensagem.getBody()).thenReturn(BinaryData.fromString(defeito));
        }
        listener.iniciar();
        var options = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(receiver).deadLetter(eq(mensagem), options.capture());
        assertEquals("MONITORAMENTO_SAIDA_INVALIDA", options.getValue().getDeadLetterReason());
        assertEquals("Mensagem nao atende ao contrato de resultado.", options.getValue().getDeadLetterErrorDescription());
        assertNull(options.getValue().getPropertiesToModify());
        verifyNoInteractions(recebimento);
        verify(receiver, never()).complete(any());
        verify(receiver, never()).abandon(any());
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void deveAbandonarFalhaDaPortaMesmoSeExcecaoForDoTipoDoMapper(boolean sincrona, boolean contrato) {
        RuntimeException falha = contrato
                ? new MapeamentoResultadoException("id-sintetico", "codigo-sintetico", "segredo-porta")
                : new IllegalStateException("segredo-porta");
        when(recebimento.executar(any())).thenAnswer(_ -> {
            if (sincrona) throw falha;
            return Uni.createFrom().failure(falha);
        });
        listener.iniciar();
        verify(receiver).abandon(mensagem);
        verify(receiver, never()).complete(any());
        verify(receiver, never()).deadLetter(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveAbandonarFalhaTecnicaDeLeituraOuPortaAusente(boolean portaAusente) {
        if (portaAusente) when(recebimento.executar(any())).thenReturn(null);
        else when(mensagem.getBody()).thenThrow(new IllegalStateException("segredo-corpo"));
        listener.iniciar();
        verify(receiver).abandon(mensagem);
        verify(receiver, never()).complete(any());
        verify(receiver, never()).deadLetter(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"complete,false", "complete,true", "abandon,false", "abandon,true", "deadLetter,false", "deadLetter,true"})
    void deveEncerrarAposFalhaDeSettlementSemSegundoSettlement(String acao, boolean sincrona) {
        var segunda = mensagem();
        var cancelado = new AtomicInteger();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda)
                .doOnCancel(cancelado::incrementAndGet));
        var falha = new IllegalStateException("segredo-settlement");
        switch (acao) {
            case "complete" -> when(receiver.complete(mensagem)).thenAnswer(_ -> falhar(falha, sincrona));
            case "abandon" -> {
                when(recebimento.executar(any())).thenReturn(Uni.createFrom().failure(falha));
                when(receiver.abandon(mensagem)).thenAnswer(_ -> falhar(falha, sincrona));
            }
            case "deadLetter" -> {
                when(mensagem.getBody()).thenReturn(null);
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
        assertEquals(1, cancelado.get());
        verifyNoInteractions(segunda);
        assertThrows(IllegalStateException.class, listener::iniciar);
        verify(receiver, never()).close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abandon", "deadLetter"})
    void deveContinuarAposFalhaLiquidadaSemAlterarResultadoNaRedelivery(String acao) {
        var segunda = mensagem();
        when(receiver.receiveMessages()).thenReturn(Flux.just(mensagem, segunda));
        if ("abandon".equals(acao)) {
            when(recebimento.executar(any())).thenReturn(Uni.createFrom().failure(new IllegalStateException()),
                    Uni.createFrom().voidItem());
        } else {
            when(mensagem.getBody()).thenReturn(null);
        }
        listener.iniciar();
        verify(receiver).complete(segunda);
        verify(receiver, never()).complete(mensagem);
        verify(recebimento, times("abandon".equals(acao) ? 2 : 1)).executar(RESULTADO);
    }

    @Test
    void deveRegistrarErroTecnicoSemDadosDaExcecaoOuPayload() throws Exception {
        int primeiraLinha = Files.readAllLines(ARQUIVO).size();
        when(recebimento.executar(any())).thenReturn(Uni.createFrom().failure(
                new IllegalStateException("credencial-sintetica-sigilosa")));
        listener.iniciar();
        var linhas = Files.readAllLines(ARQUIVO);
        int encontrados = 0;
        for (var linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registroJson = json.readTree(linha);
            if (!"orquestrador.monitoramento-dossie.resultado.falhou".equals(registroJson.path("evento").asText())) continue;
            encontrados++;
            assertEquals("ERROR", registroJson.path("level").asText());
            assertEquals("FALHA_TECNICA", registroJson.path("error_type").asText());
            assertEquals("receber", registroJson.path("operacao").asText());
            assertFalse(linha.contains("credencial-sintetica-sigilosa"));
            assertFalse(linha.contains("pre-1"));
            assertFalse(registroJson.has("exception"));
            assertFalse(registroJson.has("stacktrace"));
        }
        assertEquals(1, encontrados);
    }

    @Test
    void devePropagarFalhaSanitizadaAoAtivarSemCliente() {
        when(clientes.get()).thenThrow(new IllegalStateException("segredo-cliente"));
        var evento = new StartupEvent();
        var falha = assertThrows(IllegalStateException.class, () -> listener.iniciarNoStartup(evento, true));
        assertEquals("Falha ao iniciar consumo de resultados.", falha.getMessage());
        assertNull(falha.getCause());
        assertThrows(IllegalStateException.class, listener::iniciar);
        verifyNoInteractions(receiver, recebimento);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveEncerrarAposFalhaDaFonteSemReassinar(boolean sincrona) {
        var falha = new IllegalStateException("segredo-fonte");
        if (sincrona) when(receiver.receiveMessages()).thenThrow(falha);
        else when(receiver.receiveMessages()).thenReturn(Flux.error(falha));
        listener.iniciar();
        verify(receiver).receiveMessages();
        assertThrows(IllegalStateException.class, listener::iniciar);
        verifyNoInteractions(recebimento);
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).close();
    }

    @Test
    void deveCancelarRecepcaoUmaVezSemFecharCliente() {
        var cancelamentos = new AtomicInteger();
        when(receiver.receiveMessages()).thenReturn(Flux.<ServiceBusReceivedMessage>never()
                .doOnCancel(cancelamentos::incrementAndGet));
        listener.iniciar();
        assertThrows(IllegalStateException.class, listener::iniciar);
        listener.encerrar(new ShutdownEvent());
        listener.fechar();
        assertEquals(1, cancelamentos.get());
        assertThrows(IllegalStateException.class, listener::iniciar);
        verify(receiver).receiveMessages();
        verify(receiver, never()).close();
    }

    @Test
    void deveImpedirInicioDepoisDeEncerrado() {
        listener.fechar();
        assertThrows(IllegalStateException.class, listener::iniciar);
        verifyNoInteractions(clientes, receiver, recebimento);
    }

    @Test
    void deveCancelarSettlementPendenteSemOutraLiquidacao() {
        var cancelamentos = new AtomicInteger();
        when(receiver.complete(mensagem)).thenReturn(Mono.<Void>never()
                .doOnCancel(cancelamentos::incrementAndGet));
        listener.iniciar();
        listener.encerrar(new ShutdownEvent());
        listener.fechar();
        assertEquals(1, cancelamentos.get());
        verify(receiver).complete(mensagem);
        verify(receiver, never()).abandon(any());
        verify(receiver, never()).deadLetter(any(), any());
        verify(receiver, never()).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveRecusarReinicioAposConclusaoSincronaDaFonte(boolean vazia) {
        when(receiver.receiveMessages()).thenReturn(vazia ? Flux.empty() : Flux.just(mensagem));
        listener.iniciar();
        assertThrows(IllegalStateException.class, listener::iniciar);
        verify(receiver).receiveMessages();
    }

    @Test
    void deveCancelarAssinaturaQuandoInicioDisputaComShutdown() throws Exception {
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
        verify(receiver, never()).close();
        assertThrows(IllegalStateException.class, listener::iniciar);
    }

    @Test
    void deveRegistrarShutdownAntesDoFechamentoDosClientes() throws ReflectiveOperationException {
        var observer = io.quarkus.arc.Arc.container().beanManager()
                .resolveObserverMethods(new ShutdownEvent()).stream()
                .filter(item -> item.getBeanClass().equals(MonitoramentoResultadoListener.class))
                .findFirst().orElseThrow();
        var prioridadeFactory = br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ClientesServiceBus.class
                .getDeclaredMethod("encerrar", ShutdownEvent.class).getParameters()[0]
                .getAnnotation(jakarta.annotation.Priority.class).value();
        assertTrue(observer.getPriority() < prioridadeFactory);
    }

    private static Mono<Void> falhar(RuntimeException falha, boolean sincrona) {
        if (sincrona) throw falha;
        return Mono.error(falha);
    }

    private static ServiceBusReceivedMessage mensagem() {
        var entrega = mock(ServiceBusReceivedMessage.class);
        when(entrega.getBody()).thenReturn(BinaryData.fromString(CORPO));
        when(entrega.getMessageId()).thenReturn("MON-1:resultado:v1");
        when(entrega.getCorrelationId()).thenReturn("ORQ-1");
        when(entrega.getSubject()).thenReturn("RESULTADO_MONITORAMENTO_DOSSIE_MTR");
        when(entrega.getContentType()).thenReturn("application/json");
        return entrega;
    }
}
