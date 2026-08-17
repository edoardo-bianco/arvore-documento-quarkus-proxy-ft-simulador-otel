package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.ProdutoDossieProdutoObservabilidade;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;
import io.smallrye.mutiny.Uni;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class ObservabilidadeLogsContratoTest {

    private static final long IDENTIFICADOR_CONSULTA_DOSSIE = 4_324_680L;
    private static final String IDENTIFICADOR_CONSULTA_DOSSIE_TEXTO = "4324680";
    private static final String OPERACAO_CONSULTA_DOSSIE = "consultar-dossie-produto";
    private static final String PREFIXO_EVENTO_CONSULTA =
            "simtr-hub.dossie-produto.consulta.";
    private static final String EVENTO_CONSULTA_RECEBIDA =
            PREFIXO_EVENTO_CONSULTA + "requisicao.recebida";
    private static final String EVENTO_CONSULTA_SERVICE_INICIADA =
            PREFIXO_EVENTO_CONSULTA + "service.iniciada";
    private static final String EVENTO_CONSULTA_SIMULADOR =
            PREFIXO_EVENTO_CONSULTA + "simulador.usado";
    private static final String EVENTO_CONSULTA_SERVICE_CONCLUIDA =
            PREFIXO_EVENTO_CONSULTA + "service.concluida";
    private static final String EVENTO_CONSULTA_RESPOSTA =
            PREFIXO_EVENTO_CONSULTA + "resposta.enviada";
    private static final String EVENTO_CONSULTA_SERVICE_FALHOU =
            PREFIXO_EVENTO_CONSULTA + "service.falhou";
    private static final String EVENTO_CONSULTA_REQUISICAO_FALHOU =
            PREFIXO_EVENTO_CONSULTA + "requisicao.falhou";
    private static final String PREFIXO_EVENTO_CAPTURA =
            "simtr-hub.dossie-produto.captura.";
    private static final String ORIGEM_MOCK = "mock";
    private static final String RESULTADO_SUCESSO = "sucesso";
    private static final String RESULTADO_ERRO = "erro";
    private static final String CAMPO_OPERACAO = "operacao";
    private static final String CAMPO_DOSSIE_ID = "dossie_produto_id";
    private static final String CAMPO_RESULTADO = "resultado";
    private static final String CAMPO_ERRO_TIPO = "erro_tipo";
    private static final String CAMPO_ORIGEM = "origem";
    private static final String CAMPO_CLIENTES_QUANTIDADE = "clientes_quantidade";
    private static final String CAMPO_UNIDADES_QUANTIDADE =
            "unidades_tratamento_quantidade";
    private static final String CAMPO_PRODUTOS_QUANTIDADE =
            "produtos_contratados_quantidade";

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
    void preservaEventosEstruturadosDasOnzeCapacidadesNoCaminhoSimulador() {
        chamarOnzeEndpoints();

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

        Map<String, LogObservado> produto = observados.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("simtr-hub.dossie-produto.produto."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(eventosProdutoEsperados(), produto.keySet());
        produto.forEach((evento, log) -> assertCamposProduto(log, evento, "123", "2"));
        assertEquals("sucesso", produto.get(
                "simtr-hub.dossie-produto.produto.service.concluido").mdc().get(CAMPO_RESULTADO));
        assertEquals("sucesso", produto.get(
                "simtr-hub.dossie-produto.produto.resposta.enviada").mdc().get(CAMPO_RESULTADO));
        assertSemDadosSensiveis(produto.values());

        Map<String, LogObservado> consulta = observados.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(PREFIXO_EVENTO_CONSULTA))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertConsultaSimulador(consulta);
        assertSemDadosSensiveis(consulta.values());

        Map<String, LogObservado> captura = observados.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(PREFIXO_EVENTO_CAPTURA))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertCapturaSimulador(captura);
        assertSemDadosSensiveis(captura.values());
    }

    @Test
    void registraFalhaDeProdutoComCamposEstaveisSemDadosSensiveis() {
        var falhaEsperada = new IllegalStateException("falha observavel controlada");
        SolicitarAlteracaoProdutosContratadosDossieProduto porta =
                comando -> Uni.createFrom().failure(falhaEsperada);
        var observabilidade = new ProdutoDossieProdutoObservabilidade(porta, false);
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(
                456L,
                List.of(
                        new ProdutoContratadoDossieProduto(987654321, 876543210, false),
                        new ProdutoContratadoDossieProduto(765432109, 654321098, true)));

        var espera = observabilidade.executar(comando).await();
        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                espera::indefinitely);

        assertSame(falhaEsperada, falha);
        Map<String, LogObservado> produto = handler.logs().stream()
                .filter(log -> log.evento().startsWith(
                        "simtr-hub.dossie-produto.produto.service."))
                .collect(Collectors.toMap(LogObservado::evento, log -> log));
        assertEquals(Set.of(
                "simtr-hub.dossie-produto.produto.service.iniciado",
                "simtr-hub.dossie-produto.produto.service.falhou"), produto.keySet());
        produto.forEach((evento, log) -> assertCamposProduto(log, evento, "456", "2"));
        LogObservado falhou = produto.get(
                "simtr-hub.dossie-produto.produto.service.falhou");
        assertEquals("erro", falhou.mdc().get(CAMPO_RESULTADO));
        assertEquals("IllegalStateException", falhou.mdc().get("erro_tipo"));
        assertSemDadosSensiveis(produto.values());
    }

    @Test
    void registraFalhaDaConsultaComOrigemEClassificacaoSemDadosSensiveis() {
        long identificadorAusente = 9_876_543L;
        String identificadorAusenteTexto = "9876543";

        given()
                .get("/simtr-hub/v1/dossie-produto/{id}", identificadorAusente)
                .then().statusCode(404);

        Map<String, LogObservado> consulta = handler.logs().stream()
                .filter(log -> log.evento().startsWith(PREFIXO_EVENTO_CONSULTA))
                .collect(Collectors.toMap(LogObservado::evento, log -> log));
        assertEquals(Set.of(
                EVENTO_CONSULTA_RECEBIDA,
                EVENTO_CONSULTA_SERVICE_INICIADA,
                EVENTO_CONSULTA_SIMULADOR,
                EVENTO_CONSULTA_SERVICE_FALHOU,
                EVENTO_CONSULTA_REQUISICAO_FALHOU
        ), consulta.keySet());
        consulta.forEach((evento, log) -> {
            assertEquals(OPERACAO_CONSULTA_DOSSIE, log.mdc().get(CAMPO_OPERACAO), evento);
            assertEquals(identificadorAusenteTexto,
                    log.mdc().get(CAMPO_DOSSIE_ID), evento);
        });
        assertOrigemMock(consulta.get(EVENTO_CONSULTA_SERVICE_INICIADA));
        assertOrigemMock(consulta.get(EVENTO_CONSULTA_SIMULADOR));
        LogObservado serviceFalhou = consulta.get(EVENTO_CONSULTA_SERVICE_FALHOU);
        assertOrigemMock(serviceFalhou);
        assertEquals(RESULTADO_ERRO, serviceFalhou.mdc().get(CAMPO_RESULTADO));
        assertEquals("FalhaConsultaDossieProduto", serviceFalhou.mdc().get(CAMPO_ERRO_TIPO));
        LogObservado apiFalhou = consulta.get(EVENTO_CONSULTA_REQUISICAO_FALHOU);
        assertEquals(RESULTADO_ERRO, apiFalhou.mdc().get(CAMPO_RESULTADO));
        assertEquals("MtrBusinessErrorException", apiFalhou.mdc().get(CAMPO_ERRO_TIPO));
        assertSemDadosSensiveis(consulta.values());
    }

    private static void chamarOnzeEndpoints() {
        given()
                .get("/simtr-hub/v1/processo/identificador-negocial/{identificador}", 1000016487L)
                .then().statusCode(200);
        given()
                .get("/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}",
                        1000012583L, 1)
                .then().statusCode(200);
        given()
                .get("/simtr-hub/v1/dossie-produto/{id}", IDENTIFICADOR_CONSULTA_DOSSIE)
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
                .contentType(ContentType.JSON)
                .body("""
                        [
                          {"codigo_operacao":987654321,"codigo_modalidade":876543210},
                          {"codigo_operacao":765432109,"codigo_modalidade":654321098,"excluir":true}
                        ]
                        """)
                .patch("/simtr-hub/v1/dossie-produto/{id}/produto", 123L)
                .then().statusCode(204);
        given()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L)
                .then().statusCode(200);
        given()
                .post("/simtr-hub/v1/dossie-produto/{id}/capturar", 123L)
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
                EVENTO_CONSULTA_RECEBIDA,
                EVENTO_CONSULTA_SERVICE_INICIADA,
                EVENTO_CONSULTA_SIMULADOR,
                EVENTO_CONSULTA_SERVICE_CONCLUIDA,
                EVENTO_CONSULTA_RESPOSTA,
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
                "simtr-hub.dossie-produto.produto.requisicao.recebida",
                "simtr-hub.dossie-produto.produto.service.iniciado",
                "simtr-hub.dossie-produto.produto.simulador.usado",
                "simtr-hub.dossie-produto.produto.service.concluido",
                "simtr-hub.dossie-produto.produto.resposta.enviada",
                "simtr-hub.dossie-produto.workflow.requisicao.recebida",
                "simtr-hub.dossie-produto.workflow.service.iniciado",
                "simtr-hub.dossie-produto.workflow.simulador.usado",
                "simtr-hub.dossie-produto.workflow.service.concluido",
                "simtr-hub.dossie-produto.workflow.resposta.enviada",
                PREFIXO_EVENTO_CAPTURA + "recebida",
                PREFIXO_EVENTO_CAPTURA + "processamento.iniciada",
                PREFIXO_EVENTO_CAPTURA + "simulador.usado",
                PREFIXO_EVENTO_CAPTURA + "processamento.concluida",
                PREFIXO_EVENTO_CAPTURA + "concluida",
                "simtr-hub.gestao-documento.credencial-container.requisicao.recebida",
                "simtr-hub.gestao-documento.credencial-container.service.iniciado",
                "simtr-hub.gestao-documento.credencial-container.simulador.usado",
                "simtr-hub.gestao-documento.credencial-container.service.concluido",
                "simtr-hub.gestao-documento.credencial-container.resposta.enviada"
        );
    }

    private static Set<String> eventosProdutoEsperados() {
        return Set.of(
                "simtr-hub.dossie-produto.produto.requisicao.recebida",
                "simtr-hub.dossie-produto.produto.service.iniciado",
                "simtr-hub.dossie-produto.produto.simulador.usado",
                "simtr-hub.dossie-produto.produto.service.concluido",
                "simtr-hub.dossie-produto.produto.resposta.enviada");
    }

    private static void assertCamposProduto(
            LogObservado log,
            String evento,
            String id,
            String quantidade
    ) {
        assertEquals("alterar-produtos-contratados-dossie-produto",
                log.mdc().get("operacao"), evento + " operacao");
        assertEquals(id, log.mdc().get(CAMPO_DOSSIE_ID), evento + " id");
        assertEquals(quantidade, log.mdc().get("produtos_quantidade"), evento + " quantidade");
    }

    private static void assertConsultaSimulador(Map<String, LogObservado> consulta) {
        assertEquals(eventosConsultaEsperados(), consulta.keySet());
        consulta.forEach((evento, log) -> {
            assertEquals(OPERACAO_CONSULTA_DOSSIE, log.mdc().get(CAMPO_OPERACAO), evento);
            assertEquals(IDENTIFICADOR_CONSULTA_DOSSIE_TEXTO,
                    log.mdc().get(CAMPO_DOSSIE_ID), evento);
        });

        assertOrigemMock(consulta.get(EVENTO_CONSULTA_SERVICE_INICIADA));
        assertOrigemMock(consulta.get(EVENTO_CONSULTA_SIMULADOR));
        assertOrigemMock(consulta.get(EVENTO_CONSULTA_SERVICE_CONCLUIDA));
        assertEquals(RESULTADO_SUCESSO,
                consulta.get(EVENTO_CONSULTA_SERVICE_CONCLUIDA).mdc().get(CAMPO_RESULTADO));
        assertEquals(RESULTADO_SUCESSO,
                consulta.get(EVENTO_CONSULTA_RESPOSTA).mdc().get(CAMPO_RESULTADO));
        assertContagensConsulta(consulta.get(EVENTO_CONSULTA_SERVICE_CONCLUIDA));
        assertContagensConsulta(consulta.get(EVENTO_CONSULTA_RESPOSTA));
    }

    private static Set<String> eventosConsultaEsperados() {
        return Set.of(
                EVENTO_CONSULTA_RECEBIDA,
                EVENTO_CONSULTA_SERVICE_INICIADA,
                EVENTO_CONSULTA_SIMULADOR,
                EVENTO_CONSULTA_SERVICE_CONCLUIDA,
                EVENTO_CONSULTA_RESPOSTA
        );
    }

    private static void assertCapturaSimulador(Map<String, LogObservado> captura) {
        assertEquals(Set.of(
                PREFIXO_EVENTO_CAPTURA + "recebida",
                PREFIXO_EVENTO_CAPTURA + "processamento.iniciada",
                PREFIXO_EVENTO_CAPTURA + "simulador.usado",
                PREFIXO_EVENTO_CAPTURA + "processamento.concluida",
                PREFIXO_EVENTO_CAPTURA + "concluida"), captura.keySet());
        captura.forEach((evento, log) -> {
            assertEquals("capturar-dossie-produto-v1",
                    log.mdc().get(CAMPO_OPERACAO), evento);
            assertEquals("123", log.mdc().get(CAMPO_DOSSIE_ID), evento);
        });
        assertOrigemMock(captura.get(PREFIXO_EVENTO_CAPTURA + "processamento.iniciada"));
        assertOrigemMock(captura.get(PREFIXO_EVENTO_CAPTURA + "simulador.usado"));
        assertOrigemMock(captura.get(PREFIXO_EVENTO_CAPTURA + "processamento.concluida"));
        assertEquals(RESULTADO_SUCESSO, captura.get(
                PREFIXO_EVENTO_CAPTURA + "processamento.concluida").mdc().get(CAMPO_RESULTADO));
        assertEquals(RESULTADO_SUCESSO, captura.get(
                PREFIXO_EVENTO_CAPTURA + "concluida").mdc().get(CAMPO_RESULTADO));
    }

    private static void assertOrigemMock(LogObservado log) {
        assertEquals(ORIGEM_MOCK, log.mdc().get(CAMPO_ORIGEM));
    }

    private static void assertContagensConsulta(LogObservado log) {
        assertEquals("1", log.mdc().get(CAMPO_CLIENTES_QUANTIDADE));
        assertEquals("0", log.mdc().get(CAMPO_UNIDADES_QUANTIDADE));
        assertEquals("1", log.mdc().get(CAMPO_PRODUTOS_QUANTIDADE));
    }

    private static void assertSemDadosSensiveis(Iterable<LogObservado> logs) {
        StringBuilder sinais = new StringBuilder();
        logs.forEach(log -> sinais.append(log.evento()).append(log.mdc()));
        String observado = sinais.toString();
        assertFalse(observado.contains("codigo_operacao"));
        assertFalse(observado.contains("987654321"));
        assertFalse(observado.contains("test-apikey"));
        assertFalse(observado.contains("stub-access-token"));
        assertFalse(observado.contains("00000000000"));
        assertFalse(observado.contains("00000000000000"));
        assertFalse(observado.contains("CLIENTE SIMULADO"));
        assertFalse(observado.contains("SIMTRAPI"));
        assertFalse(observado.contains("chave_correlacao_canal"));
        assertFalse(observado.contains("localhost"));
        assertFalse(observado.contains("127.0.0.1"));
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
