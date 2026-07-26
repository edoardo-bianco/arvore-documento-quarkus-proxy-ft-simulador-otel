package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
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
        var etapa = new ConsultarChecklistEtapa(consultar);

        var resultado = etapa.executar(SOLICITACAO);

        assertFalse(resultado.toCompletableFuture().isDone());
        assertEquals(
                new ComandoConsultaChecklist(1000012583L, 1),
                consultar.comandoRecebido);

        var apontamentos = new ArrayList<>(List.of(apontamento()));
        controlado.complete(checklist(apontamentos));
        ContextoAnaliseConformidadeFlow contexto =
                resultado.toCompletableFuture().join();
        apontamentos.clear();

        assertEquals(SOLICITACAO.correlationId(), contexto.correlationId());
        assertEquals(SOLICITACAO.identificadorDocumento(), contexto.identificadorDocumento());
        assertEquals("Texto documental", contexto.texto());
        assertEquals(1, contexto.checklist().apontamentos().size());
        List<ApontamentoChecklist> apontamentosCongelados =
                contexto.checklist().apontamentos();
        assertThrows(
                UnsupportedOperationException.class,
                apontamentosCongelados::clear);
    }

    @Test
    void rejeitaItemNuloEChecklistSemApontamentos() {
        var etapaNula = new ConsultarChecklistEtapa(
                comando -> Uni.createFrom().nullItem());
        var etapaVazia = new ConsultarChecklistEtapa(
                comando -> Uni.createFrom().item(checklist(List.of())));

        FalhaAnaliseConformidade nula = falhaDa(etapaNula.executar(SOLICITACAO));
        FalhaAnaliseConformidade vazia = falhaDa(etapaVazia.executar(SOLICITACAO));

        assertEquals(FalhaAnaliseConformidade.Tipo.CHECKLIST_INVALIDO, nula.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.CHECKLIST_INVALIDO, vazia.tipo());
    }

    @Test
    void propagaFalhaDaConsultaSemRetryAdicional() {
        var falhaEsperada = new IllegalStateException("falha parametrização");
        var etapa = new ConsultarChecklistEtapa(
                comando -> Uni.createFrom().failure(falhaEsperada));
        var resultado = etapa.executar(SOLICITACAO).toCompletableFuture();

        CompletionException falha = assertThrows(
                CompletionException.class,
                resultado::join);

        assertSame(falhaEsperada, falha.getCause());
    }

    private static FalhaAnaliseConformidade falhaDa(
            java.util.concurrent.CompletionStage<ContextoAnaliseConformidadeFlow> resultado) {
        var futuro = resultado.toCompletableFuture();
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
