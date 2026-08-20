package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoSimuladorMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ConsultaDocumentosDossieProdutoSelecaoSimuladorQuarkusTest {

    private static final long IDENTIFICADOR = 4_081_899L;
    private static final String CAMINHO_PUBLICO =
            "/simtr-hub/v1/dossie-produto/{id}/documentos";
    private static final String SPAN_CLIENT =
            "mtr.dossie-produto.documentos.consultar";

    @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
    boolean simuladorHabilitado;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void limparEvidencias() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void selecionaFixtureLocalSemSpanClientNemChamadaAoStubMtr() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .queryParam("cnpj", "CNPJ_SENTINELA")
                .queryParam("cpf", "CPF_SENTINELA")
                .queryParam("fase", 7)
                .queryParam("inclui-armazenamento", true)
                .queryParam("inclui-assinaturas", false)
                .queryParam("inclui-atributos", true)
                .queryParam("inclui-conformidade", false)
                .queryParam("inclui-outsourcing", true)
                .queryParam("inclui-propriedades", false)
                .queryParam("inclui-url", true)
                .queryParam("ip-usuario", "IP_SENTINELA")
                .queryParam("tipologia", "TIPOLOGIA_SENTINELA")
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);

        assertTrue(simuladorHabilitado);
        assertEquals(14, resposta.size());
        assertEquals(1_132_220L,
                resposta.get(0).path("id_instancia_documento").longValue());
        assertEquals("GED-SIMULADO-0001", resposta.get(0).path("codigo_ged").textValue());
        assertTrue(DossieProdutoMtrStubTestResource.requisicoes().isEmpty());
        assertEquals(0L, spanExporter.getFinishedSpanItems().stream()
                .filter(span -> SpanKind.CLIENT.equals(span.getKind()))
                .filter(span -> SPAN_CLIENT.equals(span.getName()))
                .count());
    }
}
