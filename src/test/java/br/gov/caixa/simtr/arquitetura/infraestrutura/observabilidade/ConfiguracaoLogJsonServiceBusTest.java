package br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade;

import io.quarkus.logging.json.runtime.JsonFormatter;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.Map;
import java.util.logging.Logger;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@ResourceLock(Resources.SYSTEM_OUT)
@TestProfile(ConfiguracaoLogJsonServiceBusTest.LogJsonProfile.class)
class ConfiguracaoLogJsonServiceBusTest {

    @Inject
    BeanManager beanManager;

    public static final class LogJsonProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.log.file.enabled", "true",
                    "quarkus.log.file.path", "target/logs/servicebus-json-contrato-test.json",
                    "quarkus.log.file.json.enabled", "true",
                    "quarkus.log.console.json.enabled", "true");
        }
    }

    @Test
    void deveInstalarUmaVezEmHandlersIntermediariosERestaurar() {
        var configuracao = new ConfiguracaoLogJsonServiceBus();
        var original = new JsonFormatter();
        var destino = new ConsoleHandler(original);
        var textual = new ConsoleHandler(new PatternFormatter("%s%n"));
        var intermediario = new DelayedHandler();
        intermediario.setHandlers(new java.util.logging.Handler[]{destino, textual});
        var registro = FormatoLogJsonServiceBusTest.registro("br.gov.caixa.simtr.hub");
        var antes = original.format(registro);

        configuracao.instalar(intermediario, destino);
        var instalado = destino.getFormatter();
        configuracao.instalar(intermediario);

        assertInstanceOf(FormatoLogJsonServiceBus.class, instalado);
        assertSame(instalado, destino.getFormatter());
        assertInstanceOf(PatternFormatter.class, textual.getFormatter());
        assertEquals(antes, instalado.format(registro));
        configuracao.restaurar();
        assertSame(original, destino.getFormatter());
        configuracao.restaurar();
        assertSame(original, destino.getFormatter());
    }

    @Test
    void naoDeveDesfazerFormatterSubstituidoPorOutroResponsavel() {
        var configuracao = new ConfiguracaoLogJsonServiceBus();
        var destino = new ConsoleHandler(new JsonFormatter());
        configuracao.instalar(destino);
        var substituto = new JsonFormatter();
        destino.setFormatter(substituto);

        configuracao.restaurar();

        assertSame(substituto, destino.getFormatter());
    }

    @Test
    void deveEmitirUmaVezPorDestinoJsonRealSemAlterarEventoHub() throws Exception {
        String id = UUID.randomUUID().toString();
        String categoria = "br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.ProvaLog";
        var registro = FormatoLogJsonServiceBusTest.registro(categoria);
        registro.setMarker(FormatoLogJsonServiceBusTest.campos(id, 7));
        var hub = FormatoLogJsonServiceBusTest.registro("br.gov.caixa.simtr.hub");
        hub.setMessage("prova.hub." + id);
        hub.setMarker(FormatoLogJsonServiceBusTest.campos("nao-aplicar-" + id, 7));
        String console;
        try (var captura = new CapturaConsoleRuntime()) {
            Logger.getLogger(categoria).log(registro);
            Logger.getLogger("br.gov.caixa.simtr.hub").log(hub);
            console = captura.saida();
        }

        var linhas = Files.readAllLines(Path.of("target/logs/servicebus-json-contrato-test.json")).stream()
                .filter(linha -> linha.contains(id)).toList();
        assertEquals(2, linhas.size());
        var novo = FormatoLogJsonServiceBusTest.ler(linhas.getFirst());
        assertEquals(id, novo.getString("id_erro"));
        assertEquals(7, novo.getInt("linha_json"));
        assertEquals(1, novo.getJsonArray("erros").size());
        var existente = FormatoLogJsonServiceBusTest.ler(linhas.getLast());
        assertEquals("prova.hub." + id, existente.getString("message"));
        assertFalse(existente.containsKey("id_erro"));
        assertFalse(existente.containsKey("erros"));
        var linhasConsole = console.lines().filter(linha -> linha.contains(id)).toList();
        assertEquals(2, linhasConsole.size());
        assertEquals(novo, FormatoLogJsonServiceBusTest.ler(linhasConsole.getFirst()));
        assertEquals(existente, FormatoLogJsonServiceBusTest.ler(linhasConsole.getLast()));
    }

    @Test
    void deveRestaurarHandlersReaisPeloObserverCdiDeEncerramento() {
        var instalados = CapturaConsoleRuntime.handlers().stream()
                .filter(handler -> handler.getFormatter() instanceof FormatoLogJsonServiceBus).toList();
        assertEquals(2, instalados.size(), "Console e arquivo JSON devem estar decorados.");
        var originais = instalados.stream()
                .map(handler -> ((FormatoLogJsonServiceBus) handler.getFormatter()).original()).toList();
        var fim = new ShutdownEvent();
        var inicio = new StartupEvent();
        var encerrar = beanManager.resolveObserverMethods(fim).stream()
                .filter(observer -> observer.getBeanClass() == ConfiguracaoLogJsonServiceBus.class)
                .findFirst().orElseThrow();
        var iniciar = beanManager.resolveObserverMethods(inicio).stream()
                .filter(observer -> observer.getBeanClass() == ConfiguracaoLogJsonServiceBus.class)
                .findFirst().orElseThrow();

        try {
            encerrar.notify(fim);
            for (int indice = 0; indice < instalados.size(); indice++) {
                assertSame(originais.get(indice), instalados.get(indice).getFormatter());
            }
        } finally {
            iniciar.notify(inicio);
        }
        for (var handler : instalados) {
            assertInstanceOf(FormatoLogJsonServiceBus.class, handler.getFormatter());
        }
    }
}
