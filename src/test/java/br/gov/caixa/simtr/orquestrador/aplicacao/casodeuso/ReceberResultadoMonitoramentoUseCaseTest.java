package br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso;

import static org.junit.jupiter.api.Assertions.*;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class ReceberResultadoMonitoramentoUseCaseTest {
    private static final Duration ESPERA = Duration.ofSeconds(3);
    private static final ResultadoMonitoramento RESULTADO = new ResultadoMonitoramento(
            "MON-1", "ORQ-1", "pre-1", "0007", "CONCLUSIVO", "FINALIZADO_CONFORME",
            "CONFORME", "SITUACAO_CONCLUSIVA_MTR", 2, Instant.EPOCH, Instant.EPOCH.plusSeconds(5), 42L);

    @Test
    void deveEsperarRegistroPreservandoResultadoSemRepetirNaMesmaInvocacao() throws Exception {
        var recebidos = new ArrayList<ResultadoMonitoramento>();
        var registro = new CompletableFuture<Void>();
        var caso = new ReceberResultadoMonitoramentoUseCase(resultado -> {
            recebidos.add(resultado);
            return Uni.createFrom().completionStage(registro);
        });

        var operacao = caso.executar(RESULTADO);
        assertTrue(recebidos.isEmpty());
        var primeiro = operacao.subscribeAsCompletionStage().toCompletableFuture();
        var segundo = operacao.subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(primeiro.isDone());
        assertFalse(segundo.isDone());
        assertEquals(List.of(RESULTADO), recebidos);
        assertSame(RESULTADO, recebidos.getFirst());

        registro.complete(null);
        assertNull(primeiro.get(3, TimeUnit.SECONDS));
        assertNull(segundo.get(3, TimeUnit.SECONDS));
        assertNull(operacao.await().atMost(ESPERA));
        assertEquals(1, recebidos.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void devePropagarFalhaDaPortaSemSucessoOuRetry(boolean sincrona) {
        var falha = new IllegalStateException("Falha controlada de registro.");
        var chamadas = new AtomicInteger();
        var caso = new ReceberResultadoMonitoramentoUseCase(_ -> {
            chamadas.incrementAndGet();
            if (sincrona) {
                throw falha;
            }
            return Uni.createFrom().failure(falha);
        });
        var operacao = caso.executar(RESULTADO);
        assertEquals(0, chamadas.get());
        var espera = operacao.await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> espera.atMost(ESPERA)));
        assertSame(falha, assertThrows(IllegalStateException.class, () -> espera.atMost(ESPERA)));
        assertEquals(1, chamadas.get());
    }

    @Test
    void deveRejeitarResultadoAusenteAntesDaPorta() {
        var chamadas = new AtomicInteger();
        var caso = new ReceberResultadoMonitoramentoUseCase(_ -> {
            chamadas.incrementAndGet();
            return Uni.createFrom().voidItem();
        });
        var espera = caso.executar(null).await();
        assertThrows(NullPointerException.class, () -> espera.atMost(ESPERA));
        assertEquals(0, chamadas.get());
    }

    @Test
    void novaEntregaPodeRegistrarNovamenteSemIdempotenciaDuravel() {
        var chamadas = new AtomicInteger();
        var caso = new ReceberResultadoMonitoramentoUseCase(_ -> {
            chamadas.incrementAndGet();
            return Uni.createFrom().voidItem();
        });
        caso.executar(RESULTADO).await().atMost(ESPERA);
        caso.executar(RESULTADO).await().atMost(ESPERA);
        assertEquals(2, chamadas.get());
    }
}
