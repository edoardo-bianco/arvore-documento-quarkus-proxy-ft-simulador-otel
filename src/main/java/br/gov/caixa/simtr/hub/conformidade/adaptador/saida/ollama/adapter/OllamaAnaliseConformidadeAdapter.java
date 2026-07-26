package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.agent.AgenteAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro.FalhaTecnicaAgenteNaoRetriavelException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro.FalhaTecnicaAgenteTransitoriaException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro.SaidaAgenteInvalidaException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.mapper.ResultadoAnaliseAgenteMapper;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.AnalisarTextoComChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.RetriableException;
import dev.langchain4j.exception.UnresolvedModelServerException;
import dev.langchain4j.service.output.OutputParsingException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.EOFException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OllamaAnaliseConformidadeAdapter implements AnalisarTextoComChecklist {

    private static final Logger LOG =
            Logger.getLogger(OllamaAnaliseConformidadeAdapter.class);
    private static final String EVENTO_INICIADO =
            "conformidade.agente.chamada.iniciada";
    private static final String EVENTO_CONCLUIDO =
            "conformidade.agente.chamada.concluida";
    private static final String EVENTO_FALLBACK =
            "conformidade.agente.fallback.aplicado";
    private static final String ATRIBUTO_ORIGEM_RESULTADO =
            "conformidade.agente.origem_resultado";

    private final AgenteAnaliseConformidade agente;
    private final ResultadoAnaliseAgenteMapper mapper;
    private final Tracer tracer;
    private final String modelo;

    @Inject
    public OllamaAnaliseConformidadeAdapter(
            AgenteAnaliseConformidade agente,
            ResultadoAnaliseAgenteMapper mapper,
            Tracer tracer,
            @ConfigProperty(
                    name = "quarkus.langchain4j.ollama.chat-model.model-id",
                    defaultValue = "llama3.2:3b") String modelo) {
        this.agente = agente;
        this.mapper = mapper;
        this.tracer = tracer;
        this.modelo = modelo;
    }

    @Override
    @Timeout(value = 65, unit = ChronoUnit.SECONDS)
    @Retry(
            maxRetries = 2,
            delay = 500,
            delayUnit = ChronoUnit.MILLIS,
            jitter = 200,
            jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {
                FalhaTecnicaAgenteTransitoriaException.class,
                org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class
            },
            abortOn = {
                FalhaTecnicaAgenteNaoRetriavelException.class,
                SaidaAgenteInvalidaException.class,
                FalhaAnaliseConformidade.class
            })
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5d,
            delay = 10,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 2,
            failOn = {
                FalhaTecnicaAgenteTransitoriaException.class,
                org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class
            },
            skipOn = {
                FalhaTecnicaAgenteNaoRetriavelException.class,
                SaidaAgenteInvalidaException.class,
                FalhaAnaliseConformidade.class
            })
    @Fallback(
            fallbackMethod = "fallback",
            applyOn = {
                FalhaTecnicaAgenteTransitoriaException.class,
                FalhaTecnicaAgenteNaoRetriavelException.class,
                SaidaAgenteInvalidaException.class,
                org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class,
                CircuitBreakerOpenException.class
            },
            skipOn = FalhaAnaliseConformidade.class)
    public ResultadoAnaliseConformidade analisar(
            String identificadorMemoria,
            EntradaAnaliseAgente entrada) {
        Span span = tracer.spanBuilder("simtr-hub.flow.conformidade.analise")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try {
            Scope escopo = span.makeCurrent();
            try {
                return analisarNoEscopo(identificadorMemoria, entrada, span);
            } finally {
                escopo.close();
            }
        } finally {
            span.end();
        }
    }

    private ResultadoAnaliseConformidade analisarNoEscopo(
            String identificadorMemoria,
            EntradaAnaliseAgente entrada,
            Span span) {
        validarIdentificadorMemoria(identificadorMemoria);
        String entradaJson = mapper.serializarEntrada(entrada);
        Checklist checklist = entrada.checklist();
        registrarContexto(span, identificadorMemoria, checklist);
        registrarEvento(
                span,
                "conformidade.checklist.obtido",
                identificadorMemoria,
                checklist,
                null);

        ResultadoAnaliseAgente saida =
                invocarAgente(identificadorMemoria, entradaJson, checklist);

        ResultadoAnaliseConformidade resultado =
                mapper.mapearResultado(checklist, saida);
        span.setAttribute(
                ATRIBUTO_ORIGEM_RESULTADO,
                resultado.origem().name());
        span.setAttribute(
                "conformidade.analise.status",
                "RESULTADO_PRELIMINAR");
        return resultado;
    }

    private ResultadoAnaliseConformidade fallback(
            String identificadorMemoria,
            EntradaAnaliseAgente entrada) {
        Span span = tracer.spanBuilder("simtr-hub.flow.conformidade.analise")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try {
            Scope escopo = span.makeCurrent();
            try {
                validarIdentificadorMemoria(identificadorMemoria);
                ResultadoAnaliseConformidade resultado =
                        mapper.criarFallback(entrada.checklist());
                registrarContexto(span, identificadorMemoria, entrada.checklist());
                span.setAttribute(
                        ATRIBUTO_ORIGEM_RESULTADO,
                        resultado.origem().name());
                span.setAttribute(
                        "conformidade.analise.status",
                        "RESULTADO_PRELIMINAR");
                registrarEvento(
                        span,
                        EVENTO_FALLBACK,
                        identificadorMemoria,
                        entrada.checklist(),
                        resultado.origem().name());
                return resultado;
            } finally {
                escopo.close();
            }
        } finally {
            span.end();
        }
    }

    private ResultadoAnaliseAgente invocarAgente(
            String identificadorMemoria,
            String entradaJson,
            Checklist checklist) {
        Span spanAgente = tracer.spanBuilder("simtr-hub.agent.conformidade.analisar")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        registrarContexto(spanAgente, identificadorMemoria, checklist);
        try {
            Scope escopo = spanAgente.makeCurrent();
            try {
                registrarEvento(
                        spanAgente,
                        EVENTO_INICIADO,
                        identificadorMemoria,
                        checklist,
                        null);
                ResultadoAnaliseAgente saida =
                        agente.analisar(identificadorMemoria, entradaJson);
                registrarEvento(
                        spanAgente,
                        EVENTO_CONCLUIDO,
                        identificadorMemoria,
                        checklist,
                        null);
                return saida;
            } finally {
                escopo.close();
            }
        } catch (RuntimeException excecao) {
            throw classificar(excecao);
        } finally {
            spanAgente.end();
        }
    }

    private RuntimeException classificar(RuntimeException excecao) {
        if (ehTransitoria(excecao)) {
            return new FalhaTecnicaAgenteTransitoriaException(excecao);
        }
        return new FalhaTecnicaAgenteNaoRetriavelException(excecao);
    }

    private static boolean ehTransitoria(Throwable falha) {
        Throwable atual = falha;
        while (atual != null) {
            if (atual instanceof RetriableException
                    || atual instanceof OutputParsingException
                    || atual instanceof UnresolvedModelServerException
                    || atual instanceof ConnectException
                    || atual instanceof SocketTimeoutException
                    || atual instanceof HttpTimeoutException
                    || atual instanceof EOFException) {
                return true;
            }
            if (atual instanceof HttpException httpException) {
                int status = httpException.statusCode();
                return status == 408 || status == 425 || status == 429 || status >= 500;
            }
            atual = atual.getCause();
        }
        return false;
    }

    private static void validarIdentificadorMemoria(String identificadorMemoria) {
        if (identificadorMemoria == null || identificadorMemoria.isBlank()) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "O identificador da instância da análise é obrigatório");
        }
    }

    private void registrarContexto(
            Span span,
            String identificadorMemoria,
            Checklist checklist) {
        span.setAttribute(
                "conformidade.analise.instance_id",
                identificadorMemoria);
        span.setAttribute(
                "conformidade.checklist.identificador",
                checklist.identificadorNegocial());
        span.setAttribute(
                "conformidade.checklist.versao",
                checklist.versao().longValue());
        span.setAttribute(
                "conformidade.checklist.quantidade_apontamentos",
                checklist.apontamentos().size());
        span.setAttribute("conformidade.agente.modelo", modelo);
    }

    private void registrarEvento(
            Span span,
            String evento,
            String identificadorMemoria,
            Checklist checklist,
            String origemResultado) {
        span.addEvent(evento);
        Map<String, Object> campos = ObservabilityLog.fields(
                "conformidade.analise.instance_id", identificadorMemoria,
                "conformidade.checklist.identificador", checklist.identificadorNegocial(),
                "conformidade.checklist.versao", checklist.versao(),
                "conformidade.checklist.quantidade_apontamentos",
                        checklist.apontamentos().size(),
                "conformidade.agente.modelo", modelo,
                ATRIBUTO_ORIGEM_RESULTADO, origemResultado);
        ObservabilityLog.info(LOG, evento, campos);
    }
}
