package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.CapturaDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CapturarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class CapturaDossieProdutoResourceQuarkusTest {

    private static final String PATH = "/simtr-hub/v1/dossie-produto/{id}/capturar";
    private static final long IDENTIFICADOR = 123L;
    private static final String EVENTO_PREFIXO =
            "simtr-hub.dossie-produto.captura.";
    private static final String SEGREDO_SENTINELA = "TOKEN_EXTERNO_NAO_LOGAR";

    @InjectMock
    CapturarDossieProduto capturarDossieProduto;

    @Test
    void usaPortaEntradaEMapeiaRespostaPublicaExata() {
        when(capturarDossieProduto.executar(any())).thenReturn(
                Uni.createFrom().item(new ResultadoCapturaDossieProduto(IDENTIFICADOR)));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .post(PATH, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("size()", equalTo(1))
                .body("id", equalTo(123));

        var captor = ArgumentCaptor.forClass(IdentificadorDossieProduto.class);
        verify(capturarDossieProduto).executar(captor.capture());
        assertEquals(IDENTIFICADOR, captor.getValue().valor());
    }

    @Test
    void emiteEventosApiAprovadosSemConteudoExternoSensivel() {
        when(capturarDossieProduto.executar(any())).thenReturn(Uni.createFrom().failure(
                new FalhaCapturaDossieProduto(
                        FalhaCapturaDossieProduto.Tipo.NEGOCIO,
                        409,
                        "simtr-dossie-produto",
                        "captura-409",
                        "MTR-CAPTURA-409",
                        List.of(SEGREDO_SENTINELA),
                        SEGREDO_SENTINELA,
                        SEGREDO_SENTINELA,
                        new IllegalStateException(SEGREDO_SENTINELA))));
        var handler = new CapturingHandler();
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);

        try {
            given()
                    .accept(MediaType.APPLICATION_JSON)
                    .when()
                    .post(PATH, IDENTIFICADOR)
                    .then()
                    .statusCode(409)
                    .body("codigo_http", equalTo(409));

            var logs = handler.logs().stream()
                    .filter(log -> log.evento().startsWith(EVENTO_PREFIXO))
                    .toList();
            assertEquals(List.of(
                    EVENTO_PREFIXO + "recebida",
                    EVENTO_PREFIXO + "falhou"),
                    logs.stream().map(LogObservado::evento).toList());
            assertEquals("123", logs.getFirst().mdc().get("dossie_produto_id"));
            assertEquals("MtrBusinessErrorException",
                    logs.getLast().mdc().get("erro_tipo"));
            assertFalse(logs.toString().contains(SEGREDO_SENTINELA));
        } finally {
            rootLogger.removeHandler(handler);
        }
    }

    @Test
    void declaraContratoPublicoESpanServerAprovados() throws NoSuchMethodException {
        var metodo = DossieProdutoResource.class.getMethod(
                "capturarDossieProduto", Long.class);

        assertNotNull(metodo.getAnnotation(POST.class));
        assertEquals("/{id}/capturar", metodo.getAnnotation(Path.class).value());
        assertEquals(MediaType.WILDCARD, metodo.getAnnotation(Consumes.class).value()[0]);
        WithSpan span = metodo.getAnnotation(WithSpan.class);
        assertNotNull(span);
        assertEquals("simtr-hub.api.dossie-produto.capturar", span.value());
        assertEquals(SpanKind.SERVER, span.kind());

        APIResponse[] respostas = metodo.getAnnotationsByType(APIResponse.class);
        assertEquals(List.of("200", "400", "401", "403", "404", "409", "500"),
                List.of(respostas).stream()
                        .map(APIResponse::responseCode)
                        .toList());
        assertEquals(CapturaDossieProdutoResponse.class,
                respostas[0].content()[0].schema().implementation());
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new ArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord instanceof ExtLogRecord extLogRecord
                    && logRecord.getMessage() != null) {
                logs.add(new LogObservado(
                        logRecord.getMessage(), extLogRecord.getMdcCopy()));
            }
        }

        @Override
        public void flush() {
            // No-op: não há buffer no handler de teste.
        }

        @Override
        public void close() {
            logs.clear();
        }

        List<LogObservado> logs() {
            return new ArrayList<>(logs);
        }
    }

    private record LogObservado(String evento, Map<String, String> mdc) {
    }
}
