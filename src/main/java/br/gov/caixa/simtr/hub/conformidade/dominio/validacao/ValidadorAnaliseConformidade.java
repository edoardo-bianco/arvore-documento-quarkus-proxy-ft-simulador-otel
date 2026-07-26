package br.gov.caixa.simtr.hub.conformidade.dominio.validacao;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ValidadorAnaliseConformidade {

    public ResultadoAnaliseConformidade validarResultado(
            Checklist checklist,
            ResultadoAnaliseConformidade resultado) {
        Map<Long, ApontamentoChecklist> esperados = indexarChecklist(checklist);
        if (resultado == null) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O resultado da análise é obrigatório");
        }
        if (resultado.origem() == OrigemResultado.REVISAO_HUMANA) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O resultado preliminar deve ser produzido pelo agente ou fallback");
        }
        if (!Objects.equals(checklist.identificadorNegocial(), resultado.identificadorChecklist())
                || !Objects.equals(checklist.versao(), resultado.versaoChecklist())
                || !Objects.equals(checklist.nome(), resultado.nomeChecklist())) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O resultado não corresponde ao checklist consultado");
        }

        Map<Long, ResultadoApontamentoConformidade> recebidos =
                indexarResultado(resultado.apontamentos());
        if (recebidos.size() != esperados.size()
                || !recebidos.keySet().equals(esperados.keySet())) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O resultado deve cobrir exatamente os apontamentos do checklist");
        }
        esperados.forEach((id, esperado) -> validarNome(
                esperado.nome(),
                recebidos.get(id).nomeApontamento(),
                false));

        List<ResultadoApontamentoConformidade> ordenados = checklist.apontamentos().stream()
                .sorted(Comparator.comparing(
                        ApontamentoChecklist::sequenciaApresentacao,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(apontamento -> recebidos.get(apontamento.identificadorNegocial()))
                .toList();
        return copiarResultado(resultado, ordenados, resultado.origem());
    }

    public ResultadoAnaliseConformidade consolidarRevisao(
            ResultadoAnaliseConformidade preliminar,
            RevisaoHumanaConformidade revisao) {
        if (preliminar == null || revisao == null) {
            throw FalhaAnaliseConformidade.revisaoInconsistente(
                    "Resultado preliminar e revisão são obrigatórios");
        }
        if (preliminar.origem() == OrigemResultado.REVISAO_HUMANA) {
            throw FalhaAnaliseConformidade.revisaoInconsistente(
                    "Um resultado revisado não pode ser revisado novamente");
        }

        Map<Long, ResultadoApontamentoConformidade> esperados =
                indexarRevisao(preliminar.apontamentos());
        Map<Long, ResultadoApontamentoConformidade> revisados =
                indexarRevisao(revisao.apontamentos());
        if (revisados.size() != esperados.size()
                || !revisados.keySet().equals(esperados.keySet())) {
            throw FalhaAnaliseConformidade.revisaoInconsistente(
                    "A revisão deve cobrir exatamente o resultado preliminar");
        }

        List<ResultadoApontamentoConformidade> ordenados = new ArrayList<>(esperados.size());
        for (ResultadoApontamentoConformidade esperado : preliminar.apontamentos()) {
            ResultadoApontamentoConformidade revisado =
                    revisados.get(esperado.identificadorApontamento());
            validarNome(esperado.nomeApontamento(), revisado.nomeApontamento(), true);
            if (Double.compare(esperado.confianca(), revisado.confianca()) != 0) {
                throw FalhaAnaliseConformidade.revisaoInconsistente(
                        "A confiança do apontamento é somente leitura");
            }
            ordenados.add(revisado);
        }
        return copiarResultado(preliminar, ordenados, OrigemResultado.REVISAO_HUMANA);
    }

    public void validarChecklist(Checklist checklist) {
        indexarChecklist(checklist);
    }

    private static Map<Long, ApontamentoChecklist> indexarChecklist(Checklist checklist) {
        if (checklist == null
                || checklist.identificadorNegocial() == null
                || checklist.identificadorNegocial() <= 0
                || checklist.versao() == null
                || checklist.versao() <= 0
                || checklist.nome() == null
                || checklist.nome().isBlank()
                || checklist.apontamentos() == null
                || checklist.apontamentos().isEmpty()) {
            throw FalhaAnaliseConformidade.checklistInvalido(
                    "O checklist consultado é inválido ou não possui apontamentos");
        }

        Map<Long, ApontamentoChecklist> indexados = new HashMap<>();
        for (ApontamentoChecklist apontamento : checklist.apontamentos()) {
            if (apontamento == null
                    || apontamento.identificadorNegocial() == null
                    || apontamento.identificadorNegocial() <= 0
                    || apontamento.nome() == null
                    || apontamento.nome().isBlank()
                    || indexados.put(apontamento.identificadorNegocial(), apontamento) != null) {
                throw FalhaAnaliseConformidade.checklistInvalido(
                        "O checklist contém apontamento inválido ou duplicado");
            }
        }
        return indexados;
    }

    private static Map<Long, ResultadoApontamentoConformidade> indexarResultado(
            List<ResultadoApontamentoConformidade> apontamentos) {
        Map<Long, ResultadoApontamentoConformidade> indexados = new HashMap<>();
        for (ResultadoApontamentoConformidade apontamento : apontamentos) {
            if (indexados.put(apontamento.identificadorApontamento(), apontamento) != null) {
                throw FalhaAnaliseConformidade.resultadoInvalido(
                        "O resultado contém apontamento duplicado");
            }
        }
        return indexados;
    }

    private static Map<Long, ResultadoApontamentoConformidade> indexarRevisao(
            List<ResultadoApontamentoConformidade> apontamentos) {
        Map<Long, ResultadoApontamentoConformidade> indexados = new HashMap<>();
        for (ResultadoApontamentoConformidade apontamento : apontamentos) {
            if (indexados.put(apontamento.identificadorApontamento(), apontamento) != null) {
                throw FalhaAnaliseConformidade.revisaoInconsistente(
                        "A revisão contém apontamento duplicado");
            }
        }
        return indexados;
    }

    private static void validarNome(String esperado, String recebido, boolean revisao) {
        if (!Objects.equals(esperado, recebido)) {
            if (revisao) {
                throw FalhaAnaliseConformidade.revisaoInconsistente(
                        "O nome do apontamento é imutável");
            }
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O nome do apontamento não corresponde ao checklist");
        }
    }

    private static ResultadoAnaliseConformidade copiarResultado(
            ResultadoAnaliseConformidade base,
            List<ResultadoApontamentoConformidade> apontamentos,
            OrigemResultado origem) {
        return new ResultadoAnaliseConformidade(
                base.identificadorChecklist(),
                base.versaoChecklist(),
                base.nomeChecklist(),
                base.resumo(),
                apontamentos,
                origem);
    }
}
