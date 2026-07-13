package br.gov.caixa.simtr.hub.dossieproduto.servico;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.DossieProdutoGateway;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.mock.DossieProdutoMockFactory;
import br.gov.caixa.simtr.hub.dossieproduto.mapeamento.DossieProdutoMapper;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
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
                                        name = "simtr-hub.simulador.dossie-produto.habilitado",
                                        defaultValue = "false"
                                ) boolean simuladorDossieProdutoHabilitado) {
        this.dossieProdutoGateway = dossieProdutoGateway;
        this.dossieProdutoMockFactory = dossieProdutoMockFactory;
        this.dossieProdutoMapper = dossieProdutoMapper;
        this.simuladorDossieProdutoHabilitado = simuladorDossieProdutoHabilitado;
    }

    @WithSpan("simtr-hub.service.dossie-produto.validacao-negocial.registrar")
    public Uni<Void> registrarValidacaoNegocialDossieProduto(
            Long id,
            DossieProdutoValidacaoNegocialVo requisicao
    ) {
        Integer quantidadeVerificacoes = quantidadeVerificacoesValidacao(requisicao);
        Integer quantidadeRespostasFormulario = quantidadeRespostasFormularioValidacao(requisicao);

        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado", simuladorDossieProdutoHabilitado);
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.validacao.verificacoes.quantidade", quantidadeVerificacoes);
        setIntAttribute(span, "dossie_produto.validacao.respostas_formulario.quantidade", quantidadeRespostasFormulario);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.validacao-negocial.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "registrar-validacao-negocial-dossie-produto",
                        "dossie_produto_id", id,
                        "validacao_verificacoes_quantidade", quantidadeVerificacoes,
                        "validacao_respostas_formulario_quantidade", quantidadeRespostasFormulario,
                        "simulador_habilitado", simuladorDossieProdutoHabilitado
                )
        );

        return registrarValidacaoNegocialDossieProdutoNoMtrOuSimulador(id, requisicao)
                .invoke(resposta -> ObservabilityLog.info(
                        LOG,
                        "simtr-hub.dossie-produto.validacao-negocial.service.concluido",
                        ObservabilityLog.fields(
                                "camada", "application",
                                "componente", "DossieProdutoService",
                                "operacao", "registrar-validacao-negocial-dossie-produto",
                                "dossie_produto_id", id,
                                "resultado", "sucesso"
                        )
                ))
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.validacao-negocial.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "DossieProdutoService",
                                    "operacao", "registrar-validacao-negocial-dossie-produto",
                                    "dossie_produto_id", id,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<Void> registrarValidacaoNegocialDossieProdutoNoMtrOuSimulador(
            Long id,
            DossieProdutoValidacaoNegocialVo requisicao
    ) {
        DossieProdutoValidacaoNegocialDto requisicaoDto = dossieProdutoMapper.toDto(requisicao);

        if (simuladorDossieProdutoHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "simtr-hub.dossie-produto.validacao-negocial.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "DossieProdutoService",
                            "operacao", "registrar-validacao-negocial-dossie-produto",
                            "dossie_produto_id", id,
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("simtr_hub.origem_dados", "mock");
            dossieProdutoMockFactory.registrarValidacaoNegocialDossieProdutoMock(id, requisicaoDto);
            return Uni.createFrom().voidItem();
        }

        Span.current().setAttribute("simtr_hub.origem_dados", "mtr");
        return dossieProdutoGateway.registrarValidacaoNegocialDossieProduto(id, requisicaoDto);
    }

    private static Integer quantidadeVerificacoesValidacao(DossieProdutoValidacaoNegocialVo requisicao) {
        if (requisicao == null || requisicao.verificacoes() == null) {
            return null;
        }
        return requisicao.verificacoes().size();
    }

    private static Integer quantidadeRespostasFormularioValidacao(DossieProdutoValidacaoNegocialVo requisicao) {
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
