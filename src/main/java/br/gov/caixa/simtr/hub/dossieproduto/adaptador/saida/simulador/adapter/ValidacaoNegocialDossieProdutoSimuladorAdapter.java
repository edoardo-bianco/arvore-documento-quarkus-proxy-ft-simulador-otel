package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ValidacaoNegocialSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ValidacaoNegocialDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ValidacaoNegocialDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@ValidacaoNegocialSimulador
public class ValidacaoNegocialDossieProdutoSimuladorAdapter
        implements SolicitarRegistroValidacaoNegocialDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            ValidacaoNegocialDossieProdutoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/validacao-negocial-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;
    private final ValidacaoNegocialDossieProdutoSimuladorMapper mapper;

    @Inject
    public ValidacaoNegocialDossieProdutoSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            ValidacaoNegocialDossieProdutoSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<Void> registrar(ComandoRegistroValidacaoNegocialDossieProduto comando) {
        ValidacaoNegocialDossieProdutoSimuladorResponse resposta =
                mockReader.readFirstJsonObject(
                        MOCK_RESOURCE,
                        ValidacaoNegocialDossieProdutoSimuladorResponse.class);
        if (resposta == null) {
            throw new IllegalStateException(
                    "Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE);
        }

        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.validacao-negocial.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "registrar-validacao-negocial-dossie-produto",
                        "dossie_produto_id", identificadorDossie(comando),
                        "origem", "mock"));
        return Uni.createFrom().item(mapper.paraResultado(resposta));
    }

    private static Long identificadorDossie(
            ComandoRegistroValidacaoNegocialDossieProduto comando
    ) {
        return comando != null ? comando.identificadorDossieProduto() : null;
    }
}
