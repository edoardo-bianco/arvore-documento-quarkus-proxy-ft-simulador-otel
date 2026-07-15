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
                        "camada", "application",
                        "componente", "GestaoDocumentoService",
                        "operacao", "gerar-credencial-container",
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
                                    "camada", "application",
                                    "componente", "GestaoDocumentoService",
                                    "operacao", "gerar-credencial-container",
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
                                    "camada", "application",
                                    "componente", "GestaoDocumentoService",
                                    "operacao", "gerar-credencial-container",
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
