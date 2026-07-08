package br.gov.caixa.simtr.arvoredocumento.application.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.checklist.ChecklistVo;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.ParametrizacaoChecklistGateway;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.mock.ChecklistMockFactory;
import br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao.ChecklistMapper;
import br.gov.caixa.simtr.arvoredocumento.shared.observability.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChecklistService {

    private static final Logger LOG = Logger.getLogger(ChecklistService.class);

    private final ParametrizacaoChecklistGateway checklistGateway;
    private final ChecklistMockFactory checklistMockFactory;
    private final ChecklistMapper checklistMapper;
    private final boolean simuladorParametrizacaoChecklistHabilitado;

    @Inject
    public ChecklistService(ParametrizacaoChecklistGateway checklistGateway,
                            ChecklistMockFactory checklistMockFactory,
                            ChecklistMapper checklistMapper,
                            @ConfigProperty(
                                    name = "arvore-documento.simulador.parametrizacao-checklist.habilitado",
                                    defaultValue = "false"
                            ) boolean simuladorParametrizacaoChecklistHabilitado) {
        this.checklistGateway = checklistGateway;
        this.checklistMockFactory = checklistMockFactory;
        this.checklistMapper = checklistMapper;
        this.simuladorParametrizacaoChecklistHabilitado = simuladorParametrizacaoChecklistHabilitado;
    }

    @WithSpan("arvore-documento.service.checklist.consultar")
    public Uni<ChecklistVo> consultarPorIdentificadorNegocialEVersao(
            @SpanAttribute("checklist.identificador_negocial") Long identificador,
            @SpanAttribute("checklist.versao") Integer versao) {

        Span span = Span.current();
        span.setAttribute("arvore_documento.simulador_parametrizacao_checklist_habilitado", simuladorParametrizacaoChecklistHabilitado);

        ObservabilityLog.info(
                LOG,
                "arvore-documento.checklist.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "ChecklistService",
                        "operacao", "consultar-checklist",
                        "identificador_negocial", identificador,
                        "versao", versao,
                        "simulador_habilitado", simuladorParametrizacaoChecklistHabilitado
                )
        );

        return consultarChecklistNoMtrOuSimulador(identificador, versao)
                .map(checklistMapper::toVo)
                .invoke(checklistVo -> {
                    span.setAttribute("checklist.encontrado", checklistVo != null);
                    if (checklistVo != null && checklistVo.nome() != null) {
                        span.setAttribute("checklist.nome", checklistVo.nome());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.checklist.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "ChecklistService",
                                    "operacao", "consultar-checklist",
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "resultado", "sucesso",
                                    "checklist_nome", checklistVo != null ? checklistVo.nome() : null
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.checklist.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "ChecklistService",
                                    "operacao", "consultar-checklist",
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<ChecklistDto> consultarChecklistNoMtrOuSimulador(Long identificador, Integer versao) {
        if (simuladorParametrizacaoChecklistHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "arvore-documento.checklist.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "ChecklistService",
                            "operacao", "consultar-checklist",
                            "identificador_negocial", identificador,
                            "versao", versao,
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("arvore_documento.origem_dados", "mock");
            return Uni.createFrom().item(checklistMockFactory.criarChecklistMock(identificador, versao));
        }

        Span.current().setAttribute("arvore_documento.origem_dados", "mtr");
        return checklistGateway.consultarPorIdentificadorNegocialEVersao(identificador, versao);
    }
}
