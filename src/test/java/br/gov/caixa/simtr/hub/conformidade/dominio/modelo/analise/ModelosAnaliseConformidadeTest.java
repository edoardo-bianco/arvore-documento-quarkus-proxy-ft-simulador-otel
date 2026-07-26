package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ModelosAnaliseConformidadeTest {

    @Test
    void aceitaSolicitacaoNoLimiteSemAlterarOTexto() {
        String textoNoLimite = "x".repeat(SolicitacaoAnaliseConformidade.TAMANHO_MAXIMO_TEXTO);

        var solicitacao = SolicitacaoAnaliseConformidade.nova(
                "DOC-2026-000123",
                textoNoLimite,
                1000012583L,
                1);

        UUID.fromString(solicitacao.correlationId());
        assertEquals("DOC-2026-000123", solicitacao.identificadorDocumento());
        assertEquals(textoNoLimite, solicitacao.texto());
        assertEquals(1000012583L, solicitacao.identificadorChecklist());
        assertEquals(1, solicitacao.versaoChecklist());
    }

    @Test
    void rejeitaSolicitacaoComTextoVazioOuAcimaDoLimite() {
        String textoExcedente = "x".repeat(SolicitacaoAnaliseConformidade.TAMANHO_MAXIMO_TEXTO + 1);

        FalhaAnaliseConformidade vazia = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> SolicitacaoAnaliseConformidade.nova(
                        "DOC-2026-000123", " ", 1000012583L, 1));
        FalhaAnaliseConformidade excedente = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> SolicitacaoAnaliseConformidade.nova(
                        "DOC-2026-000123", textoExcedente, 1000012583L, 1));

        assertEquals(FalhaAnaliseConformidade.Tipo.SOLICITACAO_INVALIDA, vazia.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.SOLICITACAO_INVALIDA, excedente.tipo());
    }

    @Test
    void rejeitaReferenciaDeChecklistInvalida() {
        FalhaAnaliseConformidade documentoInvalido = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> SolicitacaoAnaliseConformidade.nova(" ", "texto", 1000012583L, 1));
        FalhaAnaliseConformidade identificadorInvalido = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> SolicitacaoAnaliseConformidade.nova(
                        "DOC-2026-000123", "texto", 0L, 1));
        FalhaAnaliseConformidade versaoInvalida = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> SolicitacaoAnaliseConformidade.nova(
                        "DOC-2026-000123", "texto", 1000012583L, 0));

        assertEquals(FalhaAnaliseConformidade.Tipo.SOLICITACAO_INVALIDA, documentoInvalido.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.SOLICITACAO_INVALIDA, identificadorInvalido.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.SOLICITACAO_INVALIDA, versaoInvalida.tipo());
    }

    @Test
    void impedeTrocaDeIdentidadeDuranteTransicao() {
        var visao = VisaoAnaliseConformidade.emProcessamento(
                "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a",
                "instancia-1",
                "DOC-2026-000123",
                1000012583L,
                1);
        var resultadoDeOutroChecklist = new ResultadoAnaliseConformidade(
                999L,
                2,
                "Outro checklist",
                "Resumo",
                List.of(apontamentoValido()),
                OrigemResultado.AGENTE);

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> visao.aguardandoRevisao(resultadoDeOutroChecklist));

        assertEquals(FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA, falha.tipo());
        assertEquals("7aa3ca4d-3c7e-4f61-a3a1-996571d3397a", visao.correlationId());
        assertEquals("DOC-2026-000123", visao.identificadorDocumento());
    }

    @Test
    void protegeListaDoResultadoContraMutacaoExterna() {
        var apontamento = apontamentoValido();
        var origemMutavel = new ArrayList<>(List.of(apontamento));

        var resultado = new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist exemplo",
                "Resumo",
                origemMutavel,
                OrigemResultado.AGENTE);
        origemMutavel.clear();

        assertEquals(1, resultado.apontamentos().size());
        assertSame(apontamento, resultado.apontamentos().getFirst());
        List<ResultadoApontamentoConformidade> somenteLeitura = resultado.apontamentos();
        assertThrows(UnsupportedOperationException.class, () -> somenteLeitura.add(apontamento));
    }

    @Test
    void rejeitaApontamentoSemJustificativaOuComConfiancaInvalida() {
        FalhaAnaliseConformidade semJustificativa = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> new ResultadoApontamentoConformidade(
                        1L, "Apontamento", ParecerConformidade.CONFORME, " ", null, 0.8d));
        FalhaAnaliseConformidade acimaDoLimite = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> new ResultadoApontamentoConformidade(
                        1L, "Apontamento", ParecerConformidade.CONFORME, "Justificativa", null, 1.1d));
        FalhaAnaliseConformidade naoNumerica = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> new ResultadoApontamentoConformidade(
                        1L, "Apontamento", ParecerConformidade.CONFORME, "Justificativa", null, Double.NaN));

        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, semJustificativa.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, acimaDoLimite.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, naoNumerica.tipo());
    }

    @Test
    void protegeListaDaRevisaoContraMutacaoExterna() {
        var origemMutavel = new ArrayList<>(List.of(apontamentoValido()));

        var revisao = new RevisaoHumanaConformidade("Revisado pelo operador", origemMutavel);
        origemMutavel.clear();

        assertEquals(1, revisao.apontamentos().size());
        List<ResultadoApontamentoConformidade> somenteLeitura = revisao.apontamentos();
        assertThrows(UnsupportedOperationException.class, somenteLeitura::clear);
    }

    @Test
    void rejeitaElementoNuloNasListasComFalhaDeDominio() {
        var resultadoComNulo = new ArrayList<ResultadoApontamentoConformidade>();
        resultadoComNulo.add(null);
        var revisaoComNulo = new ArrayList<ResultadoApontamentoConformidade>();
        revisaoComNulo.add(null);

        FalhaAnaliseConformidade resultadoInvalido = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> new ResultadoAnaliseConformidade(
                        1000012583L,
                        1,
                        "Checklist exemplo",
                        "Resumo",
                        resultadoComNulo,
                        OrigemResultado.AGENTE));
        FalhaAnaliseConformidade revisaoInvalida = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> new RevisaoHumanaConformidade("Revisão", revisaoComNulo));

        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, resultadoInvalido.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.REVISAO_INCONSISTENTE, revisaoInvalida.tipo());
    }

    private static ResultadoApontamentoConformidade apontamentoValido() {
        return new ResultadoApontamentoConformidade(
                1L,
                "Apontamento",
                ParecerConformidade.CONFORME,
                "Justificativa",
                "Trecho literal",
                0.8d);
    }
}
