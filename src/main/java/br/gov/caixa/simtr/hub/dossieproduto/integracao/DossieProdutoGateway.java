package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
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

    @WithSpan(value = "mtr.dossie-produto.documento.incluir", kind = SpanKind.CLIENT)
    public Uni<DossieProdutoDocumentoCriadoDto> incluirDocumentoDossieProduto(
            Long id,
            DossieProdutoDocumentoInclusaoDto requisicao
    ) {
        Integer quantidadeAtributos = quantidadeAtributosDocumento(requisicao);
        Integer quantidadePropriedades = quantidadePropriedadesDocumento(requisicao);
        String tipoDocumento = tipoDocumento(requisicao);

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v2");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("url.path", "/simtr/dossie-produto/v2/dossie-produto/" + id + "/documento");
        setLongAttribute(span, "dossie_produto.id", id);
        setStringAttribute(span, "dossie_produto.documento.tipo", tipoDocumento);
        setIntAttribute(span, "dossie_produto.documento.atributos.quantidade", quantidadeAtributos);
        setIntAttribute(span, "dossie_produto.documento.propriedades.quantidade", quantidadePropriedades);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.documento.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "DossieProdutoGateway",
                        "dependencia", "simtr-dossie-produto",
                        "operacao", "incluir-documento-dossie-produto-v2",
                        "dossie_produto_id", id,
                        "tipo_documento", tipoDocumento,
                        "documento_atributos_quantidade", quantidadeAtributos,
                        "documento_propriedades_quantidade", quantidadePropriedades
                )
        );

        return dossieProdutoClient.incluirDocumentoDossieProduto(id, requisicao)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null && resposta.idDocumento() != null) {
                        span.setAttribute("dossie_produto.documento.id", resposta.idDocumento());
                    }
                    if (resposta != null && resposta.idInstanciaDocumento() != null) {
                        span.setAttribute("dossie_produto.documento.instancia.id", resposta.idInstanciaDocumento());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.dossie-produto.documento.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "incluir-documento-dossie-produto-v2",
                                    "dossie_produto_id", id,
                                    "id_documento", resposta != null ? resposta.idDocumento() : null,
                                    "id_instancia_documento", resposta != null ? resposta.idInstanciaDocumento() : null,
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
                            "mtr.dossie-produto.documento.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "incluir-documento-dossie-produto-v2",
                                    "dossie_produto_id", id,
                                    "tipo_documento", tipoDocumento,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
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

    private static String tipoDocumento(DossieProdutoDocumentoInclusaoDto requisicao) {
        return requisicao != null ? requisicao.tipoDocumento() : null;
    }

    private static Integer quantidadeAtributosDocumento(DossieProdutoDocumentoInclusaoDto requisicao) {
        if (requisicao == null || requisicao.atributos() == null) {
            return null;
        }
        return requisicao.atributos().size();
    }

    private static Integer quantidadePropriedadesDocumento(DossieProdutoDocumentoInclusaoDto requisicao) {
        if (requisicao == null || requisicao.propriedades() == null) {
            return null;
        }
        return requisicao.propriedades().size();
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

    private static void setStringAttribute(Span span, String nome, String valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
