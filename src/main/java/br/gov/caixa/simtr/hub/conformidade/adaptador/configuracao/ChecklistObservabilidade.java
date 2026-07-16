package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso.ConsultarChecklistCasoDeUso;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChecklistObservabilidade implements ConsultarChecklist {

    private static final Logger LOG = Logger.getLogger(ChecklistObservabilidade.class);
    private static final String CAMADA = "application";
    private static final String COMPONENTE = "ChecklistService";
    private static final String OPERACAO = "consultar-checklist";

    private final ConsultarChecklist casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public ChecklistObservabilidade(
            ObterChecklist portaSaida,
            @ConfigProperty(
                    name = "simtr-hub.simulador.parametrizacao-checklist.habilitado",
                    defaultValue = "false"
            ) boolean simuladorHabilitado
    ) {
        this.casoDeUso = new ConsultarChecklistCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.checklist.consultar")
    public Uni<Checklist> executar(ComandoConsultaChecklist comando) {
        Long identificador = comando != null ? comando.identificadorNegocial() : null;
        Integer versao = comando != null ? comando.versao() : null;
        Span span = Span.current();
        span.setAttribute(
                "simtr_hub.simulador_parametrizacao_checklist_habilitado",
                simuladorHabilitado
        );
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        if (identificador != null) {
            span.setAttribute("checklist.identificador_negocial", identificador);
        }
        if (versao != null) {
            span.setAttribute("checklist.versao", versao.longValue());
        }

        ObservabilityLog.info(
                LOG,
                "simtr-hub.checklist.service.iniciado",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "operacao", OPERACAO,
                        "identificador_negocial", identificador,
                        "versao", versao,
                        "simulador_habilitado", simuladorHabilitado
                )
        );

        return casoDeUso.executar(comando)
                .invoke(checklist -> {
                    span.setAttribute("checklist.encontrado", checklist != null);
                    if (checklist != null && checklist.nome() != null) {
                        span.setAttribute("checklist.nome", checklist.nome());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.checklist.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", OPERACAO,
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "resultado", "sucesso",
                                    "checklist_nome", checklist != null
                                            ? checklist.nome()
                                            : null
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.checklist.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", OPERACAO,
                                    "identificador_negocial", identificador,
                                    "versao", versao,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }
}
