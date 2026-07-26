package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
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

    private static final String INSTANCE_ID = "instancia-analise-1";

    private final AnaliseConformidadeMemoryStore store = new AnaliseConformidadeMemoryStore();

    @Test
    void percorreSomenteASequenciaCompletaAteConclusao() {
        ResultadoAnaliseConformidade preliminar = resultado(OrigemResultado.AGENTE);
        ResultadoAnaliseConformidade finalizado = resultado(OrigemResultado.REVISAO_HUMANA);

        store.iniciar(INSTANCE_ID);
        assertEquals(
                StatusAnaliseConformidade.EM_PROCESSAMENTO,
                store.consultar(INSTANCE_ID).orElseThrow().status());

        store.aguardarRevisao(INSTANCE_ID, preliminar);
        var aguardando = store.consultar(INSTANCE_ID).orElseThrow();
        assertEquals(StatusAnaliseConformidade.AGUARDANDO_REVISAO, aguardando.status());
        assertSame(preliminar, aguardando.resultadoPreliminar());

        store.reservarRevisao(INSTANCE_ID);
        store.concluir(INSTANCE_ID, finalizado);

        var concluida = store.consultar(INSTANCE_ID).orElseThrow();
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
        store.iniciar(INSTANCE_ID);
        FalhaAnaliseConformidade conclusaoAntecipada = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.concluir(INSTANCE_ID, finalizado));
        FalhaAnaliseConformidade reservaAntecipada = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.reservarRevisao(INSTANCE_ID));
        FalhaAnaliseConformidade inicioDuplicado = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.iniciar(INSTANCE_ID));

        assertEquals(FalhaAnaliseConformidade.Tipo.INSTANCIA_NAO_ENCONTRADA, ausente.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, conclusaoAntecipada.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, reservaAntecipada.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, inicioDuplicado.tipo());
    }

    @Test
    void rejeitaIdentificadorDeInstanciaVazioDeModoControlado() {
        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> store.reservarRevisao(" "));

        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
    }

    @Test
    void permiteFalharSomenteEstadoNaoTerminalEPreservaPreliminar() {
        ResultadoAnaliseConformidade preliminar = resultado(OrigemResultado.FALLBACK_TECNICO);
        store.iniciar(INSTANCE_ID);
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
        store.iniciar(INSTANCE_ID);
        store.aguardarRevisao(INSTANCE_ID, resultado(OrigemResultado.AGENTE));
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<FalhaAnaliseConformidade.Tipo> tentativa =
                () -> tentarReservarRevisao(prontas, iniciar);

        try {
            Future<FalhaAnaliseConformidade.Tipo> primeira = executor.submit(tentativa);
            Future<FalhaAnaliseConformidade.Tipo> segunda = executor.submit(tentativa);
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

    private FalhaAnaliseConformidade.Tipo tentarReservarRevisao(
            CountDownLatch prontas,
            CountDownLatch iniciar) throws InterruptedException {
        prontas.countDown();
        iniciar.await();
        try {
            store.reservarRevisao(INSTANCE_ID);
            return null;
        } catch (FalhaAnaliseConformidade falha) {
            return falha.tipo();
        }
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
