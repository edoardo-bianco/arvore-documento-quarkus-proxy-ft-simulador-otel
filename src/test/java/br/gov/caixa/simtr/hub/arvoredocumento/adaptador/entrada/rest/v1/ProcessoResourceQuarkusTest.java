package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.entrada.ConsultarProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.erro.FalhaConsultaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProcessoResourceQuarkusTest {

    private static final String PATH =
            "/simtr-hub/v1/processo/identificador-negocial/{identificador}";

    @InjectMock
    ConsultarProcessoParametrizado portaEntrada;

    @Test
    void resourceUsaSomentePortaEntradaEMapeiaRespostaPublica() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().item(
                new ProcessoParametrizado(
                        321L, "Resposta da porta nova", true, null, false,
                        null, List.of(), null, List.of(), List.of(), null)));

        given()
                .accept("application/json")
                .when()
                .get(PATH, 321L)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("identificador_negocial", equalTo(321))
                .body("nome", equalTo("Resposta da porta nova"))
                .body("indicador_produto_obrigatorio", equalTo(false))
                .body("$", not(hasKey("ultima_alteracao")))
                .body("$", not(hasKey("produtos")));

        ArgumentCaptor<IdentificadorNegocialProcesso> captor =
                ArgumentCaptor.forClass(IdentificadorNegocialProcesso.class);
        verify(portaEntrada).executar(captor.capture());
        assertEquals(321L, captor.getValue().valor());
    }

    @Test
    void resourceTraduzFalhaInternaParaErroPublicoCompleto() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().failure(
                new FalhaConsultaProcessoParametrizado(
                        FalhaConsultaProcessoParametrizado.Tipo.NEGOCIO,
                        404,
                        "simtr-parametrizacao",
                        "processo-404",
                        "MTR-PROCESSO-404",
                        List.of("processo nao localizado"),
                        "falha controlada",
                        "stack-remota",
                        null)));

        given()
                .accept("application/json")
                .when()
                .get(PATH, 321L)
                .then()
                .statusCode(404)
                .contentType("application/json")
                .body("codigo_http", equalTo(404))
                .body("recurso", equalTo("simtr-parametrizacao"))
                .body("id_erro", equalTo("processo-404"))
                .body("codigo_erro", equalTo("MTR-PROCESSO-404"))
                .body("erros[0].mensagem", equalTo("processo nao localizado"))
                .body("detalhe", equalTo("falha controlada"))
                .body("stacktrace", equalTo("stack-remota"));
    }
}
