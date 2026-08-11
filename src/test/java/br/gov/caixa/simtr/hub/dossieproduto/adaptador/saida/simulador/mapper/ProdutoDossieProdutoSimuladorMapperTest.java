package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ProdutoDossieProdutoSimuladorResponse;
import jakarta.inject.Qualifier;

class ProdutoDossieProdutoSimuladorMapperTest {

    private static final String FIXTURE =
            "mock/dossieproduto/produto-dossie-produto.md";

    @Test
    void leFixtureVaziaPorDtoProprioEMapeiaParaVoid() {
        var reader = new MarkdownJsonMockReader(new ObjectMapper());
        var resposta = reader.readFirstJsonObject(
                FIXTURE,
                ProdutoDossieProdutoSimuladorResponse.class);

        assertNotNull(resposta);
        assertNull(new ProdutoDossieProdutoSimuladorMapper().paraResultado(resposta));
    }

    @Test
    void fixtureDocumentaEndpointMtrDeProduto() throws IOException {
        var classLoader = Thread.currentThread().getContextClassLoader();
        try (var entrada = classLoader.getResourceAsStream(FIXTURE)) {
            assertNotNull(entrada);
            var markdown = new String(entrada.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");

            assertTrue(markdown.contains(
                    "PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/produto"));
            assertTrue(markdown.contains("```json\n{}\n```"));
        }
    }

    @Test
    void declaraQualifiersCdiExclusivosDaCapacidade() {
        assertQualifier(ProdutoMtr.class);
        assertQualifier(ProdutoSimulador.class);
        assertNotEquals(ProdutoMtr.class, ProdutoSimulador.class);
    }

    @Test
    void naoAplicaFaultToleranceAoMapperDoSimulador() throws NoSuchMethodException {
        var tipo = ProdutoDossieProdutoSimuladorMapper.class;
        var metodo = tipo.getMethod(
                "paraResultado",
                ProdutoDossieProdutoSimuladorResponse.class);

        assertFalse(tipo.isAnnotationPresent(Timeout.class));
        assertFalse(tipo.isAnnotationPresent(Retry.class));
        assertFalse(tipo.isAnnotationPresent(CircuitBreaker.class));
        assertFalse(metodo.isAnnotationPresent(Timeout.class));
        assertFalse(metodo.isAnnotationPresent(Retry.class));
        assertFalse(metodo.isAnnotationPresent(CircuitBreaker.class));
    }

    private static void assertQualifier(Class<? extends Annotation> tipo) {
        assertNotNull(tipo.getAnnotation(Qualifier.class));
        assertEquals(RUNTIME, tipo.getAnnotation(Retention.class).value());
        assertEquals(
                Set.of(TYPE, FIELD, PARAMETER, METHOD),
                Set.of(tipo.getAnnotation(Target.class).value()));
    }
}
