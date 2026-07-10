package br.gov.caixa.simtr.arvoredocumento.application.gestaodocumento;

import br.gov.caixa.simtr.arvoredocumento.api.dto.gestaodocumento.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.arvoredocumento.domain.gestaodocumento.GestaoDocumentoCredencialContainerVo;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.gestaodocumento.GestaoDocumentoGateway;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.gestaodocumento.mock.GestaoDocumentoMockFactory;
import br.gov.caixa.simtr.arvoredocumento.mapper.gestaodocumento.GestaoDocumentoMapper;
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
public class GestaoDocumentoService {

    private static final Logger LOG = Logger.getLogger(GestaoDocumentoService.class);

    private final GestaoDocumentoGateway gestaoDocumentoGateway;
    private final GestaoDocumentoMockFactory gestaoDocumentoMockFactory;
    private final GestaoDocumentoMapper gestaoDocumentoMapper;
    private final boolean simuladorGestaoDocumentoHabilitado;

    @Inject
    public GestaoDocumentoService(GestaoDocumentoGateway gestaoDocumentoGateway,
                                  GestaoDocumentoMockFactory gestaoDocumentoMockFactory,
                                  GestaoDocumentoMapper gestaoDocumentoMapper,
                                  @ConfigProperty(
                                          name = "arvore-documento.simulador.gestao-documento.habilitado",
                                          defaultValue = "false"
                                  ) boolean simuladorGestaoDocumentoHabilitado) {
        this.gestaoDocumentoGateway = gestaoDocumentoGateway;
        this.gestaoDocumentoMockFactory = gestaoDocumentoMockFactory;
        this.gestaoDocumentoMapper = gestaoDocumentoMapper;
        this.simuladorGestaoDocumentoHabilitado = simuladorGestaoDocumentoHabilitado;
    }

    @WithSpan("arvore-documento.service.gestao-documento.credencial-container.gerar")
    public Uni<GestaoDocumentoCredencialContainerVo> gerarCredencialContainer() {
        Span span = Span.current();
        span.setAttribute("arvore_documento.simulador_gestao_documento_habilitado", simuladorGestaoDocumentoHabilitado);

        ObservabilityLog.info(
                LOG,
                "arvore-documento.gestao-documento.credencial-container.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "GestaoDocumentoService",
                        "operacao", "gerar-credencial-container",
                        "simulador_habilitado", simuladorGestaoDocumentoHabilitado
                )
        );

        return gerarCredencialContainerNoMtrOuSimulador()
                .map(gestaoDocumentoMapper::toVo)
                .invoke(resposta -> {
                    setStringAttribute(span, "gestao_documento.container.nome", nomeContainer(resposta));

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.gestao-documento.credencial-container.service.concluido",
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "GestaoDocumentoService",
                                    "operacao", "gerar-credencial-container",
                                    "nome_container", nomeContainer(resposta),
                                    "resultado", "sucesso"
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.gestao-documento.credencial-container.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "application",
                                    "componente", "GestaoDocumentoService",
                                    "operacao", "gerar-credencial-container",
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private Uni<GestaoDocumentoCredencialContainerDto> gerarCredencialContainerNoMtrOuSimulador() {
        if (simuladorGestaoDocumentoHabilitado) {
            ObservabilityLog.info(
                    LOG,
                    "arvore-documento.gestao-documento.credencial-container.simulador.usado",
                    ObservabilityLog.fields(
                            "camada", "application",
                            "componente", "GestaoDocumentoService",
                            "operacao", "gerar-credencial-container",
                            "origem", "mock"
                    )
            );

            Span.current().setAttribute("arvore_documento.origem_dados", "mock");
            return Uni.createFrom().item(gestaoDocumentoMockFactory.gerarCredencialContainerMock());
        }

        Span.current().setAttribute("arvore_documento.origem_dados", "mtr");
        return gestaoDocumentoGateway.gerarCredencialContainer();
    }

    private static String nomeContainer(GestaoDocumentoCredencialContainerVo resposta) {
        return resposta != null ? resposta.nomeContainer() : null;
    }

    private static void setStringAttribute(Span span, String nome, String valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
