package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.CapturaDossieProdutoSimuladorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturaDossieProdutoSimuladorMapperTest {

    private static final String FIXTURE =
            "mock/dossieproduto/captura-dossie-produto.md";

    @Test
    void leFixtureRealPorDtoProprioEMapeiaIdentificadorSintetico() {
        var resposta = new MarkdownJsonMockReader(new ObjectMapper())
                .readFirstJsonObject(FIXTURE, CapturaDossieProdutoSimuladorResponse.class);

        var resultado = new CapturaDossieProdutoSimuladorMapper().paraResultado(resposta);

        assertNotNull(resposta);
        assertNotNull(resultado);
        assertEquals(123L, resposta.id());
        assertEquals(123L, resultado.identificadorDossieProduto());
    }

    @Test
    void fixtureDocumentaPostMtrEContemSomenteIdentificadorSintetico() throws IOException {
        var classLoader = Thread.currentThread().getContextClassLoader();
        try (var entrada = classLoader.getResourceAsStream(FIXTURE)) {
            assertNotNull(entrada);
            var markdown = new String(entrada.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");
            var minusculas = markdown.toLowerCase(Locale.ROOT);

            assertTrue(markdown.contains(
                    "POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar"));
            assertTrue(markdown.contains("```json\n{\n  \"id\": 123\n}\n```"));
            assertFalse(minusculas.contains("cpf"));
            assertFalse(minusculas.contains("cnpj"));
            assertFalse(minusculas.contains("token"));
            assertFalse(minusculas.contains("apikey"));
            assertFalse(minusculas.contains("authorization"));
            assertFalse(minusculas.contains("stacktrace"));
        }
    }

    @Test
    void declaraNomeJsonExplicitoNoContratoDoSimulador() throws Exception {
        var jsonProperty = CapturaDossieProdutoSimuladorResponse.class
                .getDeclaredMethod("id")
                .getAnnotation(JsonProperty.class);

        assertNotNull(jsonProperty);
        assertEquals("id", jsonProperty.value());
    }

    @Test
    void mantemIdAusenteOuDivergenteDetectavelPeloAdapter() {
        var mapper = new CapturaDossieProdutoSimuladorMapper();
        var resultadoSemId = mapper.paraResultado(
                new CapturaDossieProdutoSimuladorResponse(null));
        var resultadoDivergente = mapper.paraResultado(
                new CapturaDossieProdutoSimuladorResponse(456L));

        assertNull(mapper.paraResultado(null));
        assertNotNull(resultadoSemId);
        assertNull(resultadoSemId.identificadorDossieProduto());
        assertEquals(456L, resultadoDivergente.identificadorDossieProduto());
        assertNotEquals(123L, resultadoDivergente.identificadorDossieProduto());
    }

    @Test
    void naoAplicaFaultToleranceAoMapperDoSimulador() throws NoSuchMethodException {
        var tipo = CapturaDossieProdutoSimuladorMapper.class;
        var metodo = tipo.getMethod(
                "paraResultado",
                CapturaDossieProdutoSimuladorResponse.class);

        assertFalse(tipo.isAnnotationPresent(Timeout.class));
        assertFalse(tipo.isAnnotationPresent(Retry.class));
        assertFalse(tipo.isAnnotationPresent(CircuitBreaker.class));
        assertFalse(metodo.isAnnotationPresent(Timeout.class));
        assertFalse(metodo.isAnnotationPresent(Retry.class));
        assertFalse(metodo.isAnnotationPresent(CircuitBreaker.class));
    }
}
