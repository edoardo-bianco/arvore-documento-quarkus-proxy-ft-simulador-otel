package br.gov.caixa.simtr.arquitetura.guardrails;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class ResultadoMonitoramentoCdiTest {
    static Stream<String> componentes() {
        return ComponentesResultadoMonitoramento.CLASSES.stream();
    }

    @ParameterizedTest
    @MethodSource("componentes")
    void componenteDeResultadoDeveResolverComoBeanUnicoSemEndpoint(String nomeClasse) throws ClassNotFoundException {
        Class<?> tipo = Class.forName(nomeClasse);
        assertFalse(tipo.isAnnotationPresent(Vetoed.class), nomeClasse);
        assertFalse(tipo.isAnnotationPresent(Path.class), nomeClasse);
        assertEquals(1, Arc.container().beanManager().getBeans(tipo).size(), nomeClasse);
    }
}
