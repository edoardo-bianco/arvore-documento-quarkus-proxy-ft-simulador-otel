package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Confirmacao controlada em memoria; nao inicializa Quarkus nem broker. */
class MonitoramentoResultadoPublisherTest {

    private static final Duration ESPERA = Duration.ofSeconds(3);
    private final ServiceBusSenderAsyncClient sender = mock(ServiceBusSenderAsyncClient.class);
    private final MonitoramentoResultadoServiceBusMapper mapper = mock(MonitoramentoResultadoServiceBusMapper.class);
    private final ServiceBusMessage mensagem = new ServiceBusMessage("resultado-sintetico");

    @Test
    void deveEsperarConfirmacaoECompartilharEnvioEntreAssinaturas() throws Exception {
        var confirmacao = new CompletableFuture<Void>();
        when(mapper.paraMensagem(ResultadoFixture.resultado())).thenReturn(mensagem);
        when(sender.sendMessage(mensagem)).thenReturn(Mono.fromFuture(confirmacao));
        var publicacao = publisher().executar(ResultadoFixture.resultado());
        verifyNoInteractions(mapper, sender);

        var primeiro = publicacao.subscribeAsCompletionStage().toCompletableFuture();
        var segundo = publicacao.subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(primeiro.isDone());
        assertFalse(segundo.isDone());
        confirmacao.complete(null);

        assertNull(primeiro.get(3, TimeUnit.SECONDS));
        assertNull(segundo.get(3, TimeUnit.SECONDS));
        assertNull(publicacao.await().atMost(ESPERA));
        verify(mapper).paraMensagem(ResultadoFixture.resultado());
        verify(sender).sendMessage(mensagem);
    }

    @Test
    void devePropagarFalhaAssincronaSemSegredoCausaOuReenvio() {
        prepararMensagem();
        when(sender.sendMessage(mensagem)).thenReturn(Mono.error(new IllegalStateException("segredo-sintetico")));
        var publicacao = publisher().executar(ResultadoFixture.resultado());

        assertFalhaSegura(publicacao);
        assertFalhaSegura(publicacao);
        verify(sender).sendMessage(mensagem);
    }

    @Test
    void devePropagarFalhaSincronaDoSdkNoUni() {
        prepararMensagem();
        when(sender.sendMessage(mensagem)).thenThrow(new IllegalStateException("segredo-sintetico"));

        assertFalhaSegura(publisher().executar(ResultadoFixture.resultado()));
    }

    @Test
    void deveFalharQuandoClienteNaoEstaDisponivel() {
        prepararMensagem();
        var referencia = referencia();
        when(referencia.get()).thenThrow(new UnsatisfiedResolutionException("configuracao-restrita"));

        assertFalhaSegura(new MonitoramentoResultadoPublisher(referencia, mapper)
                .executar(ResultadoFixture.resultado()));
        verifyNoInteractions(sender);
    }

    @Test
    void devePreservarFalhaTipadaDoMapperSemTentarEnviar() {
        var esperada = new MapeamentoResultadoException("erro-1", "CONTRATO_INVALIDO", "Contrato invalido.");
        when(mapper.paraMensagem(any())).thenThrow(esperada);
        var aguardando = publisher().executar(ResultadoFixture.resultado()).await();

        assertSame(esperada, assertThrows(MapeamentoResultadoException.class, () -> aguardando.atMost(ESPERA)));
        verifyNoInteractions(sender);
    }

    @Test
    void invocacoesIndependentesDevemPoderPublicarNovamente() {
        prepararMensagem();
        when(sender.sendMessage(mensagem)).thenReturn(Mono.empty());
        var publisher = publisher();

        assertNull(publisher.executar(ResultadoFixture.resultado()).await().atMost(ESPERA));
        assertNull(publisher.executar(ResultadoFixture.resultado()).await().atMost(ESPERA));
        verify(sender, times(2)).sendMessage(mensagem);
    }

    private void prepararMensagem() {
        when(mapper.paraMensagem(any())).thenReturn(mensagem);
    }

    private MonitoramentoResultadoPublisher publisher() {
        return new MonitoramentoResultadoPublisher(referencia(), mapper);
    }

    @SuppressWarnings("unchecked")
    private Instance<ServiceBusSenderAsyncClient> referencia() {
        Instance<ServiceBusSenderAsyncClient> referencia = mock(Instance.class);
        when(referencia.get()).thenReturn(sender);
        return referencia;
    }

    private static void assertFalhaSegura(Uni<Void> publicacao) {
        var aguardando = publicacao.await();
        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));
        assertEquals("Falha ao publicar resultado de monitoramento.", falha.getMessage());
        assertNull(falha.getCause());
        assertEquals(0, falha.getSuppressed().length);
    }
}
