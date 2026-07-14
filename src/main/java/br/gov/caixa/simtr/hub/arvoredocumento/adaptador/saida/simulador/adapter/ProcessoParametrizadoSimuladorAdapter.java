package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.configuracao.qualificador.ProcessoSimulador;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.dto.ProcessoParametrizadoSimuladorResponse;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.mapper.ProcessoParametrizadoSimuladorMapper;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jboss.logging.Logger;

@ApplicationScoped
@ProcessoSimulador
public class ProcessoParametrizadoSimuladorAdapter implements ObterProcessoParametrizado {

    private static final Logger LOG = Logger.getLogger(
            ProcessoParametrizadoSimuladorAdapter.class
    );
    private static final long DEFAULT_IDENTIFICADOR_NEGOCIAL = 1000016487L;
    private static final String MOCK_RESOURCE_TEMPLATE =
            "mock/parametrizacao/%d-consulta-processo-parametrizacao-v2-identificador-negocial.md";
    private static final String DEFAULT_MOCK_RESOURCE =
            mockResourceName(DEFAULT_IDENTIFICADOR_NEGOCIAL);

    private final MarkdownJsonMockReader mockReader;
    private final ProcessoParametrizadoSimuladorMapper mapper;

    @Inject
    public ProcessoParametrizadoSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            ProcessoParametrizadoSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<ProcessoParametrizado> obter(IdentificadorNegocialProcesso identificador) {
        Long identificadorInformado = identificador != null ? identificador.valor() : null;
        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.processo.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "ProcessoService",
                        "operacao", "consultar-processo",
                        "identificador_negocial", identificadorInformado,
                        "origem", "mock"
                )
        );

        long identificadorMock = Objects.requireNonNullElse(
                identificadorInformado,
                DEFAULT_IDENTIFICADOR_NEGOCIAL
        );
        String resourceName = mockResourceName(identificadorMock);
        ProcessoParametrizadoSimuladorResponse resposta = mockReader.readFirstJsonObject(
                resourceName,
                ProcessoParametrizadoSimuladorResponse.class
        );

        if (resposta == null && !DEFAULT_MOCK_RESOURCE.equals(resourceName)) {
            resourceName = DEFAULT_MOCK_RESOURCE;
            resposta = mockReader.readFirstJsonObject(
                    resourceName,
                    ProcessoParametrizadoSimuladorResponse.class
            );
        }
        if (resposta == null) {
            throw new IllegalStateException(
                    "Arquivo de mock nao encontrado no classpath: " + resourceName
            );
        }

        return Uni.createFrom().item(mapper.paraDominio(resposta));
    }

    private static String mockResourceName(long identificador) {
        return MOCK_RESOURCE_TEMPLATE.formatted(identificador);
    }
}
