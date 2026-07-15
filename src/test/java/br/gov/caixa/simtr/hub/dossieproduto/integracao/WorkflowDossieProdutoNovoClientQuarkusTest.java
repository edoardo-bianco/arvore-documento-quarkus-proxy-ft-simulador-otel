package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.WorkflowDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.workflow.WorkflowDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.WorkflowDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(value = DossieProdutoMtrStubTestResource.class, restrictToAnnotatedClass = true)
class WorkflowDossieProdutoNovoClientQuarkusTest {

    private static final String CAMINHO_WORKFLOW =
            "/simtr/dossie-produto/v1/dossie-produto/123/workflow";

    @Inject
    @RestClient
    WorkflowDossieProdutoMtrClient client;

    @Inject
    AvancarWorkflowDossieProduto portaSaida;

    @BeforeEach
    void resetStub() {
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void percorreNovoClientAteStubComWireHeadersOidcEResposta() {
        DossieProdutoMtrStubTestResource.responder(200, "{\"id\":4321}");

        WorkflowDossieProdutoMtrResponse resposta = client.iniciarOuAvancar(123L)
                .await().indefinitely();

        assertEquals(4321L, resposta.id());
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());
        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("POST", request.method());
        assertEquals(CAMINHO_WORKFLOW, request.path());
        assertEquals("", request.body());
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
    }

    @Test
    void classificaNegocioDentroDoClientESemRetry() {
        DossieProdutoMtrStubTestResource.responder(400, """
                {"codigo_http":400,"recurso":"simtr-dossie-produto",
                 "id_erro":"novo-client-400","codigo_erro":"MTR-WORKFLOW-400",
                 "erros":[{"mensagem":"workflow nao permitido"}],"detalhe":"negocio"}
                """);

        WorkflowDossieProdutoMtrException.Negocio falha = assertThrows(
                WorkflowDossieProdutoMtrException.Negocio.class,
                () -> client.iniciarOuAvancar(123L).await().indefinitely());

        assertEquals("novo-client-400", falha.erro().idErro());
        assertEquals("workflow nao permitido", falha.erro().erros().getFirst().mensagem());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void executaRetryDoNovoClientAposErroRecuperavel() {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto"}
                """);
        DossieProdutoMtrStubTestResource.responder(200, "{\"id\":4322}");

        WorkflowDossieProdutoMtrResponse resposta = client.iniciarOuAvancar(123L)
                .await().indefinitely();

        assertEquals(4322L, resposta.id());
        assertEquals(2, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void selecionaAdapterMtrQuandoSimuladorEstaDesabilitado() {
        DossieProdutoMtrStubTestResource.responder(200, "{\"id\":4323}");

        assertEquals(4323L, portaSaida.avancar(new IdentificadorDossieProduto(123L))
                .await().indefinitely().identificadorDossieProduto());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

}
