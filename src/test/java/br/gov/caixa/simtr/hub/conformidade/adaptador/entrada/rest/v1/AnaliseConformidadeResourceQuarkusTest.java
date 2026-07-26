package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.RevisarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class AnaliseConformidadeResourceQuarkusTest {

    private static final String BASE_PATH = "/simtr-hub/v1/conformidade/analises";
    private static final String CORRELATION_ID = "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String IDENTIFICADOR_DOCUMENTO = "DOC-2026-000123";

    @InjectMock
    IniciarAnaliseConformidade iniciar;

    @InjectMock
    ConsultarAnaliseConformidade consultar;

    @InjectMock
    RevisarAnaliseConformidade revisar;

    @Test
    void postMapeiaSolicitacaoERetornaLocationComEstadoInicial() {
        when(iniciar.executar(any()))
                .thenReturn(Uni.createFrom().item(visaoEmProcessamento()));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "identificadorDocumento": "DOC-2026-000123",
                          "texto": "Texto para análise",
                          "identificadorChecklist": 1000012583,
                          "versaoChecklist": 1
                        }
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(202)
                .header("Location", BASE_PATH + "/instancia-123")
                .contentType(ContentType.JSON)
                .body("correlationId", equalTo(CORRELATION_ID))
                .body("instanceId", equalTo("instancia-123"))
                .body("identificadorDocumento", equalTo(IDENTIFICADOR_DOCUMENTO))
                .body("identificadorChecklist", equalTo(1000012583))
                .body("versaoChecklist", equalTo(1))
                .body("status", equalTo("EM_PROCESSAMENTO"))
                .body("$", not(hasKey("resultadoPreliminar")));

        ArgumentCaptor<SolicitacaoAnaliseConformidade> captor =
                ArgumentCaptor.forClass(SolicitacaoAnaliseConformidade.class);
        verify(iniciar).executar(captor.capture());
        UUID.fromString(captor.getValue().correlationId());
        assertEquals(IDENTIFICADOR_DOCUMENTO, captor.getValue().identificadorDocumento());
        assertEquals("Texto para análise", captor.getValue().texto());
        assertEquals(1000012583L, captor.getValue().identificadorChecklist());
        assertEquals(1, captor.getValue().versaoChecklist());
    }

    @Test
    void getMapeiaVisaoCompletaSemExporOrigemInterna() {
        when(consultar.executar("instancia-123"))
                .thenReturn(Uni.createFrom().item(
                        visaoEmProcessamento().aguardandoRevisao(resultadoPreliminar())));

        given()
                .accept(ContentType.JSON)
                .when()
                .get(BASE_PATH + "/{instanceId}", "instancia-123")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("correlationId", equalTo(CORRELATION_ID))
                .body("instanceId", equalTo("instancia-123"))
                .body("identificadorDocumento", equalTo(IDENTIFICADOR_DOCUMENTO))
                .body("identificadorChecklist", equalTo(1000012583))
                .body("versaoChecklist", equalTo(1))
                .body("status", equalTo("AGUARDANDO_REVISAO"))
                .body("resultadoPreliminar.identificadorChecklist", equalTo(1000012583))
                .body("resultadoPreliminar.apontamentos[0].identificadorApontamento", equalTo(10))
                .body("resultadoPreliminar.apontamentos[0].parecer", equalTo("INCONCLUSIVO"))
                .body("resultadoPreliminar.apontamentos[0].evidencia", equalTo(null))
                .body("resultadoPreliminar", not(hasKey("origem")))
                .body("resultadoFinal", equalTo(null))
                .body("mensagemErro", equalTo(null));

        verify(consultar).executar("instancia-123");
    }

    @Test
    void putMapeiaListaCompletaERetornaAcceptedSemCorpo() {
        when(revisar.executar(eq("instancia-123"), any()))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "observacao": "Revisão realizada pelo operador.",
                          "apontamentos": [{
                            "identificadorApontamento": 10,
                            "nomeApontamento": "Documento identificado",
                            "parecer": "CONFORME",
                            "justificativa": "Confirmado",
                            "evidencia": "Trecho revisado",
                            "confianca": 0.5
                          }]
                        }
                        """)
                .when()
                .put(BASE_PATH + "/{instanceId}/revisao", "instancia-123")
                .then()
                .statusCode(202)
                .body(equalTo(""));

        ArgumentCaptor<RevisaoHumanaConformidade> captor =
                ArgumentCaptor.forClass(RevisaoHumanaConformidade.class);
        verify(revisar).executar(eq("instancia-123"), captor.capture());
        assertEquals("Revisão realizada pelo operador.", captor.getValue().observacao());
        assertEquals(ParecerConformidade.CONFORME, captor.getValue().apontamentos().getFirst().parecer());
        assertEquals(0.5d, captor.getValue().apontamentos().getFirst().confianca());
    }

    private static ResultadoAnaliseConformidade resultadoPreliminar() {
        return new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist documental",
                "Resultado preliminar",
                List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.INCONCLUSIVO,
                        "Requer revisão",
                        null,
                        0.5d)),
                OrigemResultado.AGENTE);
    }

    private static VisaoAnaliseConformidade visaoEmProcessamento() {
        return VisaoAnaliseConformidade.emProcessamento(
                CORRELATION_ID,
                "instancia-123",
                IDENTIFICADOR_DOCUMENTO,
                1000012583L,
                1);
    }
}
