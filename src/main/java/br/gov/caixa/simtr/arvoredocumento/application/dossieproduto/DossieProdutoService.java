package br.gov.caixa.simtr.arvoredocumento.application.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriadoVo;
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
