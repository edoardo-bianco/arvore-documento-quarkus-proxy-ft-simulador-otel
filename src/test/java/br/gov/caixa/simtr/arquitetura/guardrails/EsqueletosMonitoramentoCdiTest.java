package br.gov.caixa.simtr.arquitetura.guardrails;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class EsqueletosMonitoramentoCdiTest {

    static Stream<String> esqueletos() {
        return EstruturaPlanejada.ESQUELETOS.stream();
    }

    @ParameterizedTest
    @MethodSource("esqueletos")
    void estruturaPendenteNaoDeveSerAtivada(String nomeClasse) throws ClassNotFoundException {
        Class<?> tipo = Class.forName(nomeClasse);
        assertTrue(tipo.isAnnotationPresent(Vetoed.class), nomeClasse);
        assertFalse(tipo.isAnnotationPresent(Path.class), nomeClasse);
        assertTrue(Arc.container().beanManager().getBeans(tipo).isEmpty(), nomeClasse);
    }
}
