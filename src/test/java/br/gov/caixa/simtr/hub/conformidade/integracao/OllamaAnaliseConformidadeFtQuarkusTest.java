package br.gov.caixa.simtr.hub.conformidade.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.agent.AgenteAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ParecerConformidadeAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoApontamentoAgente;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.AnalisarTextoComChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import dev.langchain4j.exception.RetriableException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@QuarkusTest
@TestProfile(OllamaAnaliseConformidadeFtQuarkusTest.Perfil.class)
class OllamaAnaliseConformidadeFtQuarkusTest {

    @Inject
    AnalisarTextoComChecklist analisarTexto;

    @Inject
    AgenteControlado agente;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void limparSpans() {
        ((OpenTelemetrySdk) openTelemetry)
                .getSdkTracerProvider()
                .forceFlush()
                .join(10, TimeUnit.SECONDS);
        exporter.reset();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void aplicaRetryFallbackCircuitBreakerERecuperacaoSemChamarOllama()
            throws InterruptedException {
        agente.falharAntesDoSucesso(2);

        var recuperadoPorRetry = analisarTexto.analisar("instancia-ft", entrada());

        assertEquals(OrigemResultado.AGENTE, recuperadoPorRetry.origem());
        assertEquals(3, agente.invocacoes());

        agente.falharSempre();
        var abriuCircuito = analisarTexto.analisar("instancia-ft", entrada());

        assertEquals(OrigemResultado.FALLBACK_TECNICO, abriuCircuito.origem());
        assertEquals(1, agente.invocacoes());

        var curtoCircuito = analisarTexto.analisar("instancia-ft", entrada());

        assertEquals(OrigemResultado.FALLBACK_TECNICO, curtoCircuito.origem());
        assertEquals(1, agente.invocacoes());

        CountDownLatch janelaCircuitBreaker = new CountDownLatch(1);
        assertFalse(janelaCircuitBreaker.await(10_200, TimeUnit.MILLISECONDS));
        agente.responderComSucesso();

        assertEquals(
                OrigemResultado.AGENTE,
                analisarTexto.analisar("instancia-ft", entrada()).origem());
        assertEquals(
                OrigemResultado.AGENTE,
                analisarTexto.analisar("instancia-ft", entrada()).origem());
        assertEquals(
                OrigemResultado.AGENTE,
                analisarTexto.analisar("instancia-ft", entrada()).origem());
        assertEquals(3, agente.invocacoes());

        agente.responderComSaidaDuplicada();
        var saidaInvalida = analisarTexto.analisar("instancia-ft", entrada());

        assertEquals(OrigemResultado.FALLBACK_TECNICO, saidaInvalida.origem());
        assertEquals(1, agente.invocacoes());

        agente.responderComSucesso();
        EntradaAnaliseAgente entradaInvalida = entrada();
        assertThrows(
                FalhaAnaliseConformidade.class,
                () -> analisarTexto.analisar("", entradaInvalida));
        assertEquals(0, agente.invocacoes());

        validarSpansSemConteudoSensivel();
    }

    private void validarSpansSemConteudoSensivel() {
        ((OpenTelemetrySdk) openTelemetry)
                .getSdkTracerProvider()
                .forceFlush()
                .join(10, TimeUnit.SECONDS);
        List<SpanData> spans = exporter.getFinishedSpanItems().stream()
                .filter(span -> span.getName().equals("simtr-hub.flow.conformidade.analise")
                        || span.getName().equals("simtr-hub.agent.conformidade.analisar"))
                .toList();
        Set<String> identificadoresFlow = spans.stream()
                .filter(span -> span.getName().equals("simtr-hub.flow.conformidade.analise"))
                .map(SpanData::getSpanId)
                .collect(Collectors.toSet());

        assertTrue(spans.stream().anyMatch(span ->
                span.getName().equals("simtr-hub.flow.conformidade.analise")
                        && span.getKind() == SpanKind.INTERNAL));
        assertTrue(spans.stream().anyMatch(span ->
                span.getName().equals("simtr-hub.agent.conformidade.analisar")
                        && span.getKind() == SpanKind.CLIENT
                        && identificadoresFlow.contains(span.getParentSpanId())));
        assertTrue(spans.stream()
                .flatMap(span -> span.getEvents().stream())
                .anyMatch(evento ->
                        evento.getName().equals("conformidade.agente.fallback.aplicado")));
        assertFalse(spans.stream()
                .flatMap(span -> span.getEvents().stream())
                .anyMatch(evento -> evento.getName().equals("exception")));
        assertFalse(spans.stream()
                .flatMap(span -> span.getAttributes().asMap().keySet().stream())
                .map(chave -> chave.getKey().toLowerCase())
                .anyMatch(OllamaAnaliseConformidadeFtQuarkusTest::atributoSensivel));
    }

    private static boolean atributoSensivel(String chave) {
        return chave.contains("texto")
                || chave.contains("prompt")
                || chave.contains("response")
                || chave.contains("evidencia")
                || chave.contains("credential")
                || chave.contains("stack");
    }

    private static EntradaAnaliseAgente entrada() {
        return new EntradaAnaliseAgente(
                "Texto documental",
                new Checklist(
                        "Checklist documental",
                        1000012583L,
                        1,
                        null,
                        null,
                        false,
                        "Orientação",
                        List.of(new ApontamentoChecklist(
                                10L,
                                "Documento identificado",
                                "Verificar documento",
                                "Conferir conteúdo",
                                false,
                                1))));
    }

    public static final class Perfil implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(AgenteControlado.class);
        }
    }

    @Alternative
    @ApplicationScoped
    public static class AgenteControlado implements AgenteAnaliseConformidade {

        private enum Comportamento {
            SUCESSO,
            FALHA,
            DUPLICADO
        }

        private final AtomicInteger invocacoes = new AtomicInteger();
        private volatile int falhasRestantes;
        private volatile Comportamento comportamento = Comportamento.SUCESSO;

        public void falharAntesDoSucesso(int quantidade) {
            invocacoes.set(0);
            falhasRestantes = quantidade;
            comportamento = Comportamento.SUCESSO;
        }

        public void falharSempre() {
            invocacoes.set(0);
            falhasRestantes = 0;
            comportamento = Comportamento.FALHA;
        }

        public void responderComSucesso() {
            invocacoes.set(0);
            falhasRestantes = 0;
            comportamento = Comportamento.SUCESSO;
        }

        public void responderComSaidaDuplicada() {
            invocacoes.set(0);
            falhasRestantes = 0;
            comportamento = Comportamento.DUPLICADO;
        }

        @Override
        public ResultadoAnaliseAgente analisar(
                String identificadorMemoria,
                String entradaJson) {
            invocacoes.incrementAndGet();
            if (comportamento == Comportamento.FALHA || falhasRestantes-- > 0) {
                throw new RetriableException("falha sintética");
            }
            ResultadoApontamentoAgente apontamento =
                    new ResultadoApontamentoAgente(
                            10L,
                            "Documento identificado",
                            ParecerConformidadeAgente.CONFORME,
                            "Justificativa sintética",
                            "Evidência sintética",
                            0.9d);
            if (comportamento == Comportamento.DUPLICADO) {
                return new ResultadoAnaliseAgente(
                        "Resumo sintético",
                        List.of(apontamento, apontamento));
            }
            return new ResultadoAnaliseAgente(
                    "Resumo sintético",
                    List.of(apontamento));
        }

        public int invocacoes() {
            return invocacoes.get();
        }
    }
}
