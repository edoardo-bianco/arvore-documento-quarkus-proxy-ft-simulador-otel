package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
        .ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
        .ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise
        .SolicitacaoAnaliseConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class ContextoAnaliseConformidadeFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void criaEnvelopeInicialSomenteComIdentidades() throws Exception {
        var solicitacao = solicitacao();

        var contexto = ContextoAnaliseConformidadeFlow.inicial(solicitacao);
        var json = objectMapper.valueToTree(contexto);

        assertEquals(solicitacao.correlationId(), contexto.correlationId());
        assertEquals(solicitacao.identificadorDocumento(), contexto.identificadorDocumento());
        assertEquals(solicitacao.identificadorChecklist(), contexto.identificadorChecklist());
        assertEquals(solicitacao.versaoChecklist(), contexto.versaoChecklist());
        assertNull(contexto.checklistRef());
        assertEquals(
                Set.of(
                        "correlationId",
                        "identificadorDocumento",
                        "identificadorChecklist",
                        "versaoChecklist",
                        "checklistRef"),
                StreamSupport.stream(
                                ((Iterable<String>) json::fieldNames).spliterator(),
                                false)
                        .collect(Collectors.toSet()));
        String serializado = objectMapper.writeValueAsString(contexto);
        assertFalse(serializado.contains(solicitacao.texto()));
        assertFalse(serializado.contains("apontamentos"));
        assertFalse(serializado.contains("resultado"));
        assertFalse(serializado.contains("revisao"));
    }

    @Test
    void acrescentaSomenteAReferenciaDoChecklist() {
        var solicitacao = solicitacao();
        var referencia = new ReferenciasDocumentoAnaliseConformidade(objectMapper)
                .checklist(solicitacao.correlationId(), checklist());

        var contexto = ContextoAnaliseConformidadeFlow.inicial(solicitacao)
                .comChecklist(referencia);

        assertEquals(referencia, contexto.checklistRef());
    }

    @ParameterizedTest
    @MethodSource("identidadesInvalidas")
    void rejeitaEnvelopeComIdentidadeInvalida(
            String correlationId,
            String identificadorDocumento,
            Long identificadorChecklist,
            Integer versaoChecklist) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ContextoAnaliseConformidadeFlow(
                        correlationId,
                        identificadorDocumento,
                        identificadorChecklist,
                        versaoChecklist,
                        null));
    }

    @Test
    void rejeitaSolicitacaoAusente() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ContextoAnaliseConformidadeFlow.inicial(null));
    }

    @Test
    void rejeitaReferenciaChecklistDeOutraCorrelacao() {
        var contexto = ContextoAnaliseConformidadeFlow.inicial(solicitacao());
        var referenciaAlheia = new ReferenciaDocumentoAnaliseConformidade(
                "checklist-alheio",
                "0".repeat(64),
                ReferenciaDocumentoAnaliseConformidade.VERSAO_INICIAL);

        assertThrows(
                IllegalArgumentException.class,
                () -> contexto.comChecklist(referenciaAlheia));
    }

    private static Stream<Arguments> identidadesInvalidas() {
        return Stream.of(
                Arguments.of(null, "DOC-1", 1L, 1),
                Arguments.of(" ", "DOC-1", 1L, 1),
                Arguments.of("correlation-1", null, 1L, 1),
                Arguments.of("correlation-1", " ", 1L, 1),
                Arguments.of("correlation-1", "DOC-1", null, 1),
                Arguments.of("correlation-1", "DOC-1", 0L, 1),
                Arguments.of("correlation-1", "DOC-1", 1L, null),
                Arguments.of("correlation-1", "DOC-1", 1L, 0));
    }

    private static SolicitacaoAnaliseConformidade solicitacao() {
        return new SolicitacaoAnaliseConformidade(
                "correlation-contexto-referencial",
                "DOC-CONTEXTO-1",
                "Texto que nunca pode chegar ao checkpoint",
                1000012583L,
                1);
    }

    private static Checklist checklist() {
        return new Checklist(
                "Checklist referencial",
                1000012583L,
                1,
                "2026-08-03T00:00:00Z",
                "2026-08-03T00:00:00Z",
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
}
