package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.qualificador.ChecklistSimulador;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.dto.ChecklistSimuladorResponse;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.mapper.ChecklistSimuladorMapper;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jboss.logging.Logger;

@ApplicationScoped
@ChecklistSimulador
public class ChecklistSimuladorAdapter implements ObterChecklist {

    private static final Logger LOG = Logger.getLogger(ChecklistSimuladorAdapter.class);
    private static final long DEFAULT_IDENTIFICADOR_NEGOCIAL = 1000012583L;
    private static final int DEFAULT_VERSAO = 1;
    private static final int VERSAO_API = 1;
    private static final String MOCK_RESOURCE_TEMPLATE =
            "mock/parametrizacao/%d-v%d-checklist-parametrizacao-versao-%d.md";
    private static final String DEFAULT_MOCK_RESOURCE = mockResourceName(
            DEFAULT_IDENTIFICADOR_NEGOCIAL,
            DEFAULT_VERSAO
    );

    private final MarkdownJsonMockReader mockReader;
    private final ChecklistSimuladorMapper mapper;

    @Inject
    public ChecklistSimuladorAdapter(
            MarkdownJsonMockReader mockReader,
            ChecklistSimuladorMapper mapper
    ) {
        this.mockReader = mockReader;
        this.mapper = mapper;
    }

    @Override
    public Uni<Checklist> obter(ComandoConsultaChecklist comando) {
        Long identificador = comando.identificadorNegocial();
        Integer versao = comando.versao();
        Span.current().setAttribute("simtr_hub.origem_dados", "mock");
        ObservabilityLog.info(
                LOG,
                "simtr-hub.checklist.simulador.usado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "ChecklistService",
                        "operacao", "consultar-checklist",
                        "identificador_negocial", identificador,
                        "versao", versao,
                        "origem", "mock"
                )
        );

        long identificadorMock = Objects.requireNonNullElse(
                identificador,
                DEFAULT_IDENTIFICADOR_NEGOCIAL
        );
        int versaoMock = Objects.requireNonNullElse(versao, DEFAULT_VERSAO);
        String resourceName = mockResourceName(identificadorMock, versaoMock);
        ChecklistSimuladorResponse resposta = mockReader.readFirstJsonObject(
                resourceName,
                ChecklistSimuladorResponse.class
        );

        if (resposta == null && !DEFAULT_MOCK_RESOURCE.equals(resourceName)) {
            resourceName = DEFAULT_MOCK_RESOURCE;
            resposta = mockReader.readFirstJsonObject(
                    resourceName,
                    ChecklistSimuladorResponse.class
            );
        }
        if (resposta == null) {
            throw new IllegalStateException(
                    "Arquivo de mock nao encontrado no classpath: " + resourceName
            );
        }

        return Uni.createFrom().item(mapper.paraDominio(resposta));
    }

    private static String mockResourceName(long identificador, int versao) {
        return MOCK_RESOURCE_TEMPLATE.formatted(identificador, VERSAO_API, versao);
    }
}
