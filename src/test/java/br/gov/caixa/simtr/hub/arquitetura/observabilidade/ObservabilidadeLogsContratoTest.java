package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ObservabilidadeLogsContratoTest {

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;

    @BeforeEach
    void capturarLogs() {
        rootLogger = Logger.getLogger("");
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
    }

    @AfterEach
    void removerCaptura() {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    void preservaEventosEstruturadosDasOitoCapacidadesNoCaminhoSimulador() {
        chamarOitoEndpoints();

        Map<String, LogObservado> observados = handler.logs().stream()
                .filter(log -> eventosEsperados().contains(log.evento()))
                .collect(Collectors.toMap(LogObservado::evento, log -> log));

        assertEquals(eventosEsperados(), observados.keySet());
        observados.forEach((evento, log) -> {
            String traceId = log.mdc().get("traceId");
            String spanId = log.mdc().get("spanId");
            assertEquals(evento, log.mdc().get("evento"), evento);
            assertNotNull(log.mdc().get("camada"), evento + " camada");
            assertNotNull(log.mdc().get("componente"), evento + " componente");
            assertNotNull(log.mdc().get("operacao"), evento + " operacao");
            assertNotNull(traceId, evento + " traceId");
            assertNotNull(spanId, evento + " spanId");
            assertEquals(32, traceId.length(), evento + " traceId");
            assertEquals(16, spanId.length(), evento + " spanId");
            assertEquals("true", log.mdc().get("traceSampled"), evento + " traceSampled");
        });
    }

    private static void chamarOitoEndpoints() {
        given()
                .get("/simtr-hub/v1/processo/identificador-negocial/{identificador}", 1000016487L)
                .then().statusCode(200);
        given()
                .get("/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}",
                        1000012583L, 1)
                .then().statusCode(200);
        given()
                .contentType(ContentType.JSON)
                .body("{\"processo\":100,\"chave_correlacao_canal\":200}")
                .post("/simtr-hub/v1/dossie-produto")
                .then().statusCode(201);
        given()
                .contentType(ContentType.JSON)
                .body("[]")
                .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 123L)
                .then().statusCode(201);
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/simtr-hub/v1/dossie-produto/{id}/documento", 123L)
                .then().statusCode(201);
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 123L)
                .then().statusCode(200);
        given()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L)
                .then().statusCode(200);
        given()
                .post("/simtr-hub/v1/storage/container/credencial")
                .then().statusCode(200);
    }

    private static Set<String> eventosEsperados() {
        return Set.of(
                "simtr-hub.processo.requisicao.recebida",
                "simtr-hub.processo.service.iniciado",
                "simtr-hub.processo.simulador.usado",
                "simtr-hub.processo.service.concluido",
                "simtr-hub.processo.resposta.enviada",
                "simtr-hub.checklist.requisicao.recebida",
                "simtr-hub.checklist.service.iniciado",
                "simtr-hub.checklist.simulador.usado",
                "simtr-hub.checklist.service.concluido",
                "simtr-hub.checklist.resposta.enviada",
                "simtr-hub.dossie-produto.requisicao.recebida",
                "simtr-hub.dossie-produto.service.iniciado",
                "simtr-hub.dossie-produto.simulador.usado",
                "simtr-hub.dossie-produto.service.concluido",
                "simtr-hub.dossie-produto.resposta.enviada",
                "simtr-hub.dossie-produto.formulario.requisicao.recebida",
                "simtr-hub.dossie-produto.formulario.service.iniciado",
                "simtr-hub.dossie-produto.formulario.simulador.usado",
                "simtr-hub.dossie-produto.formulario.service.concluido",
                "simtr-hub.dossie-produto.formulario.resposta.enviada",
                "simtr-hub.dossie-produto.documento.requisicao.recebida",
                "simtr-hub.dossie-produto.documento.service.iniciado",
                "simtr-hub.dossie-produto.documento.simulador.usado",
                "simtr-hub.dossie-produto.documento.service.concluido",
                "simtr-hub.dossie-produto.documento.resposta.enviada",
                "simtr-hub.dossie-produto.validacao-negocial.requisicao.recebida",
                "simtr-hub.dossie-produto.validacao-negocial.service.iniciado",
                "simtr-hub.dossie-produto.validacao-negocial.simulador.usado",
                "simtr-hub.dossie-produto.validacao-negocial.service.concluido",
                "simtr-hub.dossie-produto.validacao-negocial.resposta.enviada",
                "simtr-hub.dossie-produto.workflow.requisicao.recebida",
                "simtr-hub.dossie-produto.workflow.service.iniciado",
                "simtr-hub.dossie-produto.workflow.simulador.usado",
                "simtr-hub.dossie-produto.workflow.service.concluido",
                "simtr-hub.dossie-produto.workflow.resposta.enviada",
                "simtr-hub.gestao-documento.credencial-container.requisicao.recebida",
                "simtr-hub.gestao-documento.credencial-container.service.iniciado",
                "simtr-hub.gestao-documento.credencial-container.simulador.usado",
                "simtr-hub.gestao-documento.credencial-container.service.concluido",
                "simtr-hub.gestao-documento.credencial-container.resposta.enviada"
        );
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record instanceof ExtLogRecord extLogRecord && record.getMessage() != null) {
                logs.add(new LogObservado(record.getMessage(), extLogRecord.getMdcCopy()));
            }
        }

        @Override
        public void flush() {
            // No-op deliberado: o appender de teste não mantém estado pendente.
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
