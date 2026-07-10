package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoValidacaoNegocialDto;
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

import java.util.List;
import java.util.Objects;

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

    @WithSpan(value = "mtr.dossie-produto.formulario.atualizar", kind = SpanKind.CLIENT)
    public Uni<DossieProdutoCriadoDto> atualizarFormularioDossieProduto(
            Long id,
            List<DossieProdutoFormularioDto> requisicao
    ) {
        Integer quantidadeVinculos = quantidadeVinculosFormulario(requisicao);
        Integer quantidadeRespostas = quantidadeRespostasFormulario(requisicao);

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "PATCH");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto/" + id + "/formulario");
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.formulario.vinculos.quantidade", quantidadeVinculos);
        setIntAttribute(span, "dossie_produto.formulario.respostas.quantidade", quantidadeRespostas);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.formulario.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "DossieProdutoGateway",
                        "dependencia", "simtr-dossie-produto",
                        "operacao", "atualizar-formulario-dossie-produto-v1",
                        "dossie_produto_id", id,
                        "formulario_vinculos_quantidade", quantidadeVinculos,
                        "formulario_respostas_quantidade", quantidadeRespostas
                )
        );

        return dossieProdutoClient.atualizarFormularioDossieProduto(id, requisicao)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id_resposta", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.dossie-produto.formulario.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "atualizar-formulario-dossie-produto-v1",
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta", resposta != null ? resposta.id() : null,
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
                            "mtr.dossie-produto.formulario.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "atualizar-formulario-dossie-produto-v1",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
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

    @WithSpan(value = "mtr.dossie-produto.workflow.avancar", kind = SpanKind.CLIENT)
    public Uni<DossieProdutoCriadoDto> iniciarOuAvancarWorkflowDossieProduto(Long id) {
        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto/" + id + "/workflow");
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.workflow.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "DossieProdutoGateway",
                        "dependencia", "simtr-dossie-produto",
                        "operacao", "iniciar-ou-avancar-workflow-dossie-produto-v1",
                        "dossie_produto_id", id
                )
        );

        return dossieProdutoClient.iniciarOuAvancarWorkflowDossieProduto(id)
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.workflow.id_resposta", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "mtr.dossie-produto.workflow.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "iniciar-ou-avancar-workflow-dossie-produto-v1",
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta", resposta != null ? resposta.id() : null,
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
                            "mtr.dossie-produto.workflow.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "DossieProdutoGateway",
                                    "dependencia", "simtr-dossie-produto",
                                    "operacao", "iniciar-ou-avancar-workflow-dossie-produto-v1",
                                    "dossie_produto_id", id,
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

    private static Integer quantidadeVinculosFormulario(List<DossieProdutoFormularioDto> requisicao) {
        if (requisicao == null) {
            return null;
        }
        return requisicao.size();
    }

    private static Integer quantidadeRespostasFormulario(List<DossieProdutoFormularioDto> requisicao) {
        if (requisicao == null) {
            return null;
        }
        return requisicao.stream()
                .filter(Objects::nonNull)
                .map(DossieProdutoFormularioDto::vinculoDossie)
                .filter(Objects::nonNull)
                .map(vinculo -> vinculo.respostasFormulario())
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
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
