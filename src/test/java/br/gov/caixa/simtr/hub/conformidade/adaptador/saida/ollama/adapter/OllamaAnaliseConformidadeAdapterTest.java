package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.agent.AgenteAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ParecerConformidadeAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoApontamentoAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro.FalhaTecnicaAgenteNaoRetriavelException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro.FalhaTecnicaAgenteTransitoriaException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.mapper.ResultadoAnaliseAgenteMapper;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.exception.RetriableException;
import dev.langchain4j.service.output.OutputParsingException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.Test;

class OllamaAnaliseConformidadeAdapterTest {

    @Test
    void usaInstanceIdComoMemoriaEMapeiaSaidaEstruturada() {
        AtomicReference<String> memoriaRecebida = new AtomicReference<>();
        AtomicReference<String> entradaRecebida = new AtomicReference<>();
        AgenteAnaliseConformidade agente = (memoria, entradaJson) -> {
            memoriaRecebida.set(memoria);
            entradaRecebida.set(entradaJson);
            return saidaValida();
        };
        var adapter = adapter(agente);

        var resultado = adapter.analisar(
                "instancia-123",
                new EntradaAnaliseAgente("Texto sob análise", checklist()));

        assertEquals("instancia-123", memoriaRecebida.get());
        assertTrue(entradaRecebida.get().contains("\"texto\":\"Texto sob análise\""));
        assertEquals(OrigemResultado.AGENTE, resultado.origem());
        assertEquals(1, resultado.apontamentos().size());
    }

    @Test
    void classificaFalhasTransitoriasEParsingParaRetry() {
        var falhaHttp = adapter((memoria, entrada) -> {
            throw new RetriableException("indisponível");
        });
        var falhaParsing = adapter((memoria, entrada) -> {
            throw new OutputParsingException(
                    "resposta truncada",
                    new IllegalArgumentException("json"));
        });
        EntradaAnaliseAgente entrada =
                new EntradaAnaliseAgente("Texto sob análise", checklist());

        assertThrows(
                FalhaTecnicaAgenteTransitoriaException.class,
                () -> falhaHttp.analisar("instancia-123", entrada));
        assertThrows(
                FalhaTecnicaAgenteTransitoriaException.class,
                () -> falhaParsing.analisar("instancia-123", entrada));
    }

    @Test
    void classificaRequisicaoInvalidaDoProviderSemRetry() {
        var adapter = adapter((memoria, entrada) -> {
            throw new InvalidRequestException("requisição rejeitada");
        });
        EntradaAnaliseAgente entrada =
                new EntradaAnaliseAgente("Texto sob análise", checklist());

        assertThrows(
                FalhaTecnicaAgenteNaoRetriavelException.class,
                () -> adapter.analisar("instancia-123", entrada));
    }

    @Test
    void declaraPoliticasFtEspanExatamenteComoAprovado() throws Exception {
        Method metodo = OllamaAnaliseConformidadeAdapter.class.getMethod(
                "analisar",
                String.class,
                EntradaAnaliseAgente.class);

        Timeout timeout = metodo.getAnnotation(Timeout.class);
        Retry retry = metodo.getAnnotation(Retry.class);
        CircuitBreaker circuitBreaker = metodo.getAnnotation(CircuitBreaker.class);
        Fallback fallback = metodo.getAnnotation(Fallback.class);
        WithSpan withSpan = metodo.getAnnotation(WithSpan.class);

        assertEquals(65L, timeout.value());
        assertEquals(ChronoUnit.SECONDS, timeout.unit());
        assertEquals(2, retry.maxRetries());
        assertEquals(500L, retry.delay());
        assertEquals(ChronoUnit.MILLIS, retry.delayUnit());
        assertEquals(200L, retry.jitter());
        assertEquals(ChronoUnit.MILLIS, retry.jitterDelayUnit());
        assertArrayEquals(
                new Class<?>[] {
                    FalhaTecnicaAgenteTransitoriaException.class,
                    org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class
                },
                retry.retryOn());
        assertEquals(4, circuitBreaker.requestVolumeThreshold());
        assertEquals(0.5d, circuitBreaker.failureRatio());
        assertEquals(10L, circuitBreaker.delay());
        assertEquals(ChronoUnit.SECONDS, circuitBreaker.delayUnit());
        assertEquals(2, circuitBreaker.successThreshold());
        assertEquals("fallback", fallback.fallbackMethod());
        assertNull(withSpan);
        assertNotNull(fallback.applyOn());
    }

    private static OllamaAnaliseConformidadeAdapter adapter(
            AgenteAnaliseConformidade agente) {
        return new OllamaAnaliseConformidadeAdapter(
                agente,
                new ResultadoAnaliseAgenteMapper(new ObjectMapper()),
                OpenTelemetry.noop().getTracer("teste"),
                "llama3.2:3b");
    }

    private static ResultadoAnaliseAgente saidaValida() {
        return new ResultadoAnaliseAgente(
                "Resumo",
                List.of(new ResultadoApontamentoAgente(
                        1L,
                        "Primeiro",
                        ParecerConformidadeAgente.CONFORME,
                        "Justificativa",
                        "Evidência objetiva",
                        0.9d)));
    }

    private static Checklist checklist() {
        return new Checklist(
                "Checklist exemplo",
                1000012583L,
                1,
                null,
                null,
                false,
                "Orientação",
                List.of(new ApontamentoChecklist(
                        1L,
                        "Primeiro",
                        "Descrição",
                        "Orientação",
                        false,
                        1)));
    }
}
