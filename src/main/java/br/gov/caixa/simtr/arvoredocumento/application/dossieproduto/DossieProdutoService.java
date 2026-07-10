package br.gov.caixa.simtr.arvoredocumento.application.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoDocumentoCriadoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoDocumentoInclusaoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoFormularioVo;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto.DossieProdutoGateway;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto.mock.DossieProdutoMockFactory;
import br.gov.caixa.simtr.arvoredocumento.mapper.dossieproduto.DossieProdutoMapper;
import br.gov.caixa.simtr.arvoredocumento.shared.observability.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class DossieProdutoService {

    private static final Logger LOG = Logger.getLogger(DossieProdutoService.class);

    private final DossieProdutoGateway dossieProdutoGateway;
    private final DossieProdutoMockFactory dossieProdutoMockFactory;
    private final DossieProdutoMapper dossieProdutoMapper;
    private final boolean simuladorDossieProdutoHabilitado;

    @Inject
    public DossieProdutoService(DossieProdutoGateway dossieProdutoGateway,
                                DossieProdutoMockFactory dossieProdutoMockFactory,
                                DossieProdutoMapper dossieProdutoMapper,
                                @ConfigProperty(
                                        name = "arvore-documento.simulador.dossie-produto.habilitado",
                                        defaultValue = "false"
                                ) boolean simuladorDossieProdutoHabilitado) {
        this.dossieProdutoGateway = dossieProdutoGateway;
        this.dossieProdutoMockFactory = dossieProdutoMockFactory;
        this.dossieProdutoMapper = dossieProdutoMapper;
        this.simuladorDossieProdutoHabilitado = simuladorDossieProdutoHabilitado;
    }

    @WithSpan("arvore-documento.service.dossie-produto.criar")
    public Uni<DossieProdutoCriadoVo> criarDossieProduto(DossieProdutoCriacaoVo requisicao) {
        Long processo = processo(requisicao);
        Long chaveCorrelacaoCanal = chaveCorrelacaoCanal(requisicao);
        Integer quantidadeClientes = quantidadeClientes(requisicao);

        Span span = Span.current();
        span.setAttribute("arvore_documento.simulador_dossie_produto_habilitado", simuladorDossieProdutoHabilitado);
        setLongAttribute(span, "dossie_produto.processo", processo);
        setLongAttribute(span, "dossie_produto.chave_correlacao_canal", chaveCorrelacaoCanal);
        setIntAttribute(span, "dossie_produto.clientes.quantidade", quantidadeClientes);

        ObservabilityLog.info(
                LOG,
                "arvore-documento.dossie-produto.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "criar-dossie-produto",
                        "processo", processo,
                        "chave_correlacao_canal", chaveCorrelacaoCanal,
                        "clientes_quantidade", quantidadeClientes,
                        "simulador_habilitado", simuladorDossieProdutoHabilitado
                )
        );

        return criarDossieProdutoNoMtrOuSimulador(requisicao)
                .map(dossieProdutoMapper::toVo)
                .invoke(resposta -> {
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.dossie-produto.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "criar-dossie-produto",
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

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.dossie-produto.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "criar-dossie-produto",
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<DossieProdutoCriadoDto> criarDossieProdutoNoMtrOuSimulador(DossieProdutoCriacaoVo requisicao) {
        DossieProdutoCriacaoDto requisicaoDto = dossieProdutoMapper.toDto(requisicao);

        if (simuladorDossieProdutoHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "arvore-documento.dossie-produto.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "DossieProdutoService",
                            "operacao", "criar-dossie-produto",
                            "processo", processo(requisicao),
                            "chave_correlacao_canal", chaveCorrelacaoCanal(requisicao),
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("arvore_documento.origem_dados", "mock");
            return Uni.createFrom().item(dossieProdutoMockFactory.criarDossieProdutoMock(requisicaoDto));
        }

        Span.current().setAttribute("arvore_documento.origem_dados", "mtr");
        return dossieProdutoGateway.criarDossieProduto(requisicaoDto);
    }

    @WithSpan("arvore-documento.service.dossie-produto.formulario.atualizar")
    public Uni<DossieProdutoCriadoVo> atualizarFormularioDossieProduto(
            Long id,
            List<DossieProdutoFormularioVo> requisicao
    ) {
        Integer quantidadeVinculos = quantidadeVinculosFormulario(requisicao);
        Integer quantidadeRespostas = quantidadeRespostasFormulario(requisicao);

        Span span = Span.current();
        span.setAttribute("arvore_documento.simulador_dossie_produto_habilitado", simuladorDossieProdutoHabilitado);
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.formulario.vinculos.quantidade", quantidadeVinculos);
        setIntAttribute(span, "dossie_produto.formulario.respostas.quantidade", quantidadeRespostas);

        ObservabilityLog.info(
                LOG,
                "arvore-documento.dossie-produto.formulario.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "atualizar-formulario-dossie-produto",
                        "dossie_produto_id", id,
                        "formulario_vinculos_quantidade", quantidadeVinculos,
                        "formulario_respostas_quantidade", quantidadeRespostas,
                        "simulador_habilitado", simuladorDossieProdutoHabilitado
                )
        );

        return atualizarFormularioDossieProdutoNoMtrOuSimulador(id, requisicao)
                .map(dossieProdutoMapper::toVo)
                .invoke(resposta -> {
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id_resposta", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.dossie-produto.formulario.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "atualizar-formulario-dossie-produto",
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta", resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.dossie-produto.formulario.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "atualizar-formulario-dossie-produto",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<DossieProdutoCriadoDto> atualizarFormularioDossieProdutoNoMtrOuSimulador(
            Long id,
            List<DossieProdutoFormularioVo> requisicao
    ) {
        List<DossieProdutoFormularioDto> requisicaoDto = dossieProdutoMapper.toFormularioDto(requisicao);

        if (simuladorDossieProdutoHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "arvore-documento.dossie-produto.formulario.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "DossieProdutoService",
                            "operacao", "atualizar-formulario-dossie-produto",
                            "dossie_produto_id", id,
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("arvore_documento.origem_dados", "mock");
            return Uni.createFrom().item(dossieProdutoMockFactory.atualizarFormularioDossieProdutoMock(id, requisicaoDto));
        }

        Span.current().setAttribute("arvore_documento.origem_dados", "mtr");
        return dossieProdutoGateway.atualizarFormularioDossieProduto(id, requisicaoDto);
    }

    @WithSpan("arvore-documento.service.dossie-produto.documento.incluir")
    public Uni<DossieProdutoDocumentoCriadoVo> incluirDocumentoDossieProduto(
            Long id,
            DossieProdutoDocumentoInclusaoVo requisicao
    ) {
        Integer quantidadeAtributos = quantidadeAtributosDocumento(requisicao);
        Integer quantidadePropriedades = quantidadePropriedadesDocumento(requisicao);
        String tipoDocumento = tipoDocumento(requisicao);

        Span span = Span.current();
        span.setAttribute("arvore_documento.simulador_dossie_produto_habilitado", simuladorDossieProdutoHabilitado);
        setLongAttribute(span, "dossie_produto.id", id);
        setStringAttribute(span, "dossie_produto.documento.tipo", tipoDocumento);
        setIntAttribute(span, "dossie_produto.documento.atributos.quantidade", quantidadeAtributos);
        setIntAttribute(span, "dossie_produto.documento.propriedades.quantidade", quantidadePropriedades);

        ObservabilityLog.info(
                LOG,
                "arvore-documento.dossie-produto.documento.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "incluir-documento-dossie-produto",
                        "dossie_produto_id", id,
                        "tipo_documento", tipoDocumento,
                        "documento_atributos_quantidade", quantidadeAtributos,
                        "documento_propriedades_quantidade", quantidadePropriedades,
                        "simulador_habilitado", simuladorDossieProdutoHabilitado
                )
        );

        return incluirDocumentoDossieProdutoNoMtrOuSimulador(id, requisicao)
                .map(dossieProdutoMapper::toVo)
                .invoke(resposta -> {
                    if (resposta != null && resposta.idDocumento() != null) {
                        span.setAttribute("dossie_produto.documento.id", resposta.idDocumento());
                    }
                    if (resposta != null && resposta.idInstanciaDocumento() != null) {
                        span.setAttribute("dossie_produto.documento.instancia.id", resposta.idInstanciaDocumento());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.dossie-produto.documento.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "incluir-documento-dossie-produto",
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

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.dossie-produto.documento.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "incluir-documento-dossie-produto",
                                    "dossie_produto_id", id,
                                    "tipo_documento", tipoDocumento,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<DossieProdutoDocumentoCriadoDto> incluirDocumentoDossieProdutoNoMtrOuSimulador(
            Long id,
            DossieProdutoDocumentoInclusaoVo requisicao
    ) {
        DossieProdutoDocumentoInclusaoDto requisicaoDto = dossieProdutoMapper.toDto(requisicao);

        if (simuladorDossieProdutoHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "arvore-documento.dossie-produto.documento.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "DossieProdutoService",
                            "operacao", "incluir-documento-dossie-produto",
                            "dossie_produto_id", id,
                            "tipo_documento", tipoDocumento(requisicao),
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("arvore_documento.origem_dados", "mock");
            return Uni.createFrom().item(dossieProdutoMockFactory.incluirDocumentoDossieProdutoMock(id, requisicaoDto));
        }

        Span.current().setAttribute("arvore_documento.origem_dados", "mtr");
        return dossieProdutoGateway.incluirDocumentoDossieProduto(id, requisicaoDto);
    }

    @WithSpan("arvore-documento.service.dossie-produto.workflow.avancar")
    public Uni<DossieProdutoCriadoVo> iniciarOuAvancarWorkflowDossieProduto(Long id) {
        Span span = Span.current();
        span.setAttribute("arvore_documento.simulador_dossie_produto_habilitado", simuladorDossieProdutoHabilitado);
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(
                LOG,
                "arvore-documento.dossie-produto.workflow.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                        "dossie_produto_id", id,
                        "simulador_habilitado", simuladorDossieProdutoHabilitado
                )
        );

        return iniciarOuAvancarWorkflowDossieProdutoNoMtrOuSimulador(id)
                .map(dossieProdutoMapper::toVo)
                .invoke(resposta -> {
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.workflow.id_resposta", resposta.id());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.dossie-produto.workflow.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                                    "dossie_produto_id", id,
                                    "dossie_produto_id_resposta", resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.dossie-produto.workflow.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<DossieProdutoCriadoDto> iniciarOuAvancarWorkflowDossieProdutoNoMtrOuSimulador(Long id) {
        if (simuladorDossieProdutoHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "arvore-documento.dossie-produto.workflow.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "DossieProdutoService",
                            "operacao", "iniciar-ou-avancar-workflow-dossie-produto",
                            "dossie_produto_id", id,
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("arvore_documento.origem_dados", "mock");
            return Uni.createFrom().item(dossieProdutoMockFactory.iniciarOuAvancarWorkflowDossieProdutoMock(id));
        }

        Span.current().setAttribute("arvore_documento.origem_dados", "mtr");
        return dossieProdutoGateway.iniciarOuAvancarWorkflowDossieProduto(id);
    }

    private static Long processo(DossieProdutoCriacaoVo requisicao) {
        return requisicao != null ? requisicao.processo() : null;
    }

    private static Long chaveCorrelacaoCanal(DossieProdutoCriacaoVo requisicao) {
        return requisicao != null ? requisicao.chaveCorrelacaoCanal() : null;
    }

    private static Integer quantidadeClientes(DossieProdutoCriacaoVo requisicao) {
        if (requisicao == null || requisicao.clientes() == null) {
            return null;
        }
        return requisicao.clientes().size();
    }

    private static Integer quantidadeVinculosFormulario(List<DossieProdutoFormularioVo> requisicao) {
        if (requisicao == null) {
            return null;
        }
        return requisicao.size();
    }

    private static Integer quantidadeRespostasFormulario(List<DossieProdutoFormularioVo> requisicao) {
        if (requisicao == null) {
            return null;
        }
        return requisicao.stream()
                .filter(Objects::nonNull)
                .map(DossieProdutoFormularioVo::vinculoDossie)
                .filter(Objects::nonNull)
                .map(vinculo -> vinculo.respostasFormulario())
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private static String tipoDocumento(DossieProdutoDocumentoInclusaoVo requisicao) {
        return requisicao != null ? requisicao.tipoDocumento() : null;
    }

    private static Integer quantidadeAtributosDocumento(DossieProdutoDocumentoInclusaoVo requisicao) {
        if (requisicao == null || requisicao.atributos() == null) {
            return null;
        }
        return requisicao.atributos().size();
    }

    private static Integer quantidadePropriedadesDocumento(DossieProdutoDocumentoInclusaoVo requisicao) {
        if (requisicao == null || requisicao.propriedades() == null) {
            return null;
        }
        return requisicao.propriedades().size();
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
