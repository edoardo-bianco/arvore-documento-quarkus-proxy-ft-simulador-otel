package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.IniciarOuAvancarWorkflowDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IniciarOuAvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WorkflowDossieProdutoObservabilidade
        implements IniciarOuAvancarWorkflowDossieProduto {

    private static final Logger LOG = Logger.getLogger(WorkflowDossieProdutoObservabilidade.class);
    private static final String CAMADA = "application";
    private static final String COMPONENTE = "DossieProdutoService";
    private static final String OPERACAO = "iniciar-ou-avancar-workflow-dossie-produto";

    private final IniciarOuAvancarWorkflowDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public WorkflowDossieProdutoObservabilidade(
            AvancarWorkflowDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new IniciarOuAvancarWorkflowDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.dossie-produto.workflow.avancar")
    public Uni<ResultadoWorkflowDossieProduto> executar(IdentificadorDossieProduto identificador) {
        Long id = identificador.valor();
        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado", simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        if (id != null) {
            span.setAttribute("dossie_produto.id", id);
        }

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.workflow.service.iniciado",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "operacao", OPERACAO,
                        "dossie_produto_id", id,
                        "simulador_habilitado", simuladorHabilitado));

        return casoDeUso.executar(identificador)
                .invoke(resposta -> {
                    if (resposta != null && resposta.identificadorDossieProduto() != null) {
                        span.setAttribute(
                                "dossie_produto.workflow.id_resposta",
                                resposta.identificadorDossieProduto());
                    }
                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.dossie-produto.workflow.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", OPERACAO,
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta",
                                    resposta != null ? resposta.identificadorDossieProduto() : null,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.workflow.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", OPERACAO,
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                });
    }
}
