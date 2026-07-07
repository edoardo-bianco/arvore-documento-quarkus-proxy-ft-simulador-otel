package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.simtr.arvoredocumento.shared.observability.ObservabilityLog;
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
public class ParametrizacaoProcessoGateway {

    private static final Logger LOG = Logger.getLogger(ParametrizacaoProcessoGateway.class);

    private final ParametrizacaoProcessoClient processoClient;

    @Inject
    public ParametrizacaoProcessoGateway(@RestClient ParametrizacaoProcessoClient processoClient) {
        this.processoClient = processoClient;
    }

    @WithSpan(value = "mtr.parametrizacao.processo.consultar", kind = SpanKind.CLIENT)
    public Uni<ProcessoDto> consultarPorIdentificadorNegocial(
            @SpanAttribute("mtr.parametrizacao.processo.identificador_negocial") Long identificador) {

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-parametrizacao");
        span.setAttribute("mtr.api", "patriarca-processo-v2");
        span.setAttribute("http.request.method", "GET");
        span.setAttribute("url.path", "/simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}");

        ObservabilityLog.info(
                LOG,
                "mtr.parametrizacao.processo.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "ParametrizacaoProcessoGateway",
                        "dependencia", "simtr-parametrizacao",
                        "operacao", "consultar-processo-parametrizacao-v2",
                        "identificador_negocial", identificador
                )
        );

        return processoClient.consultarPorIdentificadorNegocial(identificador)
                .invoke(processo -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (processo != null) {
                        if (processo.identificadorNegocial() != null) {
                            span.setAttribute("processo.identificador_negocial", processo.identificadorNegocial());
                        }
                        if (processo.nome() != null) {
                            span.setAttribute("processo.nome", processo.nome());
                        }
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.parametrizacao.processo.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "ParametrizacaoProcessoGateway",
                                    "dependencia", "simtr-parametrizacao",
                                    "operacao", "consultar-processo-parametrizacao-v2",
                                    "identificador_negocial", identificador,
                                    "processo_nome", processo != null ? processo.nome() : null,
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
                            "mtr.parametrizacao.processo.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "ParametrizacaoProcessoGateway",
                                    "dependencia", "simtr-parametrizacao",
                                    "operacao", "consultar-processo-parametrizacao-v2",
                                    "identificador_negocial", identificador,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }
}
