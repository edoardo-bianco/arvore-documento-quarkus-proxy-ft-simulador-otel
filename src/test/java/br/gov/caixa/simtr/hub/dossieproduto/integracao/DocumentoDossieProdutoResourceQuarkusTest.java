package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IncluirDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
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
class DocumentoDossieProdutoResourceQuarkusTest {

    @InjectMock
    IncluirDocumentoDossieProduto portaEntrada;

    @Test
    void resourceMapeiaContratoPublicoParaComandoInternoEResultadoParaResposta() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().item(
                new ResultadoInclusaoDocumentoDossieProduto(654L, 987L)));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "id": 10,
                          "path_storage": "container/documento.pdf",
                          "codigo_ged": "GED123",
                          "object_store_ged": "OBJECT_STORE",
                          "tipo_documento": "RG",
                          "vinculo_dossie": {
                            "cliente": {
                              "cpf": "12345678901",
                              "tipo_vinculo": 1
                            },
                            "elemento_conteudo": 700,
                            "garantia": {
                              "codigo_bacen": 300,
                              "produto_operacao": 400,
                              "produto_modalidade": 500,
                              "cliente_avalista": [
                                {"cnpj": "12345678000190", "tipo_vinculo": 2}
                              ]
                            }
                          },
                          "atributos": [
                            {
                              "chave": "numero",
                              "valor": "12345",
                              "objeto": "documento",
                              "opcoes_selecionadas": ["opcao-1"]
                            }
                          ],
                          "propriedades": [
                            {
                              "chave": "origem",
                              "valor": "pre-validacao",
                              "objeto": "documento"
                            }
                          ]
                        }
                        """)
                .when()
                .post("/simtr-hub/v1/dossie-produto/123/documento")
                .then()
                .statusCode(201)
                .body("id_documento", equalTo(654))
                .body("id_instancia_documento", equalTo(987));

        ArgumentCaptor<ComandoInclusaoDocumentoDossieProduto> captor =
                ArgumentCaptor.forClass(ComandoInclusaoDocumentoDossieProduto.class);
        verify(portaEntrada).executar(captor.capture());

        ComandoInclusaoDocumentoDossieProduto comando = captor.getValue();
        assertEquals(123L, comando.identificadorDossieProduto());
        assertEquals(10L, comando.identificadorDocumento());
        assertEquals("container/documento.pdf", comando.caminhoArmazenamento());
        assertEquals("GED123", comando.codigoGed());
        assertEquals("OBJECT_STORE", comando.repositorioGed());
        assertEquals("RG", comando.tipoDocumento());
        assertEquals("12345678901", comando.vinculoDossie().cliente().cpf());
        assertEquals(1L, comando.vinculoDossie().cliente().tipoVinculo());
        assertEquals(700L, comando.vinculoDossie().elementoConteudo());
        assertEquals(300, comando.vinculoDossie().garantia().codigoBacen());
        assertEquals("12345678000190",
                comando.vinculoDossie().garantia().clientesAvalistas().getFirst().cnpj());
        assertEquals("numero", comando.atributos().getFirst().chave());
        assertEquals("origem", comando.propriedades().getFirst().chave());
    }

    @Test
    void resourcePreservaRespostaNulaDoContratoLegado() {
        when(portaEntrada.executar(any())).thenReturn(Uni.createFrom().nullItem());

        given()
                .contentType("application/json")
                .body("{\"tipo_documento\":\"RG\"}")
                .when()
                .post("/simtr-hub/v1/dossie-produto/123/documento")
                .then()
                .statusCode(201)
                .body(emptyOrNullString());
    }
}
