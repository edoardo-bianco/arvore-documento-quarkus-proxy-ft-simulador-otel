package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria.AnaliseConformidadeMemoryStore;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.PublicarRevisaoNoWorkflow;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.AnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
class CasosDeUsoAnaliseConformidadeTest {

    private static final String CORRELATION_ID = "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String IDENTIFICADOR_DOCUMENTO = "DOC-2026-000123";

    @Test
    void iniciarCriaInstanciaEmProcessamentoSemArmazenarOTexto() {
        var store = new AnaliseConformidadeMemoryStore();
        var solicitacao = new SolicitacaoAnaliseConformidade(
                CORRELATION_ID,
                IDENTIFICADOR_DOCUMENTO,
                "Documento potencialmente sensível",
                1000012583L,
                1);
        var flow = mock(AnaliseConformidadeFlow.class);
        var instancia = mock(WorkflowInstance.class);
        when(flow.instance(solicitacao)).thenReturn(instancia);
        when(instancia.id()).thenReturn("01J3FLOWTESTE00000000000000");
        when(instancia.start()).thenReturn(new CompletableFuture<WorkflowModel>());
        var casoDeUso = new IniciarAnaliseConformidadeCasoDeUso(store, flow);

        var visao = casoDeUso.executar(solicitacao);

        assertEquals("01J3FLOWTESTE00000000000000", visao.instanceId());
        assertEquals(CORRELATION_ID, visao.correlationId());
        assertEquals(IDENTIFICADOR_DOCUMENTO, visao.identificadorDocumento());
        assertEquals(StatusAnaliseConformidade.EM_PROCESSAMENTO, visao.status());
        assertEquals(visao, store.consultar(visao.instanceId()).orElseThrow());
        verify(instancia).start();
    }

    @Test
    void iniciarRejeitaSolicitacaoAusente() {
        var flow = mock(AnaliseConformidadeFlow.class);
        var casoDeUso = new IniciarAnaliseConformidadeCasoDeUso(
                new AnaliseConformidadeMemoryStore(),
                flow);

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> casoDeUso.executar(null));

