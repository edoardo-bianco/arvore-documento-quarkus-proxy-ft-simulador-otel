package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CriarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class CriacaoDossieProdutoResourceQuarkusTest {

    @InjectMock
    CriarDossieProduto portaEntrada;

    @Test
    void resourceMapeiaContratoPublicoParaComandoInternoEResultadoParaResposta() {
        when(portaEntrada.executar(any()))
                .thenReturn(Uni.createFrom().item(new ResultadoCriacaoDossieProduto(654L)));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "processo": 101,
                          "chave_correlacao_canal": 202,
                          "numero_negocio": 303,
                          "clientes": [
                            {
                              "cpf": "12345678901",
                              "cnpj": "12345678000199",
                              "tipo_vinculo": 4,
                              "cliente_relacionado": {
                                "cpf": "10987654321",
                                "cnpj": "99876543000111"
                              },
                              "sequencia_titularidade": 5
                            }
                          ]
                        }
                        """)
                .when()
                .post("/simtr-hub/v1/dossie-produto")
                .then()
                .statusCode(201)
                .body("id", equalTo(654));

        ArgumentCaptor<ComandoCriacaoDossieProduto> captor =
                ArgumentCaptor.forClass(ComandoCriacaoDossieProduto.class);
        verify(portaEntrada).executar(captor.capture());

        ComandoCriacaoDossieProduto comando = captor.getValue();
        assertEquals(101L, comando.processo());
        assertEquals(202L, comando.chaveCorrelacaoCanal());
        assertEquals(303L, comando.numeroNegocio());
        assertEquals(1, comando.clientes().size());
        assertEquals("12345678901", comando.clientes().getFirst().cpf());
        assertEquals("12345678000199", comando.clientes().getFirst().cnpj());
        assertEquals(4L, comando.clientes().getFirst().tipoVinculo());
        assertEquals(5, comando.clientes().getFirst().sequenciaTitularidade());
        assertEquals("10987654321", comando.clientes().getFirst().clienteRelacionado().cpf());
        assertEquals("99876543000111", comando.clientes().getFirst().clienteRelacionado().cnpj());
    }

    @Test
    void resourcePreservaRespostaNulaDoContratoLegado() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().nullItem());

        given()
                .contentType("application/json")
                .body("{\"processo\":101,\"chave_correlacao_canal\":202}")
                .when()
                .post("/simtr-hub/v1/dossie-produto")
                .then()
                .statusCode(201)
                .body(emptyOrNullString());
    }
}
