package br.gov.caixa.simtr.hub.parametrizacao.servico;

import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.parametrizacao.dominio.processo.ProcessoVo;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoProcessoGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.mock.ProcessoMockFactory;
import br.gov.caixa.simtr.hub.parametrizacao.mapeamento.ProcessoMapper;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
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
                                   name = "simtr-hub.simulador.parametrizacao-processo.habilitado",
                                   defaultValue = "false"
                           ) boolean simuladorParametrizacaoProcessoHabilitado) {
        this.processoGateway = processoGateway;
        this.processoMockFactory = processoMockFactory;
        this.processoMapper = processoMapper;
        this.simuladorParametrizacaoProcessoHabilitado = simuladorParametrizacaoProcessoHabilitado;
    }

    @WithSpan("simtr-hub.service.processo.consultar")
    public Uni<ProcessoVo> consultarPorIdentificadorNegocial(
            @SpanAttribute("processo.identificador_negocial") Long identificador) {

        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_parametrizacao_processo_habilitado", simuladorParametrizacaoProcessoHabilitado);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.processo.service.iniciado",
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
                            "simtr-hub.processo.service.concluido",
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
                            "simtr-hub.processo.service.falhou",
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
                    "simtr-hub.processo.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "ProcessoService",
                            "operacao", "consultar-processo",
                            "identificador_negocial", identificador,
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("simtr_hub.origem_dados", "mock");
            return Uni.createFrom().item(processoMockFactory.criarProcessoMock(identificador));
        }

        Span.current().setAttribute("simtr_hub.origem_dados", "mtr");
        return processoGateway.consultarPorIdentificadorNegocial(identificador);
    }
}
