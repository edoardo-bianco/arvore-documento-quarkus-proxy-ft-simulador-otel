package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria.AnaliseConformidadeMemoryStore;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarChecklistEtapaTest {

    private static final SolicitacaoAnaliseConformidade SOLICITACAO =
            new SolicitacaoAnaliseConformidade(
                    "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a",
                    "DOC-2026-000123",
                    "Texto documental",
                    1000012583L,
                    1);

    @Test
    void consultaSemBloqueioECongelaChecklistNoContexto() {
        var controlado = new CompletableFuture<Checklist>();
        var consultar = new ConsultarChecklistFake(
                Uni.createFrom().completionStage(controlado));
        var store = storeIniciado();
        var etapa = etapa(consultar, store);

        var resultado = etapa.executar(
                "instancia-1",
                ContextoAnaliseConformidadeFlow.inicial(SOLICITACAO));
        var futuro = resultado.subscribeAsCompletionStage().toCompletableFuture();

        assertFalse(futuro.isDone());
        assertEquals(
                new ComandoConsultaChecklist(1000012583L, 1),
                consultar.comandoRecebido);

        var apontamentos = new ArrayList<>(List.of(apontamento()));
        controlado.complete(checklist(apontamentos));
        ContextoAnaliseConformidadeFlow contexto = futuro.join();
        apontamentos.clear();

        assertEquals(SOLICITACAO.correlationId(), contexto.correlationId());
        assertEquals(SOLICITACAO.identificadorDocumento(), contexto.identificadorDocumento());
        Checklist persistido = store.carregarChecklist(
                "instancia-1", contexto.checklistRef()).await().indefinitely();
        assertEquals(1, persistido.apontamentos().size());
        List<ApontamentoChecklist> apontamentosCongelados =
                persistido.apontamentos();
        assertThrows(
                UnsupportedOperationException.class,
                apontamentosCongelados::clear);
    }

    @Test
    void rejeitaItemNuloEChecklistSemApontamentos() {
        var storeNulo = storeIniciado();
        var storeVazio = storeIniciado();
        var etapaNula = etapa(
                comando -> Uni.createFrom().nullItem(), storeNulo);
        var etapaVazia = etapa(
                comando -> Uni.createFrom().item(checklist(List.of())), storeVazio);

        FalhaAnaliseConformidade nula = falhaDa(etapaNula.executar(
                "instancia-1", ContextoAnaliseConformidadeFlow.inicial(SOLICITACAO)));
        FalhaAnaliseConformidade vazia = falhaDa(etapaVazia.executar(
                "instancia-1", ContextoAnaliseConformidadeFlow.inicial(SOLICITACAO)));

        assertEquals(FalhaAnaliseConformidade.Tipo.CHECKLIST_INVALIDO, nula.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.CHECKLIST_INVALIDO, vazia.tipo());
    }

    @Test
    void propagaFalhaDaConsultaSemRetryAdicional() {
        var falhaEsperada = new IllegalStateException("falha parametrização");
        var etapa = etapa(
                comando -> Uni.createFrom().failure(falhaEsperada),
                storeIniciado());
        var resultado = etapa.executar(
                        "instancia-1",
                        ContextoAnaliseConformidadeFlow.inicial(SOLICITACAO))
                .subscribeAsCompletionStage()
                .toCompletableFuture();

        CompletionException falha = assertThrows(
                CompletionException.class,
                resultado::join);

        assertSame(falhaEsperada, falha.getCause());
    }

    private static ConsultarChecklistEtapa etapa(
            ConsultarChecklist consultar,
            AnaliseConformidadeMemoryStore store) {
        return new ConsultarChecklistEtapa(
                consultar,
                store,
                new ReferenciasDocumentoAnaliseConformidade(new ObjectMapper()));
    }

    private static AnaliseConformidadeMemoryStore storeIniciado() {
        var store = new AnaliseConformidadeMemoryStore();
        store.iniciar("instancia-1", SOLICITACAO).await().indefinitely();
        return store;
    }

    private static FalhaAnaliseConformidade falhaDa(
            Uni<ContextoAnaliseConformidadeFlow> resultado) {
        var futuro = resultado.subscribeAsCompletionStage().toCompletableFuture();
        CompletionException falha = assertThrows(
                CompletionException.class,
                futuro::join);
        return (FalhaAnaliseConformidade) falha.getCause();
    }

    private static Checklist checklist(List<ApontamentoChecklist> apontamentos) {
        return new Checklist(
                "Checklist documental",
                1000012583L,
                1,
                "2026-07-24T10:00:00-03:00",
                "2026-07-24T10:00:00-03:00",
                false,
                "Orientação",
                apontamentos);
    }

    private static ApontamentoChecklist apontamento() {
        return new ApontamentoChecklist(
                10L,
                "Documento identificado",
                "Verificar documento",
                "Conferir conteúdo",
                false,
                1);
    }

    private static final class ConsultarChecklistFake implements ConsultarChecklist {

        private final Uni<Checklist> resultado;
        private ComandoConsultaChecklist comandoRecebido;

        private ConsultarChecklistFake(Uni<Checklist> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<Checklist> executar(ComandoConsultaChecklist comando) {
            comandoRecebido = comando;
            return resultado;
        }
    }
}
