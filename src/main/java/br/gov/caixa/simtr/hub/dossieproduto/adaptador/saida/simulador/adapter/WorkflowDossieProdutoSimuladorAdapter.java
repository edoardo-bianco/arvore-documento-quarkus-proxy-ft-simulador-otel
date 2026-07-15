package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.WorkflowSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.WorkflowDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@WorkflowSimulador
public class WorkflowDossieProdutoSimuladorAdapter implements AvancarWorkflowDossieProduto {

    private static final Logger LOG = Logger.getLogger(WorkflowDossieProdutoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE = "mock/dossieproduto/workflow-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public WorkflowDossieProdutoSimuladorAdapter(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    @Override
    public Uni<ResultadoWorkflowDossieProduto> avancar(IdentificadorDossieProduto identificador) {
        WorkflowDossieProdutoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                MOCK_RESOURCE,
                WorkflowDossieProdutoSimuladorResponse.class);
        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE);
        }

        Long id = identificador.valor() != null ? identificador.valor() : resposta.id();
        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.workflow.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                        "dossie_produto_id", identificador.valor(),
                        "origem", "mock"));
        return Uni.createFrom().item(new ResultadoWorkflowDossieProduto(id));
    }
}
