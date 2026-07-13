package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DossieProdutoGateway {

    private static final Logger LOG = Logger.getLogger(DossieProdutoGateway.class);

    private final DossieProdutoClient dossieProdutoClient;

    @Inject
    public DossieProdutoGateway(@RestClient DossieProdutoClient dossieProdutoClient) {
        this.dossieProdutoClient = dossieProdutoClient;
    }

    @WithSpan(value = "mtr.dossie-produto.validacao-negocial.registrar", kind = SpanKind.CLIENT)
    public Uni<Void> registrarValidacaoNegocialDossieProduto(
            Long id,
            DossieProdutoValidacaoNegocialDto requisicao
    ) {
        Integer quantidadeVerificacoes = quantidadeVerificacoesValidacao(requisicao);
        Integer quantidadeRespostasFormulario = quantidadeRespostasFormularioValidacao(requisicao);

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "PATCH");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto/" + id + "/validacao-negocial");
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.validacao.verificacoes.quantidade", quantidadeVerificacoes);
        setIntAttribute(span, "dossie_produto.validacao.respostas_formulario.quantidade", quantidadeRespostasFormulario);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.validacao-negocial.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "DossieProdutoGateway",
                        "dependencia", "simtr-dossie-produto",
                        "operacao", "registrar-validacao-negocial-dossie-produto-v1",
                        "dossie_produto_id", id,
                        "validacao_verificacoes_quantidade", quantidadeVerificacoes,
                        "validacao_respostas_formulario_quantidade", quantidadeRespostasFormulario
                )
        );

        return dossieProdutoClient.registrarValidacaoNegocialDossieProduto(id, requisicao)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);

                    ObservabilityLog.info(
                            LOG,
                            "mtr.dossie-produto.validacao-negocial.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "registrar-validacao-negocial-dossie-produto-v1",
                                    "dossie_produto_id", id,
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
                            "mtr.dossie-produto.validacao-negocial.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "registrar-validacao-negocial-dossie-produto-v1",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private static Integer quantidadeVerificacoesValidacao(DossieProdutoValidacaoNegocialDto requisicao) {
        if (requisicao == null || requisicao.verificacoes() == null) {
            return null;
        }
        return requisicao.verificacoes().size();
    }

    private static Integer quantidadeRespostasFormularioValidacao(DossieProdutoValidacaoNegocialDto requisicao) {
        if (requisicao == null || requisicao.respostasFormulario() == null) {
            return null;
        }
        return requisicao.respostasFormulario().size();
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

    private static void setIntAttribute(Span span, String nome, Integer valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

}
