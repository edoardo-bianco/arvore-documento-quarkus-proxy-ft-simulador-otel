package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CriacaoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.CriacaoDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@CriacaoSimulador
public class CriacaoDossieProdutoSimuladorAdapter implements SolicitarCriacaoDossieProduto {

    private static final Logger LOG = Logger.getLogger(CriacaoDossieProdutoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE = "mock/dossieproduto/criacao-basica-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public CriacaoDossieProdutoSimuladorAdapter(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    @Override
    public Uni<ResultadoCriacaoDossieProduto> criar(ComandoCriacaoDossieProduto comando) {
        CriacaoDossieProdutoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                MOCK_RESOURCE,
                CriacaoDossieProdutoSimuladorResponse.class);
        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE);
        }

        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "criar-dossie-produto",
                        "processo", processo(comando),
                        "chave_correlacao_canal", chaveCorrelacaoCanal(comando),
                        "origem", "mock"));
        return Uni.createFrom().item(new ResultadoCriacaoDossieProduto(resposta.id()));
    }

    private static Long processo(ComandoCriacaoDossieProduto comando) {
        return comando != null ? comando.processo() : null;
    }

    private static Long chaveCorrelacaoCanal(ComandoCriacaoDossieProduto comando) {
        return comando != null ? comando.chaveCorrelacaoCanal() : null;
    }
}
