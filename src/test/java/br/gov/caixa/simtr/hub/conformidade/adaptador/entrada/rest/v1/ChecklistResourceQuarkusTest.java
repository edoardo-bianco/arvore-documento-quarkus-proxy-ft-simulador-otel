package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ChecklistResourceQuarkusTest {

    private static final String PATH =
            "/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}";
    private static final String MEDIA_TYPE_JSON = "application/json";

    @InjectMock
    ConsultarChecklist portaEntrada;

    @Test
    void resourceUsaSomentePortaEntradaEMapeiaRespostaPublica() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().item(new Checklist(
                "Resposta da porta nova",
                321L,
                7,
                null,
                "14/07/2026 12:00:00",
                false,
                null,
                Arrays.asList(null, new ApontamentoChecklist(
                        654L, null, "Descricao", null, false, 1))
        )));

        given()
                .accept(MEDIA_TYPE_JSON)
                .when()
                .get(PATH, 321L, 7)
                .then()
                .statusCode(200)
                .contentType(MEDIA_TYPE_JSON)
                .body("identificador_negocial", equalTo(321))
                .body("versao", equalTo(7))
                .body("nome", equalTo("Resposta da porta nova"))
                .body("verificacao_previa", equalTo(false))
                .body("$", not(hasKey("data_hora_criacao")))
                .body("$", not(hasKey("orientacao_operador")))
                .body("apontamentos[0]", equalTo(null))
                .body("apontamentos[1].identificador_negocial", equalTo(654))
                .body("apontamentos[1]", not(hasKey("nome")));

        ArgumentCaptor<ComandoConsultaChecklist> captor =
                ArgumentCaptor.forClass(ComandoConsultaChecklist.class);
        verify(portaEntrada).executar(captor.capture());
        assertEquals(321L, captor.getValue().identificadorNegocial());
        assertEquals(7, captor.getValue().versao());
    }

    @Test
    void resourcePreservaRespostaSemConteudo() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().nullItem());

        given()
                .accept(MEDIA_TYPE_JSON)
                .when()
                .get(PATH, 321L, 7)
                .then()
                .statusCode(204);
    }

    @Test
    void resourceTraduzFalhaInternaParaErroPublicoCompleto() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().failure(
                new FalhaConsultaChecklist(
                        FalhaConsultaChecklist.Tipo.NEGOCIO,
                        404,
                        "simtr-parametrizacao",
                        "checklist-404",
                        "MTR-CHECKLIST-404",
                        List.of("checklist nao localizado"),
                        "falha controlada",
                        "stack-remota",
                        null)));

        given()
                .accept(MEDIA_TYPE_JSON)
                .when()
                .get(PATH, 321L, 7)
                .then()
                .statusCode(404)
                .contentType(MEDIA_TYPE_JSON)
                .body("codigo_http", equalTo(404))
                .body("recurso", equalTo("simtr-parametrizacao"))
                .body("id_erro", equalTo("checklist-404"))
                .body("codigo_erro", equalTo("MTR-CHECKLIST-404"))
                .body("erros[0].mensagem", equalTo("checklist nao localizado"))
                .body("detalhe", equalTo("falha controlada"))
                .body("stacktrace", equalTo("stack-remota"));
    }
}
