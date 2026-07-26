package br.gov.caixa.simtr.hub.conformidade.dominio.validacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import java.util.List;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ValidadorAnaliseConformidadeTest {

    private final ValidadorAnaliseConformidade validador = new ValidadorAnaliseConformidade();

    @Test
    void validaCoberturaEOrdenaResultadoConformeChecklist() {
        Checklist checklist = checklistValido();
        var resultadoForaDeOrdem = resultado(
                List.of(
                        apontamentoResultado(2L, "Segundo", 0.7d),
                        apontamentoResultado(1L, "Primeiro", 0.8d)),
                OrigemResultado.AGENTE);

        ResultadoAnaliseConformidade validado =
                validador.validarResultado(checklist, resultadoForaDeOrdem);

        assertEquals(1L, validado.apontamentos().get(0).identificadorApontamento());
        assertEquals(2L, validado.apontamentos().get(1).identificadorApontamento());
        assertEquals(OrigemResultado.AGENTE, validado.origem());
    }

    @Test
    void rejeitaChecklistSemApontamentos() {
        Checklist checklistVazio = new Checklist(
                "Checklist exemplo",
                1000012583L,
                1,
                null,
                null,
                false,
                null,
                List.of());
        ResultadoAnaliseConformidade resultado = resultado(
                List.of(apontamentoResultado(1L, "Primeiro", 0.8d)),
                OrigemResultado.AGENTE);

        FalhaAnaliseConformidade falha = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.validarResultado(checklistVazio, resultado));

        assertEquals(FalhaAnaliseConformidade.Tipo.CHECKLIST_INVALIDO, falha.tipo());
    }

    @Test
    void rejeitaResultadoComApontamentoAusenteExtraOuDuplicado() {
        Checklist checklist = checklistValido();
        ResultadoAnaliseConformidade ausente = resultado(
                List.of(apontamentoResultado(1L, "Primeiro", 0.8d)),
                OrigemResultado.AGENTE);
        ResultadoAnaliseConformidade extra = resultado(
                List.of(
                        apontamentoResultado(1L, "Primeiro", 0.8d),
                        apontamentoResultado(2L, "Segundo", 0.7d),
                        apontamentoResultado(3L, "Terceiro", 0.6d)),
                OrigemResultado.AGENTE);
        ResultadoAnaliseConformidade duplicado = resultado(
                List.of(
                        apontamentoResultado(1L, "Primeiro", 0.8d),
                        apontamentoResultado(1L, "Primeiro", 0.7d)),
                OrigemResultado.AGENTE);

        FalhaAnaliseConformidade falhaAusente = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.validarResultado(checklist, ausente));
        FalhaAnaliseConformidade falhaExtra = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.validarResultado(checklist, extra));
        FalhaAnaliseConformidade falhaDuplicado = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.validarResultado(checklist, duplicado));

        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, falhaAusente.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, falhaExtra.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, falhaDuplicado.tipo());
    }

    @Test
    void rejeitaResultadoQueAlteraIdentidadeDoChecklistOuNomeDoApontamento() {
        Checklist checklist = checklistValido();
        ResultadoAnaliseConformidade checklistDiferente = new ResultadoAnaliseConformidade(
                999L,
                1,
                "Checklist exemplo",
                "Resumo",
                List.of(
                        apontamentoResultado(1L, "Primeiro", 0.8d),
                        apontamentoResultado(2L, "Segundo", 0.7d)),
                OrigemResultado.AGENTE);
        ResultadoAnaliseConformidade nomeDiferente = resultado(
                List.of(
                        apontamentoResultado(1L, "Nome alterado", 0.8d),
                        apontamentoResultado(2L, "Segundo", 0.7d)),
                OrigemResultado.AGENTE);

        FalhaAnaliseConformidade falhaChecklist = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.validarResultado(checklist, checklistDiferente));
        FalhaAnaliseConformidade falhaNome = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.validarResultado(checklist, nomeDiferente));

        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, falhaChecklist.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.RESULTADO_INVALIDO, falhaNome.tipo());
    }

    @Test
    void consolidaRevisaoCompletaPreservandoIdentidadeEConfianca() {
        ResultadoAnaliseConformidade preliminar = resultado(
                List.of(
                        apontamentoResultado(1L, "Primeiro", 0.8d),
                        apontamentoResultado(2L, "Segundo", 0.7d)),
                OrigemResultado.AGENTE);
        RevisaoHumanaConformidade revisao = new RevisaoHumanaConformidade(
                "Revisão do operador",
                List.of(
                        apontamentoRevisado(2L, "Segundo", 0.7d),
                        apontamentoRevisado(1L, "Primeiro", 0.8d)));

        ResultadoAnaliseConformidade finalizado =
                validador.consolidarRevisao(preliminar, revisao);

        assertEquals(OrigemResultado.REVISAO_HUMANA, finalizado.origem());
        assertEquals(preliminar.identificadorChecklist(), finalizado.identificadorChecklist());
        assertEquals(preliminar.versaoChecklist(), finalizado.versaoChecklist());
        assertEquals(preliminar.nomeChecklist(), finalizado.nomeChecklist());
        assertEquals(1L, finalizado.apontamentos().get(0).identificadorApontamento());
        assertEquals(0.8d, finalizado.apontamentos().get(0).confianca());
        assertEquals(ParecerConformidade.INCONFORME, finalizado.apontamentos().get(0).parecer());
    }

    @Test
    void rejeitaRevisaoQueAlteraNomeOuConfianca() {
        ResultadoAnaliseConformidade preliminar = resultado(
                List.of(
                        apontamentoResultado(1L, "Primeiro", 0.8d),
                        apontamentoResultado(2L, "Segundo", 0.7d)),
                OrigemResultado.AGENTE);
        RevisaoHumanaConformidade nomeAlterado = new RevisaoHumanaConformidade(
                "Revisão",
                List.of(
                        apontamentoRevisado(1L, "Nome alterado", 0.8d),
                        apontamentoRevisado(2L, "Segundo", 0.7d)));
        RevisaoHumanaConformidade confiancaAlterada = new RevisaoHumanaConformidade(
                "Revisão",
                List.of(
                        apontamentoRevisado(1L, "Primeiro", 0.9d),
                        apontamentoRevisado(2L, "Segundo", 0.7d)));

        FalhaAnaliseConformidade falhaNome = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.consolidarRevisao(preliminar, nomeAlterado));
        FalhaAnaliseConformidade falhaConfianca = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.consolidarRevisao(preliminar, confiancaAlterada));

        assertEquals(FalhaAnaliseConformidade.Tipo.REVISAO_INCONSISTENTE, falhaNome.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.REVISAO_INCONSISTENTE, falhaConfianca.tipo());
    }

    @Test
    void rejeitaRevisaoIncompletaExtraOuDuplicada() {
        ResultadoAnaliseConformidade preliminar = resultado(
                List.of(
                        apontamentoResultado(1L, "Primeiro", 0.8d),
                        apontamentoResultado(2L, "Segundo", 0.7d)),
                OrigemResultado.FALLBACK_TECNICO);
        RevisaoHumanaConformidade incompleta = new RevisaoHumanaConformidade(
                "Revisão",
                List.of(apontamentoRevisado(1L, "Primeiro", 0.8d)));
        RevisaoHumanaConformidade extra = new RevisaoHumanaConformidade(
                "Revisão",
                List.of(
                        apontamentoRevisado(1L, "Primeiro", 0.8d),
                        apontamentoRevisado(2L, "Segundo", 0.7d),
                        apontamentoRevisado(3L, "Terceiro", 0.6d)));
        RevisaoHumanaConformidade duplicada = new RevisaoHumanaConformidade(
                "Revisão",
                List.of(
                        apontamentoRevisado(1L, "Primeiro", 0.8d),
                        apontamentoRevisado(1L, "Primeiro", 0.8d)));

        FalhaAnaliseConformidade falhaIncompleta = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.consolidarRevisao(preliminar, incompleta));
        FalhaAnaliseConformidade falhaExtra = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.consolidarRevisao(preliminar, extra));
        FalhaAnaliseConformidade falhaDuplicada = assertThrows(
                FalhaAnaliseConformidade.class,
                () -> validador.consolidarRevisao(preliminar, duplicada));

        assertEquals(FalhaAnaliseConformidade.Tipo.REVISAO_INCONSISTENTE, falhaIncompleta.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.REVISAO_INCONSISTENTE, falhaExtra.tipo());
        assertEquals(FalhaAnaliseConformidade.Tipo.REVISAO_INCONSISTENTE, falhaDuplicada.tipo());
    }

    private static Checklist checklistValido() {
        return new Checklist(
                "Checklist exemplo",
                1000012583L,
                1,
                null,
                null,
                false,
                null,
                List.of(
                        apontamentoChecklist(2L, "Segundo", 2),
                        apontamentoChecklist(1L, "Primeiro", 1)));
    }

    private static ApontamentoChecklist apontamentoChecklist(Long id, String nome, Integer sequencia) {
        return new ApontamentoChecklist(id, nome, null, null, false, sequencia);
    }

    private static ResultadoAnaliseConformidade resultado(
            List<ResultadoApontamentoConformidade> apontamentos,
            OrigemResultado origem) {
        return new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist exemplo",
                "Resumo preliminar",
                apontamentos,
                origem);
    }

    private static ResultadoApontamentoConformidade apontamentoResultado(
            Long id,
            String nome,
            double confianca) {
        return new ResultadoApontamentoConformidade(
                id,
                nome,
                ParecerConformidade.INCONCLUSIVO,
                "Justificativa preliminar",
                null,
                confianca);
    }

    private static ResultadoApontamentoConformidade apontamentoRevisado(
            Long id,
            String nome,
            double confianca) {
        return new ResultadoApontamentoConformidade(
                id,
                nome,
                ParecerConformidade.INCONFORME,
                "Justificativa revisada",
                "Trecho revisado",
                confianca);
    }
}
