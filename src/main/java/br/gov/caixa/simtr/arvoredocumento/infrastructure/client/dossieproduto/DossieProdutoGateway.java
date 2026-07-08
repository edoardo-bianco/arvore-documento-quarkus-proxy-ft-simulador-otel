package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.shared.observability.ObservabilityLog;
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

    @WithSpan(value = "mtr.dossie-produto.criar", kind = SpanKind.CLIENT)
    public Uni<DossieProdutoCriadoDto> criarDossieProduto(DossieProdutoCriacaoDto requisicao) {
        Long processo = processo(requisicao);
        Long chaveCorrelacaoCanal = chaveCorrelacaoCanal(requisicao);
        Integer quantidadeClientes = quantidadeClientes(requisicao);

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto");
        setLongAttribute(span, "dossie_produto.processo", processo);
        setLongAttribute(span, "dossie_produto.chave_correlacao_canal", chaveCorrelacaoCanal);
        setIntAttribute(span, "dossie_produto.clientes.quantidade", quantidadeClientes);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.criacao.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "DossieProdutoGateway",
                        "dependencia", "simtr-dossie-produto",
                        "operacao", "criar-dossie-produto-v1",
                        "processo", processo,
                        "chave_correlacao_canal", chaveCorrelacaoCanal,
                        "clientes_quantidade", quantidadeClientes
                )
        );

        return dossieProdutoClient.criarDossieProduto(requisicao)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.dossie-produto.criacao.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "criar-dossie-produto-v1",
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "dossie_produto_id", resposta != null ? resposta.id() : null,
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
                            "mtr.dossie-produto.criacao.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "criar-dossie-produto-v1",
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private static Long processo(DossieProdutoCriacaoDto requisicao) {
        return requisicao != null ? requisicao.processo() : null;
    }

    private static Long chaveCorrelacaoCanal(DossieProdutoCriacaoDto requisicao) {
        return requisicao != null ? requisicao.chaveCorrelacaoCanal() : null;
    }

    private static Integer quantidadeClientes(DossieProdutoCriacaoDto requisicao) {
        if (requisicao == null || requisicao.clientes() == null) {
            return null;
        }
        return requisicao.clientes().size();
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
