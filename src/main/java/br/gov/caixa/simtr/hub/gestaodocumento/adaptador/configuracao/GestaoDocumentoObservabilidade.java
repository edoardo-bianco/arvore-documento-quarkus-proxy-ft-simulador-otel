package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.casodeuso.ObterCredencialContainerCasoDeUso;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.entrada.ObterCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GestaoDocumentoObservabilidade implements ObterCredencialContainer {

    private static final Logger LOG = Logger.getLogger(GestaoDocumentoObservabilidade.class);
    private static final String CAMADA = "camada";
    private static final String APPLICATION = "application";
    private static final String COMPONENTE = "componente";
    private static final String SERVICE = "GestaoDocumentoService";
    private static final String OPERACAO = "operacao";
    private static final String GERAR_CREDENCIAL_CONTAINER = "gerar-credencial-container";

    private final ObterCredencialContainer casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public GestaoDocumentoObservabilidade(
            SolicitarCredencialContainer portaSaida,
            @ConfigProperty(
                    name = "simtr-hub.simulador.gestao-documento.habilitado",
                    defaultValue = "false"
            ) boolean simuladorHabilitado
    ) {
        this.casoDeUso = new ObterCredencialContainerCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.gestao-documento.credencial-container.gerar")
    public Uni<CredencialContainer> executar() {
        Span span = Span.current();
        span.setAttribute(
                "simtr_hub.simulador_gestao_documento_habilitado",
                simuladorHabilitado
        );
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");

        ObservabilityLog.info(
                LOG,
                "simtr-hub.gestao-documento.credencial-container.service.iniciado",
                ObservabilityLog.fields(
                        CAMADA, APPLICATION,
                        COMPONENTE, SERVICE,
                        OPERACAO, GERAR_CREDENCIAL_CONTAINER,
                        "simulador_habilitado", simuladorHabilitado
                )
        );

        return casoDeUso.executar()
                .invoke(credencial -> {
                    setStringAttribute(
                            span,
                            "gestao_documento.container.nome",
                            nomeContainer(credencial)
                    );

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.gestao-documento.credencial-container.service.concluido",
                            ObservabilityLog.fields(
                                    CAMADA, APPLICATION,
                                    COMPONENTE, SERVICE,
                                    OPERACAO, GERAR_CREDENCIAL_CONTAINER,
                                    "nome_container", nomeContainer(credencial),
                                    "resultado", "sucesso"
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.gestao-documento.credencial-container.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA, APPLICATION,
                                    COMPONENTE, SERVICE,
                                    OPERACAO, GERAR_CREDENCIAL_CONTAINER,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private static String nomeContainer(CredencialContainer credencial) {
        return credencial != null ? credencial.nomeContainer() : null;
    }

    private static void setStringAttribute(Span span, String nome, String valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
