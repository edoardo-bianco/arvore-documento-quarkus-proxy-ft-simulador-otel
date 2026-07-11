package br.gov.caixa.simtr.hub.parametrizacao.integracao;

import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ParametrizacaoChecklistGateway {

    private static final Logger LOG = Logger.getLogger(ParametrizacaoChecklistGateway.class);

    private final ParametrizacaoChecklistClient checklistClient;

    @Inject
    public ParametrizacaoChecklistGateway(@RestClient ParametrizacaoChecklistClient checklistClient) {
        this.checklistClient = checklistClient;
    }

    @WithSpan(value = "mtr.parametrizacao.checklist.consultar", kind = SpanKind.CLIENT)
    public Uni<ChecklistDto> consultarPorIdentificadorNegocialEVersao(
            @SpanAttribute("mtr.parametrizacao.checklist.identificador_negocial") Long identificador,
            @SpanAttribute("mtr.parametrizacao.checklist.versao") Integer versao) {

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-parametrizacao");
        span.setAttribute("mtr.api", "cadastro-checklist-v1");
        span.setAttribute("http.request.method", "GET");
        span.setAttribute("url.path", "/simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}");

        ObservabilityLog.info(
                LOG,
                "mtr.parametrizacao.checklist.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "ParametrizacaoChecklistGateway",
                        "dependencia", "simtr-parametrizacao",
                        "operacao", "consultar-checklist-parametrizacao-v1",
                        "identificador_negocial", identificador,
                        "versao", versao
                )
        );

        return checklistClient.consultarPorIdentificadorNegocialEVersao(identificador, versao)
                .invoke(checklist -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (checklist != null) {
                        if (checklist.identificadorNegocial() != null) {
                            span.setAttribute("checklist.identificador_negocial", checklist.identificadorNegocial());
                        }
                        if (checklist.nome() != null) {
                            span.setAttribute("checklist.nome", checklist.nome());
                        }
                        if (checklist.apontamentos() != null) {
                            span.setAttribute("checklist.apontamentos.quantidade", checklist.apontamentos().size());
                        }
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.parametrizacao.checklist.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "ParametrizacaoChecklistGateway",
                                    "dependencia", "simtr-parametrizacao",
                                    "operacao", "consultar-checklist-parametrizacao-v1",
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "checklist_nome", checklist != null ? checklist.nome() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", erro.getClass().getName());

                    ObservabilityLog.error(
                            LOG,
                            "mtr.parametrizacao.checklist.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "ParametrizacaoChecklistGateway",
                                    "dependencia", "simtr-parametrizacao",
                                    "operacao", "consultar-checklist-parametrizacao-v1",
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }
}