        assertEquals(FalhaAnaliseConformidade.Tipo.SOLICITACAO_INVALIDA, falha.tipo());
        verifyNoInteractions(flow);
    }

    @Test
    void iniciarTraduzFalhaSincronaDoFlowEFinalizaAProjecao() {
        var store = new AnaliseConformidadeMemoryStore();
        var solicitacao = new SolicitacaoAnaliseConformidade(
                CORRELATION_ID,
                IDENTIFICADOR_DOCUMENTO,
                "Documento",
                1000012583L,
                1);
        var flow = mock(AnaliseConformidadeFlow.class);
        var instancia = mock(WorkflowInstance.class);
        when(flow.instance(solicitacao)).thenReturn(instancia);
        when(instancia.id()).thenReturn("01J3FLOWFALHA0000000000000");
        when(instancia.start()).thenThrow(new IllegalStateException("detalhe interno"));
        var casoDeUso = new IniciarAnaliseConformidadeCasoDeUso(store, flow);

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> casoDeUso.executar(solicitacao));

        assertEquals(
                FalhaAnaliseConformidade.Tipo.INDISPONIBILIDADE_TECNICA,
                falha.tipo());
        var visao = store.consultar("01J3FLOWFALHA0000000000000").orElseThrow();
        assertEquals(StatusAnaliseConformidade.FALHOU, visao.status());
        assertEquals(
                "Não foi possível consultar o checklist para a análise",
                visao.mensagemErro());
    }

    @Test
    void consultarRetornaVisaoOuFalhaControladaParaInstanciaAusente() {
        var store = new AnaliseConformidadeMemoryStore();
        iniciar(store, "instancia-existente");
        var casoDeUso = new ConsultarAnaliseConformidadeCasoDeUso(store);

        assertEquals(
                "instancia-existente",
                casoDeUso.executar("instancia-existente").instanceId());

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> casoDeUso.executar("instancia-ausente"));
        assertEquals(FalhaAnaliseConformidade.Tipo.INSTANCIA_NAO_ENCONTRADA, falha.tipo());
    }

    @Test
    void revisarAceitaRepeticaoIdenticaERepublicaParaEntregaIdempotente() {
        var store = storeAguardandoRevisao();
        var publicador = mock(PublicarRevisaoNoWorkflow.class);
        RevisaoHumanaConformidade revisao = revisaoValida();
        when(publicador.publicar("instancia-1", revisao))
                .thenReturn(Uni.createFrom().voidItem());
        var casoDeUso = new RevisarAnaliseConformidadeCasoDeUso(store, publicador);

        assertDoesNotThrow(() -> casoDeUso.executar("instancia-1", revisao)
                .await()
                .indefinitely());

        assertDoesNotThrow(() -> casoDeUso.executar("instancia-1", revisao)
                .await()
                .indefinitely());
        verify(publicador, times(2)).publicar("instancia-1", revisao);
    }

    @Test
    void revisarInconsistenteNaoReservaAInstancia() {
        var store = storeAguardandoRevisao();
        var publicador = mock(PublicarRevisaoNoWorkflow.class);
        when(publicador.publicar("instancia-1", revisaoValida()))
                .thenReturn(Uni.createFrom().voidItem());
        var casoDeUso = new RevisarAnaliseConformidadeCasoDeUso(store, publicador);
        var apontamentoAlterado = new ResultadoApontamentoConformidade(
                10L,
                "Documento identificado",
                ParecerConformidade.CONFORME,
                "Revisado",
                "Trecho",
                1.0d);
        var revisaoInconsistente = new RevisaoHumanaConformidade(
                "Tentativa inconsistente",
                List.of(apontamentoAlterado));

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> casoDeUso.executar("instancia-1", revisaoInconsistente));

        assertEquals(FalhaAnaliseConformidade.Tipo.REVISAO_INCONSISTENTE, falha.tipo());
        assertDoesNotThrow(() -> casoDeUso.executar("instancia-1", revisaoValida())
                .await()
                .indefinitely());
    }

    @Test
    void revisarPriorizaEstadoDaInstanciaAntesDoConteudo() {
        var store = new AnaliseConformidadeMemoryStore();
        iniciar(store, "instancia-1");
        var publicador = mock(PublicarRevisaoNoWorkflow.class);
        var casoDeUso = new RevisarAnaliseConformidadeCasoDeUso(store, publicador);
        var revisao = revisaoValida();

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> casoDeUso.executar("instancia-1", revisao));

        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
        verifyNoInteractions(publicador);
    }

    private static AnaliseConformidadeMemoryStore storeAguardandoRevisao() {
        var store = new AnaliseConformidadeMemoryStore();
        iniciar(store, "instancia-1");
        store.aguardarRevisao("instancia-1", resultadoPreliminar());
        return store;
    }

    private static void iniciar(
            AnaliseConformidadeMemoryStore store,
            String instanceId) {
        var solicitacao = new SolicitacaoAnaliseConformidade(
                CORRELATION_ID,
                IDENTIFICADOR_DOCUMENTO,
                "Texto para análise",
                1000012583L,
                1);
        store.iniciar(instanceId, solicitacao);
        store.registrarChecklist(instanceId, new Checklist(
                "Checklist documental",
                1000012583L,
                1,
                "2026-07-26T00:00:00Z",
                "2026-07-26T00:00:00Z",
                false,
                "Orientação",
                List.of(new ApontamentoChecklist(
                        10L,
                        "Documento identificado",
                        "Descrição",
                        "Orientação",
                        false,
                        1))));
    }

    private static ResultadoAnaliseConformidade resultadoPreliminar() {
        return new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist documental",
                "Análise preliminar",
                List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.INCONCLUSIVO,
                        "Requer revisão",
                        null,
                        0.5d)),
                OrigemResultado.AGENTE);
    }

    private static RevisaoHumanaConformidade revisaoValida() {
        return new RevisaoHumanaConformidade(
                "Revisão concluída",
                List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.CONFORME,
                        "Confirmado pelo operador",
                        "Trecho revisado",
                        0.5d)));
    }
}
