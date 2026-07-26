package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AnaliseConformidadeMemoryStoreTest {

    private static final String CORRELATION_ID = "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String INSTANCE_ID = "instancia-analise-1";
    private static final String IDENTIFICADOR_DOCUMENTO = "DOC-2026-000123";

    private final AnaliseConformidadeMemoryStore store = new AnaliseConformidadeMemoryStore();

    @Test
    void percorreSomenteASequenciaCompletaAteConclusao() {
        ResultadoAnaliseConformidade preliminar = resultado(OrigemResultado.AGENTE);
        ResultadoAnaliseConformidade finalizado = resultado(OrigemResultado.REVISAO_HUMANA);

        iniciar();
        assertEquals(
                StatusAnaliseConformidade.EM_PROCESSAMENTO,
                store.consultar(INSTANCE_ID).orElseThrow().status());

        store.aguardarRevisao(INSTANCE_ID, preliminar);
        var aguardando = store.consultar(INSTANCE_ID).orElseThrow();
        assertEquals(StatusAnaliseConformidade.AGUARDANDO_REVISAO, aguardando.status());
        assertSame(preliminar, aguardando.resultadoPreliminar());

        store.reservarRevisao(INSTANCE_ID, revisao(finalizado));
        store.concluir(INSTANCE_ID, finalizado);

        var concluida = store.consultar(INSTANCE_ID).orElseThrow();
        assertEquals(CORRELATION_ID, concluida.correlationId());
        assertEquals(IDENTIFICADOR_DOCUMENTO, concluida.identificadorDocumento());
        assertEquals(1000012583L, concluida.identificadorChecklist());
        assertEquals(1, concluida.versaoChecklist());
        assertEquals(StatusAnaliseConformidade.CONCLUIDA, concluida.status());
        assertSame(preliminar, concluida.resultadoPreliminar());
        assertSame(finalizado, concluida.resultadoFinal());
        assertNull(concluida.mensagemErro());
    }

    @Test
    void rejeitaInstanciaAusenteETransicoesForaDeOrdem() {
        ResultadoAnaliseConformidade preliminar = resultado(OrigemResultado.AGENTE);
        ResultadoAnaliseConformidade finalizado = resultado(OrigemResultado.REVISAO_HUMANA);

        FalhaAnaliseConformidade ausente = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.aguardarRevisao("ausente", preliminar));
        iniciar();
        FalhaAnaliseConformidade conclusaoAntecipada = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.concluir(INSTANCE_ID, finalizado));
        RevisaoHumanaConformidade revisaoAntecipada = revisao(finalizado);
        FalhaAnaliseConformidade reservaAntecipada = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.reservarRevisao(INSTANCE_ID, revisaoAntecipada));
        FalhaAnaliseConformidade inicioDuplicado = assertThrows(
                FalhaAnaliseConformidade.class,
                this::iniciar);

        assertEquals(FalhaAnaliseConformidade.Tipo.INSTANCIA_NAO_ENCONTRADA, ausente.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, conclusaoAntecipada.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, reservaAntecipada.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, inicioDuplicado.tipo());
    }

    @Test
    void rejeitaIdentificadorDeInstanciaVazioDeModoControlado() {
        var revisao = revisao(resultado(OrigemResultado.REVISAO_HUMANA));
        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.reservarRevisao(" ", revisao));

        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
    }

    @Test
    void permiteFalharSomenteEstadoNaoTerminalEPreservaPreliminar() {
        ResultadoAnaliseConformidade preliminar = resultado(OrigemResultado.FALLBACK_TECNICO);
        iniciar();
        store.aguardarRevisao(INSTANCE_ID, preliminar);

        store.falhar(INSTANCE_ID, "Falha pública sanitizada");

        var falhou = store.consultar(INSTANCE_ID).orElseThrow();
        assertEquals(StatusAnaliseConformidade.FALHOU, falhou.status());
        assertSame(preliminar, falhou.resultadoPreliminar());
        assertEquals("Falha pública sanitizada", falhou.mensagemErro());
        FalhaAnaliseConformidade repeticao = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.falhar(INSTANCE_ID, "Outra falha"));
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, repeticao.tipo());
    }

    @Test
    void aceitaSomenteUmaDeDuasReservasDeRevisaoConcorrentes() throws Exception {
        iniciar();
        store.aguardarRevisao(INSTANCE_ID, resultado(OrigemResultado.AGENTE));
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        var resultadoFinal = resultado(OrigemResultado.REVISAO_HUMANA);
        Callable<FalhaAnaliseConformidade.Tipo> primeiraTentativa =
                () -> tentarReservarRevisao(
                        prontas,
                        iniciar,
                        new RevisaoHumanaConformidade(
                                "Primeira revisão",
                                resultadoFinal.apontamentos()));
        Callable<FalhaAnaliseConformidade.Tipo> segundaTentativa =
                () -> tentarReservarRevisao(
                        prontas,
                        iniciar,
                        new RevisaoHumanaConformidade(
                                "Segunda revisão",
                                resultadoFinal.apontamentos()));

        try {
            Future<FalhaAnaliseConformidade.Tipo> primeira = executor.submit(primeiraTentativa);
            Future<FalhaAnaliseConformidade.Tipo> segunda = executor.submit(segundaTentativa);
            assertTrue(prontas.await(5, SECONDS));
            iniciar.countDown();

            var resultados = Stream.of(
                    primeira.get(5, SECONDS),
                    segunda.get(5, SECONDS));
            List<FalhaAnaliseConformidade.Tipo> observados = resultados.toList();
            long sucessos = observados.stream().filter(Objects::isNull).count();
            long conflitos = observados.stream()
                    .filter(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA::equals)
                    .count();

            assertEquals(1, sucessos);
            assertEquals(1, conflitos);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aceitaRepeticaoIdenticaDaReservaDeRevisao() {
        iniciar();
        store.aguardarRevisao(INSTANCE_ID, resultado(OrigemResultado.AGENTE));
        var revisao = revisao(resultado(OrigemResultado.REVISAO_HUMANA));

        store.reservarRevisao(INSTANCE_ID, revisao);
        store.reservarRevisao(INSTANCE_ID, revisao);

        assertEquals(
                StatusAnaliseConformidade.AGUARDANDO_REVISAO,
                store.consultar(INSTANCE_ID).orElseThrow().status());
    }

    private FalhaAnaliseConformidade.Tipo tentarReservarRevisao(
            CountDownLatch prontas,
            CountDownLatch iniciar,
            RevisaoHumanaConformidade revisao) throws InterruptedException {
        prontas.countDown();
        iniciar.await();
        try {
            store.reservarRevisao(INSTANCE_ID, revisao);
            return null;
        } catch (FalhaAnaliseConformidade falha) {
            return falha.tipo();
        }
    }

    private void iniciar() {
        var solicitacao = new SolicitacaoAnaliseConformidade(
                CORRELATION_ID,
                IDENTIFICADOR_DOCUMENTO,
                "Texto para análise",
                1000012583L,
                1);
        store.iniciar(INSTANCE_ID, solicitacao);
        store.registrarChecklist(INSTANCE_ID, checklist());
    }

    private static Checklist checklist() {
        return new Checklist(
                "Checklist exemplo",
                1000012583L,
                1,
                "2026-07-26T00:00:00Z",
                "2026-07-26T00:00:00Z",
                false,
                "Orientação",
                List.of(new ApontamentoChecklist(
                        1L,
                        "Apontamento",
                        "Descrição",
                        "Orientação",
                        false,
                        1)));
    }

    private static RevisaoHumanaConformidade revisao(
            ResultadoAnaliseConformidade resultado) {
        return new RevisaoHumanaConformidade(
                "Revisão",
                resultado.apontamentos());
    }

    private static ResultadoAnaliseConformidade resultado(OrigemResultado origem) {
        var apontamento = new ResultadoApontamentoConformidade(
                1L,
                "Apontamento",
                ParecerConformidade.CONFORME,
                "Justificativa",
                "Trecho",
                0.8d);
        return new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist exemplo",
                "Resumo",
                List.of(apontamento),
                origem);
    }
}
