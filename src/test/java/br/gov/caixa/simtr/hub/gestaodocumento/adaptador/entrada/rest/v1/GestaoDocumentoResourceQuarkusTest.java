package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.entrada.ObterCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.erro.FalhaObtencaoCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class GestaoDocumentoResourceQuarkusTest {

    private static final String PATH = "/simtr-hub/v1/storage/container/credencial";
    private static final String MEDIA_TYPE_JSON = "application/json";
    private static final String CAMPO_URL_STORAGE = "url_storage";
    private static final String CAMPO_NOME_CONTAINER = "nome_container";

    @InjectMock
    ObterCredencialContainer portaEntrada;

    @Test
    void resourceUsaSomentePortaEntradaEMapeiaRespostaPublica() {
        when(portaEntrada.executar()).thenReturn(Uni.createFrom().item(new CredencialContainer(
                "sv=teste&sig=valor-opaco",
                Map.of("instante", "2099-12-31T23:59:47Z"),
                "https://storage.invalid",
                "container-porta-nova"
        )));

        given()
                .accept(MEDIA_TYPE_JSON)
                .when()
                .post(PATH)
                .then()
                .statusCode(200)
                .contentType(MEDIA_TYPE_JSON)
                .body("sas", equalTo("sv=teste&sig=valor-opaco"))
                .body("validade.instante", equalTo("2099-12-31T23:59:47Z"))
                .body(CAMPO_URL_STORAGE, equalTo("https://storage.invalid"))
                .body(CAMPO_NOME_CONTAINER, equalTo("container-porta-nova"));

        verify(portaEntrada).executar();
        assertArrayEquals(
                new Class<?>[]{ObterCredencialContainer.class},
                GestaoDocumentoResource.class.getConstructors()[0].getParameterTypes()
        );
    }

    @Test
    void resourcePreservaOsQuatroCamposQuandoNulos() {
        when(portaEntrada.executar()).thenReturn(Uni.createFrom().item(
                new CredencialContainer(null, null, null, null)
        ));

        given()
                .accept(MEDIA_TYPE_JSON)
                .when()
                .post(PATH)
                .then()
                .statusCode(200)
                .contentType(MEDIA_TYPE_JSON)
                .body("$", hasKey("sas"))
                .body("$", hasKey("validade"))
                .body("$", hasKey(CAMPO_URL_STORAGE))
                .body("$", hasKey(CAMPO_NOME_CONTAINER))
                .body("sas", equalTo(null))
                .body("validade", equalTo(null))
                .body(CAMPO_URL_STORAGE, equalTo(null))
                .body(CAMPO_NOME_CONTAINER, equalTo(null));
    }

    @Test
    void resourceTraduzFalhaInternaParaErroPublicoCompleto() {
        when(portaEntrada.executar()).thenReturn(Uni.createFrom().failure(
                new FalhaObtencaoCredencialContainer(
                        FalhaObtencaoCredencialContainer.Tipo.NEGOCIO,
                        404,
                        "simtr-gestao-documento",
                        "credencial-404",
                        "MTR-CREDENCIAL-404",
                        List.of("container nao localizado"),
                        "falha controlada",
                        "stack-remota",
                        null
                )
        ));

        given()
                .accept(MEDIA_TYPE_JSON)
                .when()
                .post(PATH)
                .then()
                .statusCode(404)
                .contentType(MEDIA_TYPE_JSON)
                .body("codigo_http", equalTo(404))
                .body("recurso", equalTo("simtr-gestao-documento"))
                .body("id_erro", equalTo("credencial-404"))
                .body("codigo_erro", equalTo("MTR-CREDENCIAL-404"))
                .body("erros[0].mensagem", equalTo("container nao localizado"))
                .body("detalhe", equalTo("falha controlada"))
                .body("stacktrace", equalTo("stack-remota"));
    }
}
