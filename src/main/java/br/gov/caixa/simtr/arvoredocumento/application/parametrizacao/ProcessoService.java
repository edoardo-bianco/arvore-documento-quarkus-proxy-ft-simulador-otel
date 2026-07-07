package br.gov.caixa.simtr.arvoredocumento.application.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo.ProcessoVo;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.ParametrizacaoProcessoGateway;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.mock.ProcessoMockFactory;
import br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao.ProcessoMapper;
import br.gov.caixa.simtr.arvoredocumento.shared.observability.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProcessoService {

    private static final Logger LOG = Logger.getLogger(ProcessoService.class);

    private final ParametrizacaoProcessoGateway processoGateway;
    private final ProcessoMockFactory processoMockFactory;
    private final ProcessoMapper processoMapper;
    private final boolean simuladorParametrizacaoProcessoHabilitado;

    @Inject
    public ProcessoService(ParametrizacaoProcessoGateway processoGateway,
                           ProcessoMockFactory processoMockFactory,
                           ProcessoMapper processoMapper,
                           @ConfigProperty(
                                   name = "arvore-documento.simulador.parametrizacao-processo.habilitado",
                                   defaultValue = "false"
                           ) boolean simuladorParametrizacaoProcessoHabilitado) {
        this.processoGateway = processoGateway;
        this.processoMockFactory = processoMockFactory;
        this.processoMapper = processoMapper;
        this.simuladorParametrizacaoProcessoHabilitado = simuladorParametrizacaoProcessoHabilitado;
    }

    @WithSpan("arvore-documento.service.processo.consultar")
    public Uni<ProcessoVo> consultarPorIdentificadorNegocial(
            @SpanAttribute("processo.identificador_negocial") Long identificador) {

        Span span = Span.current();
        span.setAttribute("arvore_documento.simulador_parametrizacao_processo_habilitado", simuladorParametrizacaoProcessoHabilitado);

        ObservabilityLog.info(
                LOG,
                "arvore-documento.processo.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "ProcessoService",
                        "operacao", "consultar-processo",
                        "identificador_negocial", identificador,
                        "simulador_habilitado", simuladorParametrizacaoProcessoHabilitado
                )
        );

        return consultarProcessoNoMtrOuSimulador(identificador)
                .map(processoMapper::toVo)
                .invoke(processoVo -> {
                    span.setAttribute("processo.encontrado", processoVo != null);
                    if (processoVo != null && processoVo.nome() != null) {
                        span.setAttribute("processo.nome", processoVo.nome());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.processo.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "ProcessoService",
                                    "operacao", "consultar-processo",
                                    "identificador_negocial", identificador,
                                    "resultado", "sucesso",
                                    "processo_nome", processoVo != null ? processoVo.nome() : null
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.processo.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "ProcessoService",
                                    "operacao", "consultar-processo",
                                    "identificador_negocial", identificador,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<ProcessoDto> consultarProcessoNoMtrOuSimulador(Long identificador) {
        if (simuladorParametrizacaoProcessoHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "arvore-documento.processo.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "ProcessoService",
                            "operacao", "consultar-processo",
                            "identificador_negocial", identificador,
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("arvore_documento.origem_dados", "mock");
            return Uni.createFrom().item(processoMockFactory.criarProcessoMock(identificador));
        }

        Span.current().setAttribute("arvore_documento.origem_dados", "mtr");
        return processoGateway.consultarPorIdentificadorNegocial(identificador);
    }
}
