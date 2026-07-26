package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.contrato;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public abstract class ArmazenarEstadoAnaliseConformidadeContractTest {

    protected abstract ArmazenarEstadoAnaliseConformidade novoStore();

    @Test
    void rejeitaInicioDuplicadoDaMesmaAnaliseCompleta() {
        var cenario = novoCenario();
        var store = novoStore();
        String instanceId = cenario.instanceId();
        var solicitacao = new SolicitacaoAnaliseConformidade(
                cenario.correlationId(),
                "DOC-2026-000123",
                "Texto protegido",
                1000012583L,
                1);
        aguardar(store.iniciar(instanceId, solicitacao));

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> aguardar(store.iniciar(instanceId, solicitacao)));

        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
    }

    @Test
    void percorreSequenciaCompletaEPreservaIdentidades() {
        var cenario = novoCenario();
        var store = novoStore();
        var preliminar = resultado(OrigemResultado.AGENTE);
        var finalizado = resultado(OrigemResultado.REVISAO_HUMANA);

        iniciar(store, cenario);
        aguardar(store.aguardarRevisao(cenario.instanceId(), preliminar));
        aguardar(store.reservarRevisao(
                cenario.instanceId(),
                revisao("Aprovada", finalizado)));
        aguardar(store.concluir(cenario.instanceId(), finalizado));

        var concluida = aguardar(store.consultar(cenario.instanceId())).orElseThrow();
        assertEquals(cenario.correlationId(), concluida.correlationId());
        assertEquals(cenario.instanceId(), concluida.instanceId());
        assertEquals("DOC-2026-000123", concluida.identificadorDocumento());
        assertEquals(StatusAnaliseConformidade.CONCLUIDA, concluida.status());
        assertEquals(preliminar, concluida.resultadoPreliminar());
        assertEquals(finalizado, concluida.resultadoFinal());
        assertNull(concluida.mensagemErro());
    }

    @Test
    void aceitaRevisaoIdenticaERejeitaConteudoContraditorio() {
        var cenario = novoCenario();
        var store = novoStore();
        String instanceId = cenario.instanceId();
        var finalizado = resultado(OrigemResultado.REVISAO_HUMANA);
        iniciar(store, cenario);
        aguardar(store.aguardarRevisao(
                instanceId,
                resultado(OrigemResultado.AGENTE)));
        var primeira = revisao("Aprovada", finalizado);
        var contraditoria = revisao("Contraditória", finalizado);

        aguardar(store.reservarRevisao(instanceId, primeira));
        aguardar(store.reservarRevisao(instanceId, primeira));
        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> aguardar(store.reservarRevisao(instanceId, contraditoria)));

        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
    }

    @Test
    void permiteFalhaSanitizadaERejeitaNovaTransicaoTerminal() {
        var cenario = novoCenario();
        var store = novoStore();
        var preliminar = resultado(OrigemResultado.FALLBACK_TECNICO);
        iniciar(store, cenario);
        aguardar(store.aguardarRevisao(cenario.instanceId(), preliminar));

        aguardar(store.falhar(cenario.instanceId(), "Falha pública sanitizada"));

        var falhou = aguardar(store.consultar(cenario.instanceId())).orElseThrow();
        String instanceId = cenario.instanceId();
        assertEquals(StatusAnaliseConformidade.FALHOU, falhou.status());
        assertEquals(preliminar, falhou.resultadoPreliminar());
        assertEquals("Falha pública sanitizada", falhou.mensagemErro());
        FalhaAnaliseConformidade repeticao = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> aguardar(store.falhar(instanceId, "Outra falha")));
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, repeticao.tipo());
    }

    @Test
    void concorrenciaAceitaSomenteUmaRevisaoContraditoria() throws Exception {
        var cenario = novoCenario();
        var store = novoStore();
        var finalizado = resultado(OrigemResultado.REVISAO_HUMANA);
        iniciar(store, cenario);
        aguardar(store.aguardarRevisao(
                cenario.instanceId(),
                resultado(OrigemResultado.AGENTE)));
        var prontas = new CountDownLatch(2);
        var largada = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        Callable<FalhaAnaliseConformidade.Tipo> primeira = () -> reservar(
                store,
                cenario.instanceId(),
                revisao("Primeira", finalizado),
                prontas,
                largada);
        Callable<FalhaAnaliseConformidade.Tipo> segunda = () -> reservar(
                store,
                cenario.instanceId(),
                revisao("Segunda", finalizado),
                prontas,
                largada);

        try {
            var primeiraResposta = executor.submit(primeira);
            var segundaResposta = executor.submit(segunda);
            assertTrue(prontas.await(5, SECONDS));
            largada.countDown();
            var respostas = Stream.of(
                    primeiraResposta.get(5, SECONDS),
                    segundaResposta.get(5, SECONDS)).toList();
            assertEquals(1, respostas.stream().filter(Objects::isNull).count());
            assertEquals(
                    1,
                    respostas.stream()
                            .filter(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA::equals)
                            .count());
        } finally {
            executor.shutdownNow();
        }
    }

    private static FalhaAnaliseConformidade.Tipo reservar(
            ArmazenarEstadoAnaliseConformidade store,
            String instanceId,
            RevisaoHumanaConformidade revisao,
            CountDownLatch prontas,
            CountDownLatch largada) throws InterruptedException {
        prontas.countDown();
        largada.await();
        try {
            aguardar(store.reservarRevisao(instanceId, revisao));
            return null;
        } catch (FalhaAnaliseConformidade falha) {
            return falha.tipo();
        }
    }

    protected static void iniciar(
            ArmazenarEstadoAnaliseConformidade store,
            Cenario cenario) {
        aguardar(store.iniciar(
                cenario.instanceId(),
                new SolicitacaoAnaliseConformidade(
                        cenario.correlationId(),
                        "DOC-2026-000123",
                        "Texto protegido",
                        1000012583L,
                        1)));
        aguardar(store.registrarChecklist(cenario.instanceId(), checklist()));
    }

    protected static <T> T aguardar(io.smallrye.mutiny.Uni<T> operacao) {
        return operacao.await().indefinitely();
    }

    protected static Cenario novoCenario() {
        String sufixo = UUID.randomUUID().toString();
        return new Cenario("correlation-" + sufixo, "instance-" + sufixo);
    }

    protected static Checklist checklist() {
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

    protected static ResultadoAnaliseConformidade resultado(OrigemResultado origem) {
        return new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist exemplo",
                "Resumo",
                List.of(new ResultadoApontamentoConformidade(
                        1L,
                        "Apontamento",
                        ParecerConformidade.CONFORME,
                        "Justificativa",
                        "Trecho",
                        0.8d)),
                origem);
    }

    protected static RevisaoHumanaConformidade revisao(
            String observacao,
            ResultadoAnaliseConformidade resultado) {
        return new RevisaoHumanaConformidade(observacao, resultado.apontamentos());
    }

    protected record Cenario(String correlationId, String instanceId) {
    }
}
