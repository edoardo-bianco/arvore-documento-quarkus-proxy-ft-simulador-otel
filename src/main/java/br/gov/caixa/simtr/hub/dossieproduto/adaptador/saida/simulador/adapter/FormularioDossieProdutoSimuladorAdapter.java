package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.FormularioSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.FormularioDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@FormularioSimulador
public class FormularioDossieProdutoSimuladorAdapter
        implements SolicitarAtualizacaoFormularioDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            FormularioDossieProdutoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/formulario-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public FormularioDossieProdutoSimuladorAdapter(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    @Override
    public Uni<ResultadoAtualizacaoFormularioDossieProduto> atualizar(
            ComandoAtualizacaoFormularioDossieProduto comando
    ) {
        FormularioDossieProdutoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                MOCK_RESOURCE,
                FormularioDossieProdutoSimuladorResponse.class);
        if (resposta == null) {
            throw new IllegalStateException(
                    "Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE);
        }

        Long idInformado = comando != null ? comando.identificadorDossieProduto() : null;
        Long id = idInformado != null ? idInformado : resposta.id();
        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.formulario.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "atualizar-formulario-dossie-produto",
                        "dossie_produto_id", idInformado,
                        "origem", "mock"));
        return Uni.createFrom().item(new ResultadoAtualizacaoFormularioDossieProduto(id));
    }
}
