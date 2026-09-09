package br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.PublicarTentativaMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

@QuarkusTest
class IniciarMonitoramentoUseCaseTest {

    private static final Instant INICIO = Instant.parse("2026-09-08T12:00:00.123456789Z");
    private static final Duration ESPERA = Duration.ofSeconds(3);
    private static final SolicitacaoMonitoramento SOLICITACAO = new SolicitacaoMonitoramento("pre-em-analise", "0007");
    private static final Clock RELOGIO = Clock.fixed(INICIO, ZoneOffset.UTC);

    @Test
    void devePrepararPublicarEAguardarConfirmacaoPreservandoDados() throws Exception {
        var enviados = new ArrayList<TentativaMonitoramento>();
        var ordem = new ArrayList<String>();
        var confirmacao = new CompletableFuture<Void>();
        var caso = new IniciarMonitoramentoUseCase(instante -> {
            ordem.add("parametros");
            assertEquals(INICIO, instante);
            return new ParametrosMonitoramento(INICIO.plusSeconds(731), "politica-configurada");
        }, tentativa -> {
            ordem.add("publicacao");
            enviados.add(tentativa);
            return Uni.createFrom().completionStage(confirmacao);
        }, RELOGIO);

        var inicio = caso.executar(SOLICITACAO);
        assertEquals(0, ordem.size());
        var primeiro = inicio.subscribeAsCompletionStage().toCompletableFuture();
        var segundo = inicio.subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(primeiro.isDone());
        assertFalse(segundo.isDone());
        confirmacao.complete(null);
        var resultado = primeiro.get(3, TimeUnit.SECONDS);
        assertEquals(resultado, segundo.get(3, TimeUnit.SECONDS));
        assertEquals(resultado, inicio.await().atMost(ESPERA));
        assertEquals(java.util.List.of("parametros", "publicacao"), ordem);
        assertEquals(1, enviados.size());
        var tentativa = enviados.getFirst();
        assertEquals(new MonitoramentoIniciado(tentativa.monitoramentoId(), tentativa.orquestracaoId()), resultado);
        assertNotNull(UUID.fromString(resultado.monitoramentoId()));
        assertNotNull(UUID.fromString(resultado.orquestracaoId()));
        assertNotEquals(resultado.monitoramentoId(), resultado.orquestracaoId());
        assertEquals("pre-em-analise", tentativa.idDossiePreValidacao());
        assertEquals("0007", tentativa.idDossieMtr());
        assertEquals(1, tentativa.tentativaAtual());
        assertEquals(INICIO, tentativa.iniciadoEm());
        assertEquals(INICIO.plusSeconds(731), tentativa.limiteEm());
        assertEquals("politica-configurada", tentativa.politicaMonitoramentoVersao());
    }

    @Test
    void deveGerarIdentidadesIndependentesParaSolicitacoesDistintas() {
        var caso = new IniciarMonitoramentoUseCase(parametros(),
                _ -> Uni.createFrom().voidItem(), RELOGIO);
        var primeiro = caso.executar(SOLICITACAO).await().atMost(ESPERA);
        var segundo = caso.executar(SOLICITACAO).await().atMost(ESPERA);
        assertNotEquals(primeiro.monitoramentoId(), segundo.monitoramentoId());
        assertNotEquals(primeiro.orquestracaoId(), segundo.orquestracaoId());
    }

    @Test
    void devePropagarFalhaDePreparacaoSemPublicar() {
        var falha = new IllegalStateException("Falha sintetica de preparacao.");
        var chamadas = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(_ -> { throw falha; },
                _ -> { chamadas.incrementAndGet(); return Uni.createFrom().voidItem(); }, RELOGIO);
        var aguardando = caso.executar(SOLICITACAO).await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertEquals(0, chamadas.get());
    }

    @Test
    void devePropagarFalhaAssincronaSemRetornoDeSucessoOuRetry() {
        var falha = new IllegalStateException("Falha sintetica de publicacao.");
        var chamadas = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(parametros(), _ -> {
            chamadas.incrementAndGet();
            return Uni.createFrom().failure(falha);
        }, RELOGIO);
        var aguardando = caso.executar(SOLICITACAO).await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertEquals(1, chamadas.get());
    }

    @Test
    void deveEmitirFalhaSincronaDaPortaNoFluxo() {
        var falha = new IllegalStateException("Falha sintetica sincrona.");
        PublicarTentativaMonitoramento publicar = _ -> { throw falha; };
        var caso = new IniciarMonitoramentoUseCase(parametros(), publicar, RELOGIO);
        var aguardando = caso.executar(SOLICITACAO).await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
    }

    @Test
    void deveRejeitarSolicitacaoAusenteAntesDePrepararOuPublicar() {
        var chamadas = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(_ -> {
            chamadas.incrementAndGet();
            return new ParametrosMonitoramento(INICIO.plusSeconds(60), "v1");
        }, _ -> { chamadas.incrementAndGet(); return Uni.createFrom().voidItem(); }, RELOGIO);
        var aguardando = caso.executar(null).await();
        assertThrows(NullPointerException.class, () -> aguardando.atMost(ESPERA));
        assertEquals(0, chamadas.get());
    }

    private static ObterParametrosMonitoramento parametros() {
        return iniciadoEm -> new ParametrosMonitoramento(iniciadoEm.plusSeconds(60), "v1");
    }
}
