package br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.ProcessarTentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarPreValidacao;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarSituacaoDossie;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.PublicarResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.PreValidacaoConsultada;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.CatalogoPoliticasMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramentoProgressiva;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Portas locais controladas e relógio determinístico; nenhum broker. */
@QuarkusTest
class ProcessarTentativaMonitoramentoUseCaseTest {
    private static final Instant INICIO = Instant.parse("2026-09-09T12:00:00Z");
    private static final Instant AGORA = INICIO.plusSeconds(60);
    private static final Instant LIMITE = INICIO.plus(Duration.ofHours(24));
    private static final Duration ESPERA = Duration.ofSeconds(3);
    private final ConsultarPreValidacao pre = mock(ConsultarPreValidacao.class);
    private final ConsultarSituacaoDossie hub = mock(ConsultarSituacaoDossie.class);
    private final PublicarResultadoMonitoramento publicar = mock(PublicarResultadoMonitoramento.class);
    private final Clock relogio = mock(Clock.class);
    private CatalogoPoliticasMonitoramento catalogo;
    private ProcessarTentativaMonitoramentoUseCase caso;

    @Inject
    ProcessarTentativaMonitoramento portaCdi;

    @BeforeEach
    void preparar() {
        catalogo = new CatalogoPoliticasMonitoramento(List.of());
        when(relogio.instant()).thenReturn(AGORA);
        when(pre.executar("pre-1")).thenReturn(Uni.createFrom().item(
                new PreValidacaoConsultada("EM_ANALISE_ENVIO_MTR", true)));
        when(hub.executar("0007")).thenReturn(Uni.createFrom().item(
                new SituacaoDossieConsultada(null, "EM_ANALISE")));
        when(publicar.executar(any())).thenReturn(Uni.createFrom().voidItem());
        caso = criarCaso();
    }

    @Test
    void deveConectarPortaCdiSemAtivarBroker() {
        assertNotNull(portaCdi);
        var aguardando = portaCdi.executar(null, 1L).await();
        var falha = assertThrows(NullPointerException.class, () -> aguardando.atMost(ESPERA));
        assertNotNull(falha.getMessage());
    }

    @Test
    void deveIgnorarNaoElegivelAntesDePoliticaHubOuPublicacao() {
        when(pre.executar("pre-1")).thenReturn(Uni.createFrom().item(new PreValidacaoConsultada("CONFORME", true)));
        // Versão ausente seria inválida no catálogo; o no-op deve preceder sua resolução.
        assertInstanceOf(DecisaoProcessamento.Ignorar.class, executar(tentativa(1, null)));
        verify(pre).executar("pre-1");
        verifyNoInteractions(hub, publicar, relogio);
    }

