package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

/** Envio e confirmacao simulados; nao usa broker nem emulador. */
@QuarkusTest
class MonitoramentoEntradaPublisherTest {

    private static final Duration ESPERA = Duration.ofSeconds(3);
    private static final TentativaMonitoramento TENTATIVA = new TentativaMonitoramento(
            "MON-1", "ORQ-1", "pre-em-analise", "0007", 1,
            Instant.parse("2026-09-08T12:00:00Z"), Instant.parse("2026-09-09T12:00:00Z"), "v1");

    @Inject
    ObjectMapper json;

    @Test
    void deveAguardarConfirmacaoECompartilhaLaSemReenviar() throws Exception {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var referencia = referencia(sender);
        var confirmacao = new CompletableFuture<Void>();
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(Mono.fromFuture(confirmacao));
        var publicacao = new MonitoramentoEntradaPublisher(referencia, json).executar(TENTATIVA);
        verifyNoInteractions(sender);
        var primeiro = publicacao.subscribeAsCompletionStage();
        var segundo = publicacao.subscribeAsCompletionStage();
        assertFalse(primeiro.toCompletableFuture().isDone());
        assertFalse(segundo.toCompletableFuture().isDone());
        confirmacao.complete(null);
        assertNull(primeiro.toCompletableFuture().get(3, java.util.concurrent.TimeUnit.SECONDS));
        assertNull(segundo.toCompletableFuture().get(3, java.util.concurrent.TimeUnit.SECONDS));
        assertNull(publicacao.await().atMost(ESPERA));
        var mensagem = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sender).sendMessage(mensagem.capture());
        var enviada = mensagem.getValue();
        assertEquals("MON-1:tentativa:1", enviada.getMessageId());
        assertEquals("ORQ-1", enviada.getCorrelationId());
        assertEquals("MONITORAR_DOSSIE_MTR", enviada.getSubject());
        assertEquals("application/json", enviada.getContentType());
        assertEquals("0007", json.readTree(enviada.getBody().toString()).path("idDossieMtr").asText());
    }

    @Test
    void devePropagarFalhaAssincronaSemDadosDoBrokerOuRetry() {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        when(sender.sendMessage(any(ServiceBusMessage.class)))
                .thenReturn(Mono.error(new IllegalStateException("namespace-e-chave-restritos")));
        var publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json).executar(TENTATIVA);
        var aguardando = publicacao.await();
        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));
        assertEquals("Falha ao publicar tentativa de monitoramento.", falha.getMessage());
        assertNull(falha.getCause());
        verify(sender).sendMessage(any(ServiceBusMessage.class));
    }

    @Test
    void deveEmitirFalhaSincronaDoSdkNoUniComDiagnosticoSeguro() {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        when(sender.sendMessage(any(ServiceBusMessage.class)))
                .thenThrow(new IllegalStateException("dado-restrito"));
        var aguardando = new MonitoramentoEntradaPublisher(referencia(sender), json).executar(TENTATIVA).await();
        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));
        assertNull(falha.getCause());
        assertEquals("Falha ao publicar tentativa de monitoramento.", falha.getMessage());
    }

    @Test
    void deveFalharExplicitamenteQuandoExtensaoEstaDesabilitada() {
        var referencia = referencia(mock(ServiceBusSenderAsyncClient.class));
        when(referencia.get()).thenThrow(new UnsatisfiedResolutionException("configuracao-indisponivel"));
        var aguardando = new MonitoramentoEntradaPublisher(referencia, json).executar(TENTATIVA).await();
        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));
        assertNull(falha.getCause());
        assertEquals("Falha ao publicar tentativa de monitoramento.", falha.getMessage());
    }

    @Test
    void devePreservarErroDeSerializacaoSemTentarEnvio() throws JsonProcessingException {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var serializer = mock(ObjectMapper.class);
        when(serializer.writeValueAsString(any())).thenThrow(new JsonProcessingException("dado-restrito") { });
        var aguardando = new MonitoramentoEntradaPublisher(referencia(sender), serializer).executar(TENTATIVA).await();
        var falha = assertThrows(SerializacaoEntradaException.class, () -> aguardando.atMost(ESPERA));
        assertEquals("ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU", falha.codigoErro());
        assertNull(falha.getCause());
        verifyNoInteractions(sender);
    }

    @SuppressWarnings("unchecked")
    private static Instance<ServiceBusSenderAsyncClient> referencia(ServiceBusSenderAsyncClient sender) {
        Instance<ServiceBusSenderAsyncClient> referencia = mock(Instance.class);
        when(referencia.get()).thenReturn(sender);
        return referencia;
    }
}
