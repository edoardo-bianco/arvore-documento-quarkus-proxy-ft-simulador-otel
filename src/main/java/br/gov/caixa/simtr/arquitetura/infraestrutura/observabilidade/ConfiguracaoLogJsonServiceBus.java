package br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.formatters.JsonFormatter;

@ApplicationScoped
public class ConfiguracaoLogJsonServiceBus {

    private final Map<Handler, FormatoLogJsonServiceBus> instalados = new IdentityHashMap<>();

    void iniciar(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent evento) {
        instalar(Logger.getLogger("").getHandlers());
    }

    void encerrar(@Observes ShutdownEvent evento) {
        restaurar();
    }

    void instalar(Handler... destinos) {
        instalar(destinos, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private void instalar(Handler[] destinos, Set<Handler> visitados) {
        for (Handler destino : destinos) {
            if (!visitados.add(destino)) {
                continue;
            }
            if (destino.getFormatter() instanceof JsonFormatter original) {
                var formato = new FormatoLogJsonServiceBus(original);
                destino.setFormatter(formato);
                instalados.put(destino, formato);
            }
            if (destino instanceof ExtHandler intermediario) {
                instalar(intermediario.getHandlers(), visitados);
            }
        }
    }

    void restaurar() {
        instalados.forEach((destino, formato) -> {
            if (destino.getFormatter() == formato) {
                destino.setFormatter(formato.original());
            }
        });
        instalados.clear();
    }
}
