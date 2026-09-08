package br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.handlers.ConsoleHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CapturaConsoleRuntime implements AutoCloseable {

    private final ConsoleHandler console;
    private final ConsoleHandler.Target destinoOriginal;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    CapturaConsoleRuntime() {
        var consoles = handlers().stream().filter(ConsoleHandler.class::isInstance)
                .map(ConsoleHandler.class::cast).toList();
        assertEquals(1, consoles.size(), "A prova requer o console real unico do profile.");
        console = consoles.getFirst();
        boolean stderr = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.log.console.stderr", Boolean.class).orElse(false);
        destinoOriginal = stderr ? ConsoleHandler.Target.SYSTEM_ERR : ConsoleHandler.Target.SYSTEM_OUT;
        console.flush();
        console.setOutputStream(bytes);
    }

    String saida() {
        console.flush();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    ConsoleHandler console() {
        return console;
    }

    @Override
    public void close() {
        console.flush();
        console.setTarget(destinoOriginal);
    }

    static List<Handler> handlers() {
        var encontrados = new ArrayList<Handler>();
        Set<Handler> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        buscar(Logger.getLogger("").getHandlers(), encontrados, visitados);
        return encontrados;
    }

    private static void buscar(Handler[] handlers, List<Handler> encontrados, Set<Handler> visitados) {
        for (Handler handler : handlers) {
            if (visitados.add(handler)) {
                encontrados.add(handler);
                if (handler instanceof ExtHandler intermediario) {
                    buscar(intermediario.getHandlers(), encontrados, visitados);
                }
            }
        }
    }
}
