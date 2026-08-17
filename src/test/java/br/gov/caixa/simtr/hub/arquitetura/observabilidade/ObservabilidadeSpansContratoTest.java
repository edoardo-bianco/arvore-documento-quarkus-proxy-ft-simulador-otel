package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.adapter.ProcessoParametrizadoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.CapturaDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.CriacaoDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.ConsultaDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.DocumentoDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.FormularioDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.ProdutoDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.ValidacaoNegocialDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.WorkflowDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.adapter.ChecklistMtrAdapter;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.adapter.GestaoDocumentoMtrAdapter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ObservabilidadeSpansContratoTest {

    private static final String API_DOSSIE_PRODUTO_V1 = "dossie-produto-v1";
    private static final String API_DOSSIE_PRODUTO_V2 = "dossie-produto-v2";
    private static final long IDENTIFICADOR_CONSULTA_DOSSIE_PRODUTO = 4_324_680L;
    private static final String ROTA_CONSULTA_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}";
    private static final String ATRIBUTO_DOSSIE_PRODUTO_ID = "dossie_produto.id";
    private static final String ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO =
            "simtr_hub.simulador_dossie_produto_habilitado";

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void limparSpans() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush().join(10, TimeUnit.SECONDS);
        exporter.reset();
    }

    @Test
    void preservaSpansDasOnzeCapacidadesNoCaminhoSimulador() {
        given()
                .get("/simtr-hub/v1/processo/identificador-negocial/{identificador}", 1000016487L)
                .then()
                .statusCode(200);
        given()
                .get("/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}",
                        1000012583L, 1)
                .then()
                .statusCode(200);
        given()
                .get(ROTA_CONSULTA_DOSSIE_PRODUTO, IDENTIFICADOR_CONSULTA_DOSSIE_PRODUTO)
                .then()
                .statusCode(200);
        given()
                .contentType(ContentType.JSON)
                .body("{\"processo\":100,\"chave_correlacao_canal\":200}")
                .post("/simtr-hub/v1/dossie-produto")
                .then()
                .statusCode(201);
        given()
                .contentType(ContentType.JSON)
                .body("[]")
                .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 123L)
                .then()
                .statusCode(201);
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/simtr-hub/v1/dossie-produto/{id}/documento", 123L)
                .then()
                .statusCode(201);
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 123L)
                .then()
                .statusCode(200);
        given()
                .contentType(ContentType.JSON)
                .body("[{\"codigo_operacao\":100,\"codigo_modalidade\":200}]")
                .patch("/simtr-hub/v1/dossie-produto/{id}/produto", 123L)
                .then()
                .statusCode(204);
        given()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L)
                .then()
                .statusCode(200);
        given()
                .post("/simtr-hub/v1/dossie-produto/{id}/capturar", 123L)
                .then()
                .statusCode(200);
        given()
                .post("/simtr-hub/v1/storage/container/credencial")
                .then()
                .statusCode(200);

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush().join(10, TimeUnit.SECONDS);
        List<SpanData> finalizados = exporter.getFinishedSpanItems();
        Map<String, SpanData> spans = finalizados.stream()
                .filter(span -> span.getName().startsWith("simtr-hub."))
                .collect(Collectors.toMap(SpanData::getName, span -> span));
        Map<String, SpanEsperado> esperados = spansEsperados();

        assertEquals(esperados.keySet(), spans.keySet());
        esperados.forEach((nome, esperado) -> {
            SpanData span = spans.get(nome);
            assertEquals(esperado.kind(), span.getKind(), nome);
            Map<String, Object> atributos = span.getAttributes().asMap().entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
            esperado.atributos().forEach((chave, valor) -> assertEquals(valor, atributos.get(chave),
                    nome + " atributo " + chave));
        });

        SpanData apiProduto = spans.get("simtr-hub.api.dossie-produto.produto.alterar");
        SpanData serviceProduto = spans.get("simtr-hub.service.dossie-produto.produto.alterar");
        assertEquals(apiProduto.getTraceId(), serviceProduto.getTraceId());
        assertEquals(apiProduto.getSpanId(), serviceProduto.getParentSpanId());
        assertEquals(123L, atributo(apiProduto, ATRIBUTO_DOSSIE_PRODUTO_ID));
        assertEquals(1L, atributo(apiProduto, "dossie_produto.produtos.quantidade"));
        assertEquals(123L, atributo(serviceProduto, ATRIBUTO_DOSSIE_PRODUTO_ID));
        assertEquals(1L, atributo(serviceProduto, "dossie_produto.produtos.quantidade"));

        assertArvoreConsultaSimulador(spans);
        assertArvoreCapturaSimulador(spans, finalizados);
    }

    @Test
    void preservaDeclaracoesDosSpansDeIntegracaoMtr() {
        Map<String, String> esperado = Map.ofEntries(
                Map.entry("FormularioDossieProdutoMtrAdapter#atualizar",
                        "mtr.dossie-produto.formulario.atualizar|CLIENT|"),
                Map.entry("CriacaoDossieProdutoMtrAdapter#criar",
                        "mtr.dossie-produto.criar|CLIENT|"),
                Map.entry("ConsultaDossieProdutoMtrAdapter#obter",
                        "mtr.dossie-produto.consultar|CLIENT|"),
                Map.entry("DocumentoDossieProdutoMtrAdapter#incluir",
                        "mtr.dossie-produto.documento.incluir|CLIENT|"),
                Map.entry("ProdutoDossieProdutoMtrAdapter#alterar",
                        "mtr.dossie-produto.produto.alterar|CLIENT|"),
                Map.entry("WorkflowDossieProdutoMtrAdapter#avancar",
                        "mtr.dossie-produto.workflow.avancar|CLIENT|"),
                Map.entry("ValidacaoNegocialDossieProdutoMtrAdapter#registrar",
                        "mtr.dossie-produto.validacao-negocial.registrar|CLIENT|"),
                Map.entry("GestaoDocumentoMtrAdapter#obter",
                        "mtr.gestao-documento.credencial-container.gerar|CLIENT|"),
                Map.entry("ChecklistMtrAdapter#obter",
                        "mtr.parametrizacao.checklist.consultar|CLIENT|"),
                Map.entry("ProcessoParametrizadoMtrAdapter#obter",
                        "mtr.parametrizacao.processo.consultar|CLIENT|"),
                Map.entry("CapturaDossieProdutoMtrAdapter#capturar",
                        "mtr.dossie-produto.capturar|CLIENT|")
        );

        assertEquals(esperado, extrairSpansDeclarados(
                CriacaoDossieProdutoMtrAdapter.class,
                ConsultaDossieProdutoMtrAdapter.class,
                DocumentoDossieProdutoMtrAdapter.class,
                FormularioDossieProdutoMtrAdapter.class,
                ProdutoDossieProdutoMtrAdapter.class,
                CapturaDossieProdutoMtrAdapter.class,
                ValidacaoNegocialDossieProdutoMtrAdapter.class,
                WorkflowDossieProdutoMtrAdapter.class,
                GestaoDocumentoMtrAdapter.class,
                ChecklistMtrAdapter.class,
                ProcessoParametrizadoMtrAdapter.class
        ));
    }

    private static Map<String, SpanEsperado> spansEsperados() {
        return Map.ofEntries(
                api("simtr-hub.api.processo.consultar",
                        "/simtr-hub/v1/processo/identificador-negocial/{identificador}",
                        "parametrizacao-processo-v1"),
                service("simtr-hub.service.processo.consultar",
                        "simtr_hub.simulador_parametrizacao_processo_habilitado"),
                api("simtr-hub.api.checklist.consultar",
                        "/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}",
                        "parametrizacao-checklist-v1"),
                service("simtr-hub.service.checklist.consultar",
                        "simtr_hub.simulador_parametrizacao_checklist_habilitado"),
                api("simtr-hub.api.dossie-produto.consultar",
                        ROTA_CONSULTA_DOSSIE_PRODUTO, API_DOSSIE_PRODUTO_V2),
                service("simtr-hub.service.dossie-produto.consultar",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.dossie-produto.criar",
                        "/simtr-hub/v1/dossie-produto", API_DOSSIE_PRODUTO_V1),
                service("simtr-hub.service.dossie-produto.criar",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.dossie-produto.formulario.atualizar",
                        "/simtr-hub/v1/dossie-produto/{id}/formulario", API_DOSSIE_PRODUTO_V1),
                service("simtr-hub.service.dossie-produto.formulario.atualizar",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.dossie-produto.documento.incluir",
                        "/simtr-hub/v1/dossie-produto/{id}/documento", API_DOSSIE_PRODUTO_V2),
                service("simtr-hub.service.dossie-produto.documento.incluir",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.dossie-produto.validacao-negocial.registrar",
                        "/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", API_DOSSIE_PRODUTO_V1),
                service("simtr-hub.service.dossie-produto.validacao-negocial.registrar",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.dossie-produto.produto.alterar",
                        "/simtr-hub/v1/dossie-produto/{id}/produto", API_DOSSIE_PRODUTO_V1),
                service("simtr-hub.service.dossie-produto.produto.alterar",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.dossie-produto.workflow.avancar",
                        "/simtr-hub/v1/dossie-produto/{id}/workflow", API_DOSSIE_PRODUTO_V1),
                service("simtr-hub.service.dossie-produto.workflow.avancar",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.dossie-produto.capturar",
                        "/simtr-hub/v1/dossie-produto/{id}/capturar", API_DOSSIE_PRODUTO_V1),
                service("simtr-hub.service.dossie-produto.capturar",
                        ATRIBUTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO),
                api("simtr-hub.api.gestao-documento.credencial-container.gerar",
                        "/simtr-hub/v1/storage/container/credencial", "gestao-documento-v1"),
                service("simtr-hub.service.gestao-documento.credencial-container.gerar",
                        "simtr_hub.simulador_gestao_documento_habilitado")
        );
    }

    private static Map.Entry<String, SpanEsperado> api(String nome, String rota, String api) {
        return Map.entry(nome, new SpanEsperado(SpanKind.SERVER, Map.of(
                "http.route", rota,
                "simtr_hub.api", api
        )));
    }

    private static Map.Entry<String, SpanEsperado> service(String nome, String atributoSimulador) {
        return Map.entry(nome, new SpanEsperado(SpanKind.INTERNAL, Map.of(
                atributoSimulador, true,
                "simtr_hub.origem_dados", "mock"
        )));
    }

    private static Map<String, String> extrairSpansDeclarados(Class<?>... tipos) {
        Map<String, String> spans = new TreeMap<>();
        for (Class<?> tipo : tipos) {
            for (Method metodo : tipo.getDeclaredMethods()) {
                WithSpan withSpan = metodo.getAnnotation(WithSpan.class);
                if (withSpan == null) {
                    continue;
                }
                List<String> atributos = Arrays.stream(metodo.getParameterAnnotations())
                        .flatMap(Arrays::stream)
                        .filter(SpanAttribute.class::isInstance)
                        .map(SpanAttribute.class::cast)
                        .map(SpanAttribute::value)
                        .sorted()
                        .toList();
                spans.put(tipo.getSimpleName() + "#" + metodo.getName(),
                        withSpan.value() + "|" + withSpan.kind() + "|" + String.join(",", atributos));
            }
        }
        return spans;
    }

    private static Object atributo(SpanData span, String nome) {
        return span.getAttributes().asMap().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(nome))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static void assertArvoreConsultaSimulador(Map<String, SpanData> spans) {
        SpanData api = spans.get("simtr-hub.api.dossie-produto.consultar");
        SpanData aplicacao = spans.get("simtr-hub.service.dossie-produto.consultar");

        assertEquals(api.getTraceId(), aplicacao.getTraceId());
        assertEquals(api.getSpanId(), aplicacao.getParentSpanId());
        assertEquals(IDENTIFICADOR_CONSULTA_DOSSIE_PRODUTO,
                atributo(api, ATRIBUTO_DOSSIE_PRODUTO_ID));
        assertEquals(IDENTIFICADOR_CONSULTA_DOSSIE_PRODUTO,
                atributo(aplicacao, ATRIBUTO_DOSSIE_PRODUTO_ID));
        assertEquals("mock", atributo(aplicacao, "simtr_hub.origem_dados"));
        assertEquals(1L, atributo(aplicacao, "dossie_produto.clientes.quantidade"));
        assertEquals(0L,
                atributo(aplicacao, "dossie_produto.unidades_tratamento.quantidade"));
        assertEquals(1L,
                atributo(aplicacao, "dossie_produto.produtos_contratados.quantidade"));
    }

    private static void assertArvoreCapturaSimulador(
            Map<String, SpanData> spans,
            List<SpanData> finalizados
    ) {
        SpanData api = spans.get("simtr-hub.api.dossie-produto.capturar");
        SpanData aplicacao = spans.get("simtr-hub.service.dossie-produto.capturar");

        assertEquals(api.getTraceId(), aplicacao.getTraceId());
        assertEquals(api.getSpanId(), aplicacao.getParentSpanId());
        assertEquals(123L, atributo(api, ATRIBUTO_DOSSIE_PRODUTO_ID));
        assertEquals(123L, atributo(aplicacao, ATRIBUTO_DOSSIE_PRODUTO_ID));
        assertEquals("mock", atributo(aplicacao, "simtr_hub.origem_dados"));
        assertEquals(0L, finalizados.stream()
                .filter(span -> "mtr.dossie-produto.capturar".equals(span.getName()))
                .count());
    }

    private record SpanEsperado(SpanKind kind, Map<String, Object> atributos) {
    }
}
