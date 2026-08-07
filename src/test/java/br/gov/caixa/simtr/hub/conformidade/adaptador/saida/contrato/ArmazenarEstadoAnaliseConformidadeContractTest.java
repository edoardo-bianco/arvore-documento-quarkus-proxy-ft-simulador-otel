package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.contrato;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EmissaoReferencialAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.TipoEmissaoAnaliseConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public abstract class ArmazenarEstadoAnaliseConformidadeContractTest {

    private final ReferenciasDocumentoAnaliseConformidade referencias =
            new ReferenciasDocumentoAnaliseConformidade(new ObjectMapper());

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
    void recarregaSolicitacaoPersistidaSemDependerDoContextoFlow() {
        var cenario = novoCenario();
        var store = novoStore();
        var solicitacao = new SolicitacaoAnaliseConformidade(
                cenario.correlationId(),
                "DOC-2026-000123",
                "Texto protegido",
                1000012583L,
                1);
        aguardar(store.iniciar(cenario.instanceId(), solicitacao));

        assertEquals(
                solicitacao,
                aguardar(store.carregarSolicitacao(cenario.instanceId())));
    }

    @Test
    void recarregaChecklistSomenteComReferenciaIntegra() {
        var cenario = novoCenario();
        var store = novoStore();
        iniciar(store, cenario);
        Checklist checklist = checklist();
        var referencia = referencias.checklist(cenario.correlationId(), checklist);

        assertEquals(
                checklist,
                aguardar(store.carregarChecklist(cenario.instanceId(), referencia)));

        var adulterada = new ReferenciaDocumentoAnaliseConformidade(
                referencia.documentoRef(),
                "0".repeat(64),
                referencia.versaoSchema());
        String instanceId = cenario.instanceId();
        var leituraAdulterada = store.carregarChecklist(instanceId, adulterada);
        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> aguardar(leituraAdulterada));
        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
    }

    @Test
    void recarregaResultadoPreliminarSomenteComReferenciaIntegra() {
        var cenario = novoCenario();
        var store = novoStore();
        var preliminar = resultado(OrigemResultado.AGENTE);
        iniciar(store, cenario);
        aguardar(store.aguardarRevisao(cenario.instanceId(), preliminar));
        var referencia = referencias.resultadoPreliminar(
                cenario.correlationId(), preliminar);

        assertEquals(
                preliminar,
                aguardar(store.carregarResultadoPreliminar(
                        cenario.instanceId(), referencia)));
    }

    @Test
    void recarregaRevisaoSomenteComReferenciaIntegra() {
        var cenario = novoCenario();
        var store = novoStore();
        var preliminar = resultado(OrigemResultado.AGENTE);
        var revisao = revisao(
                "Aprovada",
                resultado(OrigemResultado.REVISAO_HUMANA));
        iniciar(store, cenario);
        aguardar(store.aguardarRevisao(cenario.instanceId(), preliminar));
        aguardar(store.reservarRevisao(cenario.instanceId(), revisao));
        var referencia = referencias.revisao(cenario.correlationId(), revisao);

        assertEquals(
                revisao,
                aguardar(store.carregarRevisao(cenario.instanceId(), referencia)));
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
    void emissaoReferencialAtualizaProjecaoEEToleranteARepeticao() {
        var cenario = novoCenario();
        var store = novoStore();
        var preliminar = resultado(OrigemResultado.AGENTE);
        iniciar(store, cenario);
        var referencia = referencias.resultadoPreliminar(
                cenario.correlationId(), preliminar);
        aguardar(store.prepararResultadoPreliminar(
                cenario.instanceId(), preliminar, referencia));

        assertEquals(
                StatusAnaliseConformidade.EM_PROCESSAMENTO,
                aguardar(store.consultar(cenario.instanceId())).orElseThrow().status());

        var emissao = new EmissaoReferencialAnaliseConformidade(
                "evento:" + referencia.documentoRef(),
                URI.create("urn:simtr-hub:conformidade"),
                TipoEmissaoAnaliseConformidade.REVISAO_SOLICITADA,
                OffsetDateTime.parse("2026-08-02T12:00:00Z"),
                cenario.instanceId(),
                cenario.correlationId(),
                "emitirSolicitacaoRevisao",
                referencia);
        aguardar(store.registrarEmissao(emissao));
        aguardar(store.registrarEmissao(emissao));

        var aguardando = aguardar(store.consultar(cenario.instanceId())).orElseThrow();
        assertEquals(StatusAnaliseConformidade.AGUARDANDO_REVISAO, aguardando.status());
        assertEquals(preliminar, aguardando.resultadoPreliminar());
    }

    @Test
    void emissaoReferencialRejeitaDocumentoQueNaoCorrespondeACorrelacao() {
        var cenario = novoCenario();
        var store = novoStore();
        var preliminar = resultado(OrigemResultado.AGENTE);
        iniciar(store, cenario);
        var referencia = referencias.resultadoPreliminar(
                cenario.correlationId(), preliminar);
        aguardar(store.prepararResultadoPreliminar(
                cenario.instanceId(), preliminar, referencia));
        var adulterada = new br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
                .ReferenciaDocumentoAnaliseConformidade(
                        "resultado-preliminar-adulterado",
                        referencia.hashConteudo(),
                        referencia.versaoSchema());
        var emissao = new EmissaoReferencialAnaliseConformidade(
                "evento-adulterado",
                URI.create("urn:simtr-hub:conformidade"),
                TipoEmissaoAnaliseConformidade.REVISAO_SOLICITADA,
                OffsetDateTime.parse("2026-08-02T12:00:00Z"),
                cenario.instanceId(),
                cenario.correlationId(),
                null,
                adulterada);

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> aguardar(store.registrarEmissao(emissao)));

        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
    }

    @Test
    void emissaoReferencialFinalConcluiComDocumentoPreparado() {
        var cenario = novoCenario();
        var store = novoStore();
        var preliminar = resultado(OrigemResultado.AGENTE);
        var finalizado = resultado(OrigemResultado.REVISAO_HUMANA);
        iniciar(store, cenario);
        var referenciaPreliminar = referencias.resultadoPreliminar(
                cenario.correlationId(), preliminar);
        aguardar(store.prepararResultadoPreliminar(
                cenario.instanceId(), preliminar, referenciaPreliminar));
        aguardar(store.registrarEmissao(emissao(
                cenario,
                TipoEmissaoAnaliseConformidade.REVISAO_SOLICITADA,
                referenciaPreliminar)));
        aguardar(store.reservarRevisao(
                cenario.instanceId(),
                revisao("Aprovada", finalizado)));
        var referenciaFinal = referencias.resultadoFinal(
                cenario.correlationId(), finalizado);
        aguardar(store.prepararResultadoFinal(
                cenario.instanceId(), finalizado, referenciaFinal));
        var emissaoFinal = emissao(
                cenario,
                TipoEmissaoAnaliseConformidade.ANALISE_CONCLUIDA,
                referenciaFinal);

        aguardar(store.registrarEmissao(emissaoFinal));
        aguardar(store.registrarEmissao(emissaoFinal));

        var concluida = aguardar(store.consultar(cenario.instanceId())).orElseThrow();
        assertEquals(StatusAnaliseConformidade.CONCLUIDA, concluida.status());
        assertEquals(finalizado, concluida.resultadoFinal());
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

    private static EmissaoReferencialAnaliseConformidade emissao(
            Cenario cenario,
            TipoEmissaoAnaliseConformidade tipo,
            br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
                    .ReferenciaDocumentoAnaliseConformidade referencia) {
        return new EmissaoReferencialAnaliseConformidade(
                "evento:" + referencia.documentoRef(),
                URI.create("urn:simtr-hub:conformidade"),
                tipo,
                OffsetDateTime.parse("2026-08-02T12:00:00Z"),
                cenario.instanceId(),
                cenario.correlationId(),
                null,
                referencia);
    }

    protected record Cenario(String correlationId, String instanceId) {
    }
}
