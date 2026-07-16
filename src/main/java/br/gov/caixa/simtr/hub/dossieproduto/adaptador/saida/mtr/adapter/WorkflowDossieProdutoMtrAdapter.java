package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.WorkflowMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.WorkflowDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.WorkflowDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@WorkflowMtr
public class WorkflowDossieProdutoMtrAdapter implements AvancarWorkflowDossieProduto {

    private static final Logger LOG = Logger.getLogger(WorkflowDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String WORKFLOW = "iniciar-ou-avancar-workflow-dossie-produto-v1";

    private final WorkflowDossieProdutoMtrClient client;

    @Inject
    public WorkflowDossieProdutoMtrAdapter(@RestClient WorkflowDossieProdutoMtrClient client) {
        this.client = client;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.workflow.avancar", kind = SpanKind.CLIENT)
    public Uni<ResultadoWorkflowDossieProduto> avancar(IdentificadorDossieProduto identificador) {
        Long id = identificador.valor();
        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto/" + id + "/workflow");
        if (id != null) {
            span.setAttribute("dossie_produto.id", id);
        }

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.workflow.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE, OPERACAO, WORKFLOW,
                        "dossie_produto_id", id));

        return client.iniciarOuAvancar(id)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.workflow.id_resposta", resposta.id());
                    }
                    ObservabilityLog.info(
                            LOG,
                            "mtr.dossie-produto.workflow.chamada.concluida",
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE, OPERACAO, WORKFLOW,
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta", resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", erro.getClass().getName());
                    ObservabilityLog.error(
                            LOG,
                            "mtr.dossie-produto.workflow.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE, OPERACAO, WORKFLOW,
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                })
                .map(resposta -> new ResultadoWorkflowDossieProduto(resposta.id()))
                .onFailure().transform(WorkflowDossieProdutoMtrAdapter::traduzir);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof WorkflowDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaWorkflowDossieProduto.Tipo tipo = falha instanceof TimeoutException
                ? FalhaWorkflowDossieProduto.Tipo.TIMEOUT
                : FalhaWorkflowDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaWorkflowDossieProduto(
                tipo, null, "simtr-dossie-produto", null, null,
                null, null, null, falha);
    }

    private static FalhaWorkflowDossieProduto traduzirMtr(WorkflowDossieProdutoMtrException falha) {
        WorkflowDossieProdutoMtrException.Erro erro = falha.erro();
        FalhaWorkflowDossieProduto.Tipo tipo = switch (falha) {
            case WorkflowDossieProdutoMtrException.Negocio ignored ->
                    FalhaWorkflowDossieProduto.Tipo.NEGOCIO;
            case WorkflowDossieProdutoMtrException.TecnicaCliente ignored ->
                    FalhaWorkflowDossieProduto.Tipo.TECNICA_CLIENTE;
            case WorkflowDossieProdutoMtrException.Servidor ignored ->
                    FalhaWorkflowDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaWorkflowDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaWorkflowDossieProduto(
                tipo,
                falha.status(),
                erro != null ? erro.recurso() : null,
                erro != null ? erro.idErro() : null,
                erro != null ? erro.codigoErro() : null,
                mensagens(erro),
                erro != null ? erro.detalhe() : null,
                erro != null ? erro.stacktrace() : null,
                falha);
    }

    private static List<String> mensagens(WorkflowDossieProdutoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (WorkflowDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }
}
