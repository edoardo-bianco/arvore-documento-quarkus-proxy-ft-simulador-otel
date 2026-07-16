package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.casodeuso.ConsultarProcessoParametrizadoCasoDeUso;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.entrada.ConsultarProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProcessoParametrizadoObservabilidade implements ConsultarProcessoParametrizado {

    private static final Logger LOG = Logger.getLogger(
            ProcessoParametrizadoObservabilidade.class);
    private static final String CAMADA = "application";
    private static final String COMPONENTE = "ProcessoService";
    private static final String OPERACAO = "consultar-processo";
    private static final String CAMADA_KEY = "camada";
    private static final String COMPONENTE_KEY = "componente";
    private static final String OPERACAO_KEY = "operacao";
    private static final String IDENTIFICADOR_NEGOCIAL_KEY = "identificador_negocial";

    private final ConsultarProcessoParametrizado casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public ProcessoParametrizadoObservabilidade(
            ObterProcessoParametrizado portaSaida,
            @ConfigProperty(
                    name = "simtr-hub.simulador.parametrizacao-processo.habilitado",
                    defaultValue = "false"
            ) boolean simuladorHabilitado
    ) {
        this.casoDeUso = new ConsultarProcessoParametrizadoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.processo.consultar")
    public Uni<ProcessoParametrizado> executar(IdentificadorNegocialProcesso identificador) {
        Long valor = identificador != null ? identificador.valor() : null;
        Span span = Span.current();
        span.setAttribute(
                "simtr_hub.simulador_parametrizacao_processo_habilitado",
                simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        if (valor != null) {
            span.setAttribute("processo.identificador_negocial", valor);
        }

        ObservabilityLog.info(
                LOG,
                "simtr-hub.processo.service.iniciado",
                ObservabilityLog.fields(
                        CAMADA_KEY, CAMADA,
                        COMPONENTE_KEY, COMPONENTE,
                        OPERACAO_KEY, OPERACAO,
                        IDENTIFICADOR_NEGOCIAL_KEY, valor,
                        "simulador_habilitado", simuladorHabilitado
                )
        );

        return casoDeUso.executar(identificador)
                .invoke(processo -> {
                    span.setAttribute("processo.encontrado", processo != null);
                    if (processo != null && processo.nome() != null) {
                        span.setAttribute("processo.nome", processo.nome());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.processo.service.concluido",
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    OPERACAO_KEY, OPERACAO,
                                    IDENTIFICADOR_NEGOCIAL_KEY, valor,
                                    "resultado", "sucesso",
                                    "processo_nome", processo != null ? processo.nome() : null
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.processo.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    OPERACAO_KEY, OPERACAO,
                                    IDENTIFICADOR_NEGOCIAL_KEY, valor,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }
}
