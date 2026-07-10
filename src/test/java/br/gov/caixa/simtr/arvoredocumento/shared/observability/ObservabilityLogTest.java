package br.gov.caixa.simtr.arvoredocumento.shared.observability;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class ObservabilityLogTest {

    private static final Logger LOG = Logger.getLogger(ObservabilityLogTest.class);

    @Test
    void fieldsIgnoraChavesOuValoresNulos() {
        Map<String, Object> campos = ObservabilityLog.fields(
                "campo", "valor",
                null, "ignorado",
                "nulo", null
        );

        assertEquals(1, campos.size());
        assertEquals("valor", campos.get("campo"));
    }

    @Test
    void fieldsRejeitaQuantidadeImparDeArgumentos() {
        assertThrows(IllegalArgumentException.class, () -> ObservabilityLog.fields("campo"));
    }

    @Test
    void infoEErrorLimpamMdcAposLog() {
        ObservabilityLog.info(LOG, "evento.info", ObservabilityLog.fields("campo", "valor"));
        ObservabilityLog.error(LOG, "evento.error", new RuntimeException("falha"),
                ObservabilityLog.fields("campo", "valor"));

        assertFalse(MDC.getMap().containsKey("evento"));
        assertFalse(MDC.getMap().containsKey("campo"));
    }
}
