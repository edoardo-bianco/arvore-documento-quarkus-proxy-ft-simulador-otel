package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConsultaDocumentosDossieProdutoResourceQuarkusTest {

    private static final String PATH = "/simtr-hub/v1/dossie-produto/{id}/documentos";

    @InjectMock
    ConsultarDocumentosDossieProduto consultarDocumentos;

    @Test
    void encaminhaTodosOsFiltrosParaPortaEMapeiaArray200() {
        when(consultarDocumentos.executar(any())).thenReturn(
                Uni.createFrom().item(List.of(documento())));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .queryParam("cnpj", "00000000000000")
                .queryParam("cpf", "00000000000")
                .queryParam("fase", 5033L)
                .queryParam("inclui-armazenamento", true)
                .queryParam("inclui-assinaturas", false)
                .queryParam("inclui-atributos", true)
                .queryParam("inclui-conformidade", false)
                .queryParam("inclui-outsourcing", true)
                .queryParam("inclui-propriedades", false)
                .queryParam("inclui-url", true)
                .queryParam("ip-usuario", "192.0.2.10")
                .queryParam("tipologia", "SIM-0001")
                .when()
                .get(PATH, 4081899L)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("size()", equalTo(1))
                .body("[0].id_instancia_documento", equalTo(9000001));

        var captor = ArgumentCaptor.forClass(
                CriteriosConsultaDocumentosDossieProduto.class);
        verify(consultarDocumentos).executar(captor.capture());
        var criterios = captor.getValue();
        assertEquals(4081899L, criterios.identificador().valor());
        assertEquals("00000000000000", criterios.cnpj());
        assertEquals("00000000000", criterios.cpf());
        assertEquals(5033L, criterios.fase());
        assertTrue(criterios.incluiArmazenamento());
        assertFalse(criterios.incluiAssinaturas());
        assertTrue(criterios.incluiAtributos());
        assertFalse(criterios.incluiConformidade());
        assertTrue(criterios.incluiOutsourcing());
        assertFalse(criterios.incluiPropriedades());
        assertTrue(criterios.incluiUrl());
        assertEquals("192.0.2.10", criterios.ipUsuario());
        assertEquals("SIM-0001", criterios.tipologia());
    }

    @Test
    void retorna204SemCorpoQuandoPortaEntregaListaVazia() {
        when(consultarDocumentos.executar(any())).thenReturn(
                Uni.createFrom().item(List.of()));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .get(PATH, 4081900L)
                .then()
                .statusCode(204)
                .body(equalTo(""));
    }

    @Test
    void traduzFalhaInternaParaErroPublico() {
        when(consultarDocumentos.executar(any())).thenReturn(Uni.createFrom().failure(
                new FalhaConsultaDocumentosDossieProduto(
                        FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO,
                        404,
                        "/dossies/4081999/documentos",
                        "erro-sintetico",
                        "MTRPRD0002",
                        List.of("documentos nao localizados"),
                        "detalhe sintetico",
                        "stacktrace externo sintetico",
                        null)));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .get(PATH, 4081999L)
                .then()
                .statusCode(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("codigo_http", equalTo(404))
                .body("codigo_erro", equalTo("MTRPRD0002"))
                .body("erros[0].mensagem", equalTo("documentos nao localizados"));
    }

    private static DocumentoDossieProdutoConsultado documento() {
        return new DocumentoDossieProdutoConsultado(
                9000001L,
                9000002L,
                "GED-SIMULADO-0001",
                "01/01/2030 10:00:00",
                null,
                "SIM0001",
                null,
                "Criado",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
