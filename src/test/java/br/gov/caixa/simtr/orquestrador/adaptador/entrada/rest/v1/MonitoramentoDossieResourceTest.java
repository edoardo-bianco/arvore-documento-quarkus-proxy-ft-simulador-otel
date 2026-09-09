package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieRequest;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieResponse;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.IniciarMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Contrato HTTP local com porta simulada e Service Bus desabilitado. */
@QuarkusTest
class MonitoramentoDossieResourceTest {

    private static final String PATH = "/simtr-hub/v1/monitoramentos-dossie";
    private static final String VALIDO = "{\"idDossiePreValidacao\":\"pre-em-analise\",\"idDossieMtr\":\"0007\"}";
    private static final MonitoramentoIniciado RESULTADO = new MonitoramentoIniciado("MON-SERVIDOR", "ORQ-SERVIDOR");

    @InjectMock
    IniciarMonitoramento iniciar;

    @Inject
    MonitoramentoDossieResource resource;

    @BeforeEach
    void prepararPorta() {
        reset(iniciar);
        when(iniciar.executar(any())).thenReturn(Uni.createFrom().item(RESULTADO));
    }

    @Test
    void deveResponder202ComIdsDoServidorEPreservarIdentificadores() {
        given().contentType(ContentType.JSON).body(VALIDO)
                .when().post(PATH).then().statusCode(202).contentType(ContentType.JSON)
                .body("monitoramentoId", equalTo("MON-SERVIDOR"),
                        "orquestracaoId", equalTo("ORQ-SERVIDOR"), "size()", equalTo(2));
        verify(iniciar).executar(new SolicitacaoMonitoramento("pre-em-analise", "0007"));
    }

    @Test
    void deveIgnorarCamposDesconhecidosSemAceitarIdsGeradosPeloCliente() {
        given().contentType(ContentType.JSON)
                .body(VALIDO.replace("}", ",\"monitoramentoId\":\"id-cliente\",\"tentativaAtual\":99}"))
                .when().post(PATH).then().statusCode(202).body("monitoramentoId", equalTo("MON-SERVIDOR"));
        verify(iniciar).executar(new SolicitacaoMonitoramento("pre-em-analise", "0007"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "{}", "{\"idDossieMtr\":\"7\"}",
            "{\"idDossiePreValidacao\":\"pre\"}",
            "{\"idDossiePreValidacao\":\" \",\"idDossieMtr\":\"7\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"0\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"-1\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"+7\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"7.1\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"9223372036854775808\"}"
    })
    void deveRejeitarRequestInvalidoAntesDaPorta(String corpo) {
        given().contentType(ContentType.JSON).body(corpo)
                .when().post(PATH).then().statusCode(400)
                .body("codigo_http", equalTo(400), "codigo_erro", equalTo("ARVDOCP0001"));
        verifyNoInteractions(iniciar);
    }

    @Test
    void deveRejeitarJsonMalformadoAntesDaPorta() {
        given().contentType(ContentType.JSON).body("{")
                .when().post(PATH).then().statusCode(400);
        verifyNoInteractions(iniciar);
    }

    @Test
    void devePreservarLimiteMaximoMtrEmString() {
        var corpo = "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"09223372036854775807\"}";
        given().contentType(ContentType.JSON).body(corpo)
                .when().post(PATH).then().statusCode(202);
        verify(iniciar).executar(new SolicitacaoMonitoramento("pre", "09223372036854775807"));
    }

    @Test
    void deveResponderErroPadraoSemDetalhesDaFalhaAssincrona() {
        when(iniciar.executar(any())).thenReturn(Uni.createFrom().failure(new IllegalStateException("dado-restrito")));
        var corpo = given().contentType(ContentType.JSON).body(VALIDO)
                .when().post(PATH).then().statusCode(500).contentType(ContentType.JSON)
                .body("codigo_http", equalTo(500), "recurso", equalTo("simtr-hub"),
                        "codigo_erro", equalTo("ARVDOCP9999"), "id_erro", notNullValue(),
                        "erros[0].mensagem", equalTo("Erro interno ao processar a requisição."),
                        "$", not(hasKey("stacktrace")), "$", not(hasKey("detalhe")))
                .extract().asString();
        assertFalse(corpo.contains("dado-restrito"));
    }

    @Test
    void deveSanitizarTambemFalhaSincronaDaPorta() {
        when(iniciar.executar(any())).thenThrow(new IllegalStateException("dado-restrito"));
        given().contentType(ContentType.JSON).body(VALIDO)
                .when().post(PATH).then().statusCode(500)
                .body("codigo_erro", equalTo("ARVDOCP9999"),
                        "erros[0].mensagem", equalTo("Erro interno ao processar a requisição."));
    }

    @Test
    void deveProduzirResponseSomenteDepoisDaConclusaoDaPorta() throws Exception {
        var confirmacao = new CompletableFuture<MonitoramentoIniciado>();
        when(iniciar.executar(any())).thenReturn(Uni.createFrom().completionStage(confirmacao));
        var resposta = resource.iniciar(new IniciarMonitoramentoDossieRequest("pre", "7"))
                .subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(resposta.isDone());
        confirmacao.complete(RESULTADO);
        try (var concluida = resposta.get(3, TimeUnit.SECONDS)) {
            assertEquals(202, concluida.getStatus());
            assertEquals(new IniciarMonitoramentoDossieResponse("MON-SERVIDOR", "ORQ-SERVIDOR"), concluida.getEntity());
        }
    }
}
