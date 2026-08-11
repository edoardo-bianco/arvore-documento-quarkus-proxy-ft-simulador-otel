package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import org.jboss.logging.Logger;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ProdutoDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ProdutoDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@ProdutoSimulador
public class ProdutoDossieProdutoSimuladorAdapter
        implements SolicitarAlteracaoProdutosContratadosDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            ProdutoDossieProdutoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/produto-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;
    private final ProdutoDossieProdutoSimuladorMapper mapper;

    @Inject
    public ProdutoDossieProdutoSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            ProdutoDossieProdutoSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<Void> alterar(ComandoAlteracaoProdutosContratadosDossieProduto comando) {
        ProdutoDossieProdutoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                MOCK_RESOURCE,
                ProdutoDossieProdutoSimuladorResponse.class);
        if (resposta == null) {
            throw new IllegalStateException(
                    "Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE);
        }

        Long identificador = comando != null ? comando.identificadorDossieProduto() : null;
        Integer quantidadeProdutos = comando != null && comando.produtos() != null
                ? comando.produtos().size() : null;
        Span span = Span.current();
        span.setAttribute("simtr_hub.origem_dados", "mock");
        setLongAttribute(span, "dossie_produto.id", identificador);
        setIntAttribute(span, "dossie_produto.produtos.quantidade", quantidadeProdutos);
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.produto.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "alterar-produtos-contratados-dossie-produto",
                        "dossie_produto_id", identificador,
                        "produtos_quantidade", quantidadeProdutos,
                        "origem", "mock"));
        return Uni.createFrom().item(mapper.paraResultado(resposta));
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

    private static void setIntAttribute(Span span, String nome, Integer valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
