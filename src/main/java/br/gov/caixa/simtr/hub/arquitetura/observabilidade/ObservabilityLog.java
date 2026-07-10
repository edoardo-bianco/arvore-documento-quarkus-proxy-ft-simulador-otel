package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ObservabilityLog {

    private ObservabilityLog() {
    }

    public static void info(Logger logger, String evento, Map<String, ?> campos) {
        withMdc(evento, campos, () -> logger.info(evento));
    }

    public static void error(Logger logger, String evento, Throwable throwable, Map<String, ?> campos) {
        withMdc(evento, campos, () -> logger.error(evento, throwable));
    }

    public static Map<String, Object> fields(Object... chaveValor) {
        Map<String, Object> campos = new LinkedHashMap<>();
        if (chaveValor == null) {
            return campos;
        }
        if (chaveValor.length % 2 != 0) {
            throw new IllegalArgumentException("A lista de campos deve possuir pares chave/valor.");
        }
        for (int i = 0; i < chaveValor.length; i += 2) {
            Object chave = chaveValor[i];
            Object valor = chaveValor[i + 1];
            if (chave != null && valor != null) {
                campos.put(String.valueOf(chave), valor);
            }
        }
        return campos;
    }

    private static void withMdc(String evento, Map<String, ?> campos, Runnable logAction) {
        Map<String, String> aplicados = new LinkedHashMap<>();
        try {
            put(aplicados, "evento", evento);
            putTraceContext(aplicados);

            if (campos != null) {
                campos.forEach((chave, valor) -> {
                    if (chave != null && valor != null) {
                        put(aplicados, String.valueOf(chave), String.valueOf(valor));
                    }
                });
            }

            logAction.run();
        } finally {
            aplicados.keySet().forEach(MDC::remove);
        }
    }

    private static void putTraceContext(Map<String, String> aplicados) {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            put(aplicados, "traceId", spanContext.getTraceId());
            put(aplicados, "spanId", spanContext.getSpanId());
            put(aplicados, "traceSampled", String.valueOf(spanContext.isSampled()));
        }
    }

    private static void put(Map<String, String> aplicados, String chave, String valor) {
        MDC.put(chave, valor);
        aplicados.put(chave, valor);
    }
}
