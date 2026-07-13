package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

import br.gov.caixa.simtr.hub.dossieproduto.integracao.DossieProdutoGateway;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.CriacaoDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter.WorkflowDossieProdutoMtrAdapter;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoChecklistGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoProcessoGateway;
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
    void preservaSpansDasOitoCapacidadesNoCaminhoSimulador() {
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
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L)
                .then()
                .statusCode(200);
        given()
                .post("/simtr-hub/v1/storage/container/credencial")
                .then()
                .statusCode(200);

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush().join(10, TimeUnit.SECONDS);
        Map<String, SpanData> spans = exporter.getFinishedSpanItems().stream()
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
    }

    @Test
    void preservaDeclaracoesDosSpansDeIntegracaoMtr() {
        Map<String, String> esperado = Map.ofEntries(
                Map.entry("DossieProdutoGateway#atualizarFormularioDossieProduto",
                        "mtr.dossie-produto.formulario.atualizar|CLIENT|"),
                Map.entry("CriacaoDossieProdutoMtrAdapter#criar",
                        "mtr.dossie-produto.criar|CLIENT|"),
                Map.entry("DossieProdutoGateway#incluirDocumentoDossieProduto",
                        "mtr.dossie-produto.documento.incluir|CLIENT|"),
                Map.entry("WorkflowDossieProdutoMtrAdapter#avancar",
                        "mtr.dossie-produto.workflow.avancar|CLIENT|"),
                Map.entry("DossieProdutoGateway#registrarValidacaoNegocialDossieProduto",
                        "mtr.dossie-produto.validacao-negocial.registrar|CLIENT|"),
                Map.entry("GestaoDocumentoGateway#gerarCredencialContainer",
                        "mtr.gestao-documento.credencial-container.gerar|CLIENT|"),
                Map.entry("ParametrizacaoChecklistGateway#consultarPorIdentificadorNegocialEVersao",
                        "mtr.parametrizacao.checklist.consultar|CLIENT|"
                                + "mtr.parametrizacao.checklist.identificador_negocial,"
                                + "mtr.parametrizacao.checklist.versao"),
                Map.entry("ParametrizacaoProcessoGateway#consultarPorIdentificadorNegocial",
                        "mtr.parametrizacao.processo.consultar|CLIENT|"
                                + "mtr.parametrizacao.processo.identificador_negocial")
        );

        assertEquals(esperado, extrairSpansDeclarados(
                DossieProdutoGateway.class,
                CriacaoDossieProdutoMtrAdapter.class,
                WorkflowDossieProdutoMtrAdapter.class,
                GestaoDocumentoGateway.class,
                ParametrizacaoChecklistGateway.class,
                ParametrizacaoProcessoGateway.class
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
                api("simtr-hub.api.dossie-produto.criar",
                        "/simtr-hub/v1/dossie-produto", "dossie-produto-v1"),
                service("simtr-hub.service.dossie-produto.criar",
                        "simtr_hub.simulador_dossie_produto_habilitado"),
                api("simtr-hub.api.dossie-produto.formulario.atualizar",
                        "/simtr-hub/v1/dossie-produto/{id}/formulario", "dossie-produto-v1"),
                service("simtr-hub.service.dossie-produto.formulario.atualizar",
                        "simtr_hub.simulador_dossie_produto_habilitado"),
                api("simtr-hub.api.dossie-produto.documento.incluir",
                        "/simtr-hub/v1/dossie-produto/{id}/documento", "dossie-produto-v2"),
                service("simtr-hub.service.dossie-produto.documento.incluir",
                        "simtr_hub.simulador_dossie_produto_habilitado"),
                api("simtr-hub.api.dossie-produto.validacao-negocial.registrar",
                        "/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", "dossie-produto-v1"),
                service("simtr-hub.service.dossie-produto.validacao-negocial.registrar",
                        "simtr_hub.simulador_dossie_produto_habilitado"),
                api("simtr-hub.api.dossie-produto.workflow.avancar",
                        "/simtr-hub/v1/dossie-produto/{id}/workflow", "dossie-produto-v1"),
                service("simtr-hub.service.dossie-produto.workflow.avancar",
                        "simtr_hub.simulador_dossie_produto_habilitado"),
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

    private record SpanEsperado(SpanKind kind, Map<String, Object> atributos) {
    }
}