    @ParameterizedTest
    @CsvSource({"FINALIZADO_CONFORME,CONFORME", "FINALIZADO_INCONFORME,INCONFORME",
            "PENDENTE_INFORMACA,INCONFORME"})
    void devePublicarMapeamentoLiteralPreservandoDados(String mtr, String situacaoPre) {
        when(hub.executar("0007")).thenReturn(Uni.createFrom().item(new SituacaoDossieConsultada(999, mtr)));
        var decisao = assertInstanceOf(DecisaoProcessamento.ResultadoPublicado.class,
                executar(tentativa(3, "v-removida")));
        var esperado = new ResultadoMonitoramento("MON-1", "ORQ-1", "pre-1", "0007",
                "CONCLUSIVO", mtr, situacaoPre, "SITUACAO_CONCLUSIVA_MTR", 3, INICIO, AGORA, Long.MIN_VALUE);
        assertEquals(esperado, decisao.resultado());
        assertEquals(new DecisaoProcessamento.PoliticaAplicada("v-removida", "v1", true), decisao.politica());
        var ordem = inOrder(pre, hub, publicar);
        ordem.verify(pre).executar("pre-1");
        ordem.verify(hub).executar("0007");
        ordem.verify(publicar).executar(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_ANALISE", "CONFORME", "NAO_CONFORME", "PENDENTE_INFORMACAO", "finalizado_conforme"})
    void deveTratarOutrosLiteraisComoNaoConclusivos(String mtr) {
        when(hub.executar("0007")).thenReturn(Uni.createFrom().item(new SituacaoDossieConsultada(null, mtr)));
        var original = tentativa(100, "v-removida");
        var decisao = assertInstanceOf(DecisaoProcessamento.ReagendamentoPendente.class, executar(original));
        assertSame(original, decisao.tentativa());
        assertEquals(101, decisao.proximaTentativa());
        assertEquals(Duration.ofMinutes(30), decisao.intervalo());
        assertEquals(AGORA, decisao.processadoEm());
        assertEquals(new DecisaoProcessamento.PoliticaAplicada("v-removida", "v1", true), decisao.politica());
        verifyNoInteractions(publicar);
    }

    @ParameterizedTest
    @CsvSource({"1,3", "2,4", "3,6", "4,6", "100,6"})
    void deveUsarDefinicaoRecebidaSemDependerDaAtiva(int atual, int horas) {
        configurar("v-antiga", OptionalInt.empty());
        var decisao = assertInstanceOf(DecisaoProcessamento.ReagendamentoPendente.class,
                executar(tentativa(atual, "v-antiga")));
        assertEquals(Duration.ofHours(horas), decisao.intervalo());
        assertEquals(new DecisaoProcessamento.PoliticaAplicada("v-antiga", "v-antiga", false), decisao.politica());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void deveEncerrarPrazoAntesDoHubSemRenovarJanela(int segundosApos) {
        var limiteRecebido = INICIO.plus(Duration.ofHours(2));
        var conclusao = limiteRecebido.plusSeconds(segundosApos);
        when(relogio.instant()).thenReturn(conclusao);
        var original = new TentativaMonitoramento("MON-1", "ORQ-1", "pre-1", "0007",
                1, INICIO, limiteRecebido, "v-removida");
        var decisao = assertInstanceOf(DecisaoProcessamento.ResultadoPublicado.class, executar(original));
        assertEquals(new ResultadoMonitoramento("MON-1", "ORQ-1", "pre-1", "0007",
                "QUARENTENA", null, "QUARENTENA", "PRAZO_MAXIMO", 0, INICIO, conclusao, Long.MIN_VALUE),
                decisao.resultado());
        assertEquals(new DecisaoProcessamento.PoliticaAplicada("v-removida", "v1", true), decisao.politica());
        verifyNoInteractions(hub);
    }

    @Test
    void devePermitirPrimeiraConsultaComMaximoUmEEncerrarAposNaoConclusivo() {
        configurar("v-unica", OptionalInt.of(1));
        var resultado = resultado(executar(tentativa(1, "v-unica")));
        assertEquals("MAXIMO_TENTATIVAS", resultado.motivo());
        assertEquals(1, resultado.tentativasRealizadas());
        assertEquals("EM_ANALISE", resultado.situacaoMtr());
        verify(hub).executar("0007");
    }

    @Test
    void deveEncerrarMaximoAntesDoHubQuandoTentativasJaForamRealizadas() {
        configurar("v-unica", OptionalInt.of(1));
        var resultado = resultado(executar(tentativa(2, "v-unica")));
        assertEquals("MAXIMO_TENTATIVAS", resultado.motivo());
        assertEquals(1, resultado.tentativasRealizadas());
        assertNull(resultado.situacaoMtr());
        verifyNoInteractions(hub);
    }

    @Test
    void devePriorizarPrazoQuandoAmbosLimitesEsgotaram() {
        configurar("v-unica", OptionalInt.of(1));
        when(relogio.instant()).thenReturn(LIMITE);
        assertEquals("PRAZO_MAXIMO", resultado(executar(tentativa(2, "v-unica"))).motivo());
        verifyNoInteractions(hub);
    }

    @Test
    void deveReavaliarPrazoAposRespostaNaoConclusiva() {
        when(relogio.instant()).thenReturn(AGORA, LIMITE);
        var resultado = resultado(executar(tentativa(1, "v1")));
        assertEquals("PRAZO_MAXIMO", resultado.motivo());
        assertEquals("EM_ANALISE", resultado.situacaoMtr());
        assertEquals(1, resultado.tentativasRealizadas());
        assertEquals(LIMITE, resultado.concluidoEm());
    }

    @Test
    void devePreservarConclusaoQuandoPrazoVenceDuranteConsulta() {
        when(relogio.instant()).thenReturn(AGORA, LIMITE);
        when(hub.executar("0007")).thenReturn(Uni.createFrom().item(
                new SituacaoDossieConsultada(null, "FINALIZADO_CONFORME")));
        var resultado = resultado(executar(tentativa(1, "v1")));
        assertEquals("CONCLUSIVO", resultado.resultadoMonitoramento());
        assertEquals(LIMITE, resultado.concluidoEm());
    }

    @Test
    void deveAguardarConfirmacaoECompartilharEfeitosDaMesmaInvocacao() {
        when(hub.executar("0007")).thenReturn(Uni.createFrom().item(
                new SituacaoDossieConsultada(null, "FINALIZADO_CONFORME")));
        var confirmacao = new CompletableFuture<Void>();
        when(publicar.executar(any())).thenReturn(Uni.createFrom().completionStage(confirmacao));
        var operacao = caso.executar(tentativa(1, "v1"), Long.MAX_VALUE);
        verifyNoInteractions(pre, hub, publicar, relogio);
        var primeira = operacao.subscribeAsCompletionStage().toCompletableFuture();
        var segunda = operacao.subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(primeira.isDone());
        assertFalse(segunda.isDone());
        confirmacao.complete(null);
        assertSame(primeira.join(), segunda.join());
        assertEquals(Long.MAX_VALUE, resultado(primeira.join()).inputSequenceNumber());
        verify(pre).executar("pre-1");
        verify(hub).executar("0007");
        verify(publicar).executar(any());
    }

    @ParameterizedTest
    @CsvSource({"pre,false", "pre,true", "hub,false", "hub,true", "publicar,false", "publicar,true"})
    void devePropagarFalhasSemRepetirEfeitos(String etapa, boolean sincrona) {
        var falha = new IllegalStateException("falha controlada");
        if ("pre".equals(etapa)) {
            when(pre.executar("pre-1")).thenAnswer(inv -> falhar(falha, sincrona));
        } else if ("hub".equals(etapa)) {
            when(hub.executar("0007")).thenAnswer(inv -> falhar(falha, sincrona));
        } else {
            when(hub.executar("0007")).thenReturn(Uni.createFrom().item(
                    new SituacaoDossieConsultada(null, "FINALIZADO_CONFORME")));
            when(publicar.executar(any())).thenAnswer(inv -> falhar(falha, sincrona));
        }
        var aguardando = caso.executar(tentativa(1, "v1"), 42L).await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        verify(pre).executar("pre-1");
        if ("pre".equals(etapa)) {
            verifyNoInteractions(hub, publicar);
        } else {
            verify(hub).executar("0007");
            if ("hub".equals(etapa)) verifyNoInteractions(publicar);
            else verify(publicar).executar(any());
        }
    }

    @Test
    void deveManterInvocacoesIndependentesParaRedelivery() {
        executar(tentativa(1, "v1"));
        executar(tentativa(1, "v1"));
        verify(pre, times(2)).executar("pre-1");
        verify(hub, times(2)).executar("0007");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveRecusarContadorInvalidoSemConsultarFontes(int atual) {
        var aguardando = caso.executar(tentativa(atual, "v1"), 1L).await();
        assertThrows(IllegalArgumentException.class, () -> aguardando.atMost(ESPERA));
        verifyNoInteractions(pre, hub, publicar);
    }

    @ParameterizedTest
    @CsvSource({"EM_ANALISE,QUARENTENA,MAXIMO_TENTATIVAS",
            "FINALIZADO_CONFORME,CONCLUSIVO,SITUACAO_CONCLUSIVA_MTR"})
    void deveEncerrarUltimoInteiroSemIncrementarOuPerderConclusao(String mtr, String tipo, String motivo) {
        when(hub.executar("0007")).thenReturn(Uni.createFrom().item(new SituacaoDossieConsultada(null, mtr)));
        var encerrado = resultado(executar(tentativa(Integer.MAX_VALUE, "v-removida")));
        assertEquals(tipo, encerrado.resultadoMonitoramento());
        assertEquals(motivo, encerrado.motivo());
        assertEquals(Integer.MAX_VALUE, encerrado.tentativasRealizadas());
        assertEquals(mtr, encerrado.situacaoMtr());
        verify(hub).executar("0007");
        verify(publicar).executar(encerrado);
    }

    private static Uni<?> falhar(RuntimeException falha, boolean sincrona) {
        if (sincrona) throw falha;
        return Uni.createFrom().failure(falha);
    }

    private void configurar(String versao, OptionalInt maximo) {
        catalogo = new CatalogoPoliticasMonitoramento(List.of(new PoliticaMonitoramentoProgressiva(
                versao, List.of(Duration.ofHours(3), Duration.ofHours(4), Duration.ofHours(6)),
                maximo, Duration.ofHours(7))));
        caso = criarCaso();
    }

    private ProcessarTentativaMonitoramentoUseCase criarCaso() {
        return new ProcessarTentativaMonitoramentoUseCase(pre, hub, publicar, catalogo, relogio);
    }

    private DecisaoProcessamento executar(TentativaMonitoramento tentativa) {
        return caso.executar(tentativa, Long.MIN_VALUE).await().atMost(ESPERA);
    }

    private static ResultadoMonitoramento resultado(DecisaoProcessamento decisao) {
        return assertInstanceOf(DecisaoProcessamento.ResultadoPublicado.class, decisao).resultado();
    }

    private static TentativaMonitoramento tentativa(int atual, String versao) {
        return new TentativaMonitoramento("MON-1", "ORQ-1", "pre-1", "0007", atual, INICIO, LIMITE, versao);
    }
}
