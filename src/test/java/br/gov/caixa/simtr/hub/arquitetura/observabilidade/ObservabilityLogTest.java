package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

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
    private static final String CHAVE_CAMPO = "campo";
    private static final String VALOR_CAMPO = "valor";

    @Test
    void fieldsIgnoraChavesOuValoresNulos() {
        Map<String, Object> campos = ObservabilityLog.fields(
                CHAVE_CAMPO, VALOR_CAMPO,
                null, "ignorado",
                "nulo", null
        );

        assertEquals(1, campos.size());
        assertEquals(VALOR_CAMPO, campos.get(CHAVE_CAMPO));
    }

    @Test
    void fieldsRejeitaQuantidadeImparDeArgumentos() {
        assertThrows(IllegalArgumentException.class, () -> ObservabilityLog.fields(CHAVE_CAMPO));
    }

    @Test
    void infoEErrorLimpamMdcAposLog() {
        ObservabilityLog.info(LOG, "evento.info", ObservabilityLog.fields(CHAVE_CAMPO, VALOR_CAMPO));
        ObservabilityLog.error(LOG, "evento.error", new RuntimeException("falha"),
                ObservabilityLog.fields(CHAVE_CAMPO, VALOR_CAMPO));

        assertFalse(MDC.getMap().containsKey("evento"));
        assertFalse(MDC.getMap().containsKey(CHAVE_CAMPO));
    }
}
