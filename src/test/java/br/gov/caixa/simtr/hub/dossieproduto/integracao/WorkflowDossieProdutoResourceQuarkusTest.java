package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IniciarOuAvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class WorkflowDossieProdutoResourceQuarkusTest {

    @InjectMock
    IniciarOuAvancarWorkflowDossieProduto portaEntrada;

    @Test
    void resourceUsaPortaDeEntradaEMapeiaResultadoParaContratoPublico() {
        when(portaEntrada.executar(any()))
                .thenReturn(Uni.createFrom().item(new ResultadoWorkflowDossieProduto(654L)));

        given()
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L)
                .then()
                .statusCode(200)
                .body("id", equalTo(654));
    }
}
