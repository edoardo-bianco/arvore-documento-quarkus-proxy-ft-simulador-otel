package br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade;

import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@ResourceLock(Resources.SYSTEM_OUT)
class ConsoleTextualServiceBusTest {

    @Test
    void devePreservarConsoleTextualRealParaNovoEventoEHub() {
        String id = UUID.randomUUID().toString();
        String categoria = "br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.ProvaTexto";
        var novo = FormatoLogJsonServiceBusTest.registro(categoria);
        novo.setMessage("prova.novo." + id);
        novo.setMarker(FormatoLogJsonServiceBusTest.campos(id, 1));
        var hub = FormatoLogJsonServiceBusTest.registro("br.gov.caixa.simtr.hub");
        hub.setMessage("prova.hub." + id);
        hub.setMarker(FormatoLogJsonServiceBusTest.campos(id, 1));

        try (var captura = new CapturaConsoleRuntime()) {
            var original = captura.console().getFormatter();
            assertFalse(original instanceof FormatoLogJsonServiceBus);
            var esperado = List.of(original.format(novo).stripTrailing(),
                    original.format(hub).stripTrailing());

            Logger.getLogger(categoria).log(novo);
            Logger.getLogger("br.gov.caixa.simtr.hub").log(hub);

            assertEquals(esperado, captura.saida().lines().filter(linha -> linha.contains(id)).toList());
            assertSame(original, captura.console().getFormatter());
        }
    }
}
