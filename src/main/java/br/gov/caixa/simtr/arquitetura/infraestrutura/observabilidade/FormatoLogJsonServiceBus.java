package br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade;

import io.quarkus.jsonp.JsonProviderHolder;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.JsonFormatter;

final class FormatoLogJsonServiceBus extends ExtFormatter {

    private static final List<String> CATEGORIAS = List.of(
            "br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.",
            "br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.",
            "br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.",
            "br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.");

    private final JsonFormatter original;

    FormatoLogJsonServiceBus(JsonFormatter original) {
        this.original = original;
    }

    @Override
    public String format(ExtLogRecord registro) {
        String categoria = registro.getLoggerName();
        if (!(registro.getMarker() instanceof CamposLogJson(var campos))
                || categoria == null || CATEGORIAS.stream().noneMatch(categoria::startsWith)) {
            return original.format(registro);
        }
        if (registro.getThrown() != null) {
            throw new IllegalArgumentException("Registro JSON marcado nao pode incluir Throwable original.");
        }
        String saida = original.format(registro);
        JsonObject metadados;
        try (var leitor = JsonProviderHolder.jsonProvider().createReader(new StringReader(saida))) {
            metadados = leitor.readObject();
        }
        if (campos.keySet().stream().anyMatch(metadados::containsKey)) {
            throw new IllegalArgumentException("Campos JSON nao podem sobrescrever metadados do logger.");
        }
        var objeto = JsonProviderHolder.jsonProvider().createObjectBuilder(metadados);
        campos.forEach(objeto::add);
        return objeto.build() + Objects.toString(original.getRecordDelimiter(), "");
    }

    @Override
    public boolean isCallerCalculationRequired() {
        return original.isCallerCalculationRequired();
    }

    @Override
    public String getHead(Handler handler) {
        return original.getHead(handler);
    }

    @Override
    public String getTail(Handler handler) {
        return original.getTail(handler);
    }

    JsonFormatter original() {
        return original;
    }
}
