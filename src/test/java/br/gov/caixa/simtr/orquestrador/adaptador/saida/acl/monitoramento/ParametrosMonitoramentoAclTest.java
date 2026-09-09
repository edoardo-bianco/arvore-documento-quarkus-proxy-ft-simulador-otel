package br.gov.caixa.simtr.orquestrador.adaptador.saida.acl.monitoramento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ParametrosMonitoramento;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ParametrosMonitoramentoAclTest {

    @Inject
    ObterParametrosMonitoramento parametros;

    @Test
    void deveTraduzirSomenteCamposPublicosPreservandoInstanteEVersao() {
        var inicio = Instant.parse("2026-09-08T12:00:00.123456789Z");
        var limite = inicio.plusSeconds(137);
        var recebido = new AtomicReference<Instant>();
        var chamadas = new AtomicInteger();
        var acl = new ParametrosMonitoramentoAcl(instante -> {
            recebido.set(instante);
            chamadas.incrementAndGet();
            return new br.gov.caixa.simtr.monitoramento.dominio.modelo.ParametrosMonitoramento(
                    limite, "versao-distinta");
        });

        assertEquals(new ParametrosMonitoramento(limite, "versao-distinta"), acl.executar(inicio));
        assertSame(inicio, recebido.get());
        assertEquals(1, chamadas.get());
    }

    @Test
    void devePropagarFalhaDaPortaSemFallbackOuNovaTentativa() {
        var falha = new IllegalStateException("Falha sintetica de preparacao.");
        var chamadas = new AtomicInteger();
        var acl = new ParametrosMonitoramentoAcl(_ -> {
            chamadas.incrementAndGet();
            throw falha;
        });
        assertSame(falha, assertThrows(IllegalStateException.class, () -> acl.executar(Instant.EPOCH)));
        assertEquals(1, chamadas.get());
    }

    @Test
    void deveResolverColaboracaoRealPorCdiSemBroker() {
        assertEquals(new ParametrosMonitoramento(Instant.EPOCH.plus(Duration.ofHours(24)), "v1"),
                parametros.executar(Instant.EPOCH));
    }
}
