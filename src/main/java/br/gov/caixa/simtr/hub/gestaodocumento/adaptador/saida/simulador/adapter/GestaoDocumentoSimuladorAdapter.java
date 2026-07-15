package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao.qualificador.GestaoDocumentoSimulador;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.dto.GestaoDocumentoSimuladorResponse;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.mapper.GestaoDocumentoSimuladorMapper;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@GestaoDocumentoSimulador
public class GestaoDocumentoSimuladorAdapter implements SolicitarCredencialContainer {

    private static final Logger LOG = Logger.getLogger(GestaoDocumentoSimuladorAdapter.class);
    private static final String MOCK_RESOURCE =
            "mock/gestaodocumento/credencial-container.md";

    private final MarkdownJsonMockReader mockReader;
    private final GestaoDocumentoSimuladorMapper mapper;

    @Inject
    public GestaoDocumentoSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            GestaoDocumentoSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<CredencialContainer> obter() {
        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.gestao-documento.credencial-container.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "GestaoDocumentoService",
                        "operacao", "gerar-credencial-container",
                        "origem", "mock"
                )
        );

        GestaoDocumentoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                MOCK_RESOURCE,
                GestaoDocumentoSimuladorResponse.class
        );
        if (resposta == null) {
            throw new IllegalStateException(
                    "Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE
            );
        }

        return Uni.createFrom().item(mapper.paraDominio(resposta));
    }
}
