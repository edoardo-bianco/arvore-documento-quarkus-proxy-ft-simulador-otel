package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ConsultaDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ConsultaDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
@ConsultaDossieProdutoSimulador
public class ConsultaDossieProdutoSimuladorAdapter implements ObterDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            ConsultaDossieProdutoSimuladorAdapter.class
    );
    private static final String MOCK_RESOURCE_TEMPLATE =
            "mock/dossieproduto/%d-v2-consulta-dossie-produto.md";
    private static final String SERVICO = "simtr-dossie-produto";
    private static final String CODIGO_NAO_ENCONTRADO = "DOSSIE_PRODUTO_NAO_ENCONTRADO";

    private final MarkdownJsonMockReader mockReader;
    private final ConsultaDossieProdutoSimuladorMapper mapper;

    @Inject
    public ConsultaDossieProdutoSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            ConsultaDossieProdutoSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<DossieProdutoConsultado> obter(IdentificadorDossieProduto identificador) {
        Long id = identificador != null ? identificador.valor() : null;
        Span span = Span.current();
        span.setAttribute("simtr_hub.origem_dados", "mock");
        if (id != null) {
            span.setAttribute("dossie_produto.id", id);
        }
        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.consulta.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "consultar-dossie-produto",
                        "dossie_produto_id", id,
                        "origem", "mock"
                )
        );

        if (id == null) {
            return Uni.createFrom().failure(naoEncontrado(null));
        }
        String resourceName = MOCK_RESOURCE_TEMPLATE.formatted(id);
        ConsultaDossieProdutoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                resourceName,
                ConsultaDossieProdutoSimuladorResponse.class
        );
        if (resposta == null) {
            return Uni.createFrom().failure(naoEncontrado(id));
        }
        return Uni.createFrom().item(mapper.paraDominio(resposta));
    }

    private static FalhaConsultaDossieProduto naoEncontrado(Long identificador) {
        String valor = String.valueOf(identificador);
        return new FalhaConsultaDossieProduto(
                FalhaConsultaDossieProduto.Tipo.NEGOCIO,
                404,
                SERVICO,
                "mock-consulta-dossie-produto-" + valor,
                CODIGO_NAO_ENCONTRADO,
                List.of("Dossie produto " + valor + " nao encontrado no simulador."),
                null,
                null,
                null
        );
    }
}
