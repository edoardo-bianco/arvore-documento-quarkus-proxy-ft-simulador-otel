package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.mapper;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoApontamentoAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro.SaidaAgenteInvalidaException;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.validacao.ValidadorAnaliseConformidade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ResultadoAnaliseAgenteMapper {

    static final String JUSTIFICATIVA_NAO_ANALISADO =
            "O agente não retornou resultado para este apontamento";
    static final String RESUMO_FALLBACK =
            "Análise automática indisponível; revisão humana necessária";
    static final String JUSTIFICATIVA_FALLBACK =
            "Análise automática indisponível; item encaminhado para revisão humana";

    private final ObjectMapper objectMapper;
    private final ValidadorAnaliseConformidade validador =
            new ValidadorAnaliseConformidade();

    @Inject
    public ResultadoAnaliseAgenteMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serializarEntrada(EntradaAnaliseAgente entrada) {
        if (entrada == null) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "A entrada da análise é obrigatória");
        }
        validador.validarChecklist(entrada.checklist());
        try {
            return objectMapper.writeValueAsString(projetar(entrada));
        } catch (JsonProcessingException excecao) {
            throw new SaidaAgenteInvalidaException(excecao);
        }
    }

    public ResultadoAnaliseConformidade mapearResultado(
            Checklist checklist,
            ResultadoAnaliseAgente saida) {
        validador.validarChecklist(checklist);
        if (saida == null || saida.apontamentos() == null) {
            throw new SaidaAgenteInvalidaException();
        }

        Map<Long, ApontamentoChecklist> esperados = checklist.apontamentos().stream()
                .collect(Collectors.toMap(
                        ApontamentoChecklist::identificadorNegocial,
                        apontamento -> apontamento));
        Map<Long, ResultadoApontamentoConformidade> recebidos = new HashMap<>();
        Set<Long> identificadores = new HashSet<>();
        try {
            for (ResultadoApontamentoAgente apontamento : saida.apontamentos()) {
                validarApontamentoRecebido(apontamento, esperados, identificadores);
                ApontamentoChecklist esperado =
                        esperados.get(apontamento.identificadorApontamento());
                recebidos.put(
                        apontamento.identificadorApontamento(),
                        mapearApontamento(esperado, apontamento));
            }

            List<ResultadoApontamentoConformidade> completos =
                    completarEOrdenar(checklist, recebidos);
            ResultadoAnaliseConformidade resultado = new ResultadoAnaliseConformidade(
                    checklist.identificadorNegocial(),
                    checklist.versao(),
                    checklist.nome(),
                    saida.resumo(),
                    completos,
                    OrigemResultado.AGENTE);
            return validador.validarResultado(checklist, resultado);
        } catch (FalhaAnaliseConformidade excecao) {
            throw new SaidaAgenteInvalidaException(excecao);
        }
    }

    public ResultadoAnaliseConformidade criarFallback(Checklist checklist) {
        validador.validarChecklist(checklist);
        List<ResultadoApontamentoConformidade> apontamentos = checklist.apontamentos().stream()
                .map(apontamento -> new ResultadoApontamentoConformidade(
                        apontamento.identificadorNegocial(),
                        apontamento.nome(),
                        ParecerConformidade.NAO_ANALISADO,
                        JUSTIFICATIVA_FALLBACK,
                        null,
                        0.0d))
                .toList();
        ResultadoAnaliseConformidade fallback = new ResultadoAnaliseConformidade(
                checklist.identificadorNegocial(),
                checklist.versao(),
                checklist.nome(),
                RESUMO_FALLBACK,
                apontamentos,
                OrigemResultado.FALLBACK_TECNICO);
        return validador.validarResultado(checklist, fallback);
    }

    private static EntradaPrompt projetar(EntradaAnaliseAgente entrada) {
        Checklist checklist = entrada.checklist();
        List<ApontamentoPrompt> apontamentos = checklist.apontamentos().stream()
                .map(apontamento -> new ApontamentoPrompt(
                        apontamento.identificadorNegocial(),
                        apontamento.nome(),
                        apontamento.descricao(),
                        apontamento.orientacaoOperador(),
                        apontamento.indicadorReanalise(),
                        apontamento.sequenciaApresentacao()))
                .toList();
        return new EntradaPrompt(
                entrada.texto(),
                new ChecklistPrompt(
                        checklist.identificadorNegocial(),
                        checklist.versao(),
                        checklist.nome(),
                        checklist.orientacaoOperador(),
                        apontamentos));
    }

    private static void validarApontamentoRecebido(
            ResultadoApontamentoAgente recebido,
            Map<Long, ApontamentoChecklist> esperados,
            Set<Long> identificadores) {
        if (recebido == null
                || recebido.identificadorApontamento() == null
                || !identificadores.add(recebido.identificadorApontamento())) {
            throw new SaidaAgenteInvalidaException();
        }
        ApontamentoChecklist esperado =
                esperados.get(recebido.identificadorApontamento());
        if (esperado == null
                || !Objects.equals(esperado.nome(), recebido.nomeApontamento())
                || recebido.parecer() == null) {
            throw new SaidaAgenteInvalidaException();
        }
    }

    private static ResultadoApontamentoConformidade mapearApontamento(
            ApontamentoChecklist esperado,
            ResultadoApontamentoAgente recebido) {
        return new ResultadoApontamentoConformidade(
                esperado.identificadorNegocial(),
                esperado.nome(),
                ParecerConformidade.valueOf(recebido.parecer().name()),
                recebido.justificativa(),
                recebido.evidencia(),
                recebido.confianca());
    }

    private static List<ResultadoApontamentoConformidade> completarEOrdenar(
            Checklist checklist,
            Map<Long, ResultadoApontamentoConformidade> recebidos) {
        List<ApontamentoChecklist> ordenados = checklist.apontamentos().stream()
                .sorted(Comparator.comparing(
                        ApontamentoChecklist::sequenciaApresentacao,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<ResultadoApontamentoConformidade> completos =
                new ArrayList<>(ordenados.size());
        for (ApontamentoChecklist apontamento : ordenados) {
            ResultadoApontamentoConformidade resultado =
                    recebidos.get(apontamento.identificadorNegocial());
            if (resultado == null) {
                resultado = new ResultadoApontamentoConformidade(
                        apontamento.identificadorNegocial(),
                        apontamento.nome(),
                        ParecerConformidade.NAO_ANALISADO,
                        JUSTIFICATIVA_NAO_ANALISADO,
                        null,
                        0.0d);
            }
            completos.add(resultado);
        }
        return List.copyOf(completos);
    }

    private record EntradaPrompt(String texto, ChecklistPrompt checklist) {
    }

    private record ChecklistPrompt(
            Long identificador,
            Integer versao,
            String nome,
            String orientacaoOperador,
            List<ApontamentoPrompt> apontamentos) {
    }

    private record ApontamentoPrompt(
            Long identificador,
            String nome,
            String descricao,
            String orientacaoOperador,
            Boolean indicadorReanalise,
            Integer sequenciaApresentacao) {
    }
}
