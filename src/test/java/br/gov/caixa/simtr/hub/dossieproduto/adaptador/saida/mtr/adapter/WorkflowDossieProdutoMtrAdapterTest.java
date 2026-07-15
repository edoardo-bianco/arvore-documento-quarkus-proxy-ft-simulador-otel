package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.WorkflowDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.workflow.WorkflowDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.WorkflowDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Vetoed;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowDossieProdutoMtrAdapterTest {

    @Test
    void mapeiaIdentificadorERespostaSemUsarDtoPublico() {
        FakeClient client = new FakeClient(Uni.createFrom().item(
                new WorkflowDossieProdutoMtrResponse(987L)));
        WorkflowDossieProdutoMtrAdapter adapter = new WorkflowDossieProdutoMtrAdapter(client);

        ResultadoWorkflowDossieProduto resultado = adapter
                .avancar(new IdentificadorDossieProduto(123L))
                .await().indefinitely();

        assertEquals(123L, client.identificadorRecebido);
        assertEquals(987L, resultado.identificadorDossieProduto());
    }

    @Test
    void traduzErroDeNegocioSemPerderDados() {
        WorkflowDossieProdutoMtrException.Erro erro =
                new WorkflowDossieProdutoMtrException.Erro(
                        400,
                        "simtr-dossie-produto",
                        "stub-workflow-400",
                        "MTR-DOS-WORKFLOW-400",
                        List.of(new WorkflowDossieProdutoMtrException.Mensagem("workflow nao permitido")),
                        "falha de negocio controlada",
                        "stacktrace externo"
                );
        WorkflowDossieProdutoMtrAdapter adapter = new WorkflowDossieProdutoMtrAdapter(
                new FakeClient(Uni.createFrom().failure(
                        new WorkflowDossieProdutoMtrException.Negocio(400, erro))));

        FalhaWorkflowDossieProduto falha = assertThrows(
                FalhaWorkflowDossieProduto.class,
                () -> adapter.avancar(new IdentificadorDossieProduto(123L)).await().indefinitely()
        );

        assertEquals(FalhaWorkflowDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(400, falha.status());
        assertEquals("simtr-dossie-produto", falha.recurso());
        assertEquals("stub-workflow-400", falha.idErro());
        assertEquals("MTR-DOS-WORKFLOW-400", falha.codigoErro());
        assertEquals(List.of("workflow nao permitido"), falha.mensagens());
        assertEquals("falha de negocio controlada", falha.detalhe());
        assertEquals("stacktrace externo", falha.stacktraceExterno());
    }

    @Test
    void traduzTimeoutSomenteDepoisDaFalhaDoClient() {
        TimeoutException timeout = new TimeoutException("tempo esgotado");
        WorkflowDossieProdutoMtrAdapter adapter = new WorkflowDossieProdutoMtrAdapter(
                new FakeClient(Uni.createFrom().failure(timeout)));

        FalhaWorkflowDossieProduto falha = assertThrows(
                FalhaWorkflowDossieProduto.class,
                () -> adapter.avancar(new IdentificadorDossieProduto(123L)).await().indefinitely()
        );

        assertEquals(FalhaWorkflowDossieProduto.Tipo.TIMEOUT, falha.tipo());
        assertSame(timeout, falha.getCause());
    }

    @Vetoed
    private static final class FakeClient implements WorkflowDossieProdutoMtrClient {

        private final Uni<WorkflowDossieProdutoMtrResponse> resposta;
        private Long identificadorRecebido;

        private FakeClient(Uni<WorkflowDossieProdutoMtrResponse> resposta) {
            this.resposta = resposta;
        }

        @Override
        public Uni<WorkflowDossieProdutoMtrResponse> iniciarOuAvancar(Long identificador) {
            identificadorRecebido = identificador;
            return resposta;
        }
    }
}
