package br.gov.caixa.simtr.hub.gestaodocumento.integracao;

import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
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
public class GestaoDocumentoGateway {

    private static final Logger LOG = Logger.getLogger(GestaoDocumentoGateway.class);

    private final GestaoDocumentoClient gestaoDocumentoClient;

    @Inject
    public GestaoDocumentoGateway(@RestClient GestaoDocumentoClient gestaoDocumentoClient) {
        this.gestaoDocumentoClient = gestaoDocumentoClient;
    }

    @WithSpan(value = "mtr.gestao-documento.credencial-container.gerar", kind = SpanKind.CLIENT)
    public Uni<GestaoDocumentoCredencialContainerDto> gerarCredencialContainer() {
        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-gestao-documento");
        span.setAttribute("mtr.api", "gestao-documento-v1");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("url.path", "/simtr/gestao-documento/v1/storage/container/credencial");

        ObservabilityLog.info(
                LOG,
                "mtr.gestao-documento.credencial-container.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "GestaoDocumentoGateway",
                        "dependencia", "simtr-gestao-documento",
                        "operacao", "gerar-credencial-container-v1"
                )
        );

        return gestaoDocumentoClient.gerarCredencialContainer()
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    setStringAttribute(span, "gestao_documento.container.nome", nomeContainer(resposta));

                    ObservabilityLog.info(
                            LOG,
                            "mtr.gestao-documento.credencial-container.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "GestaoDocumentoGateway",
                                    "dependencia", "simtr-gestao-documento",
                                    "operacao", "gerar-credencial-container-v1",
                                    "nome_container", nomeContainer(resposta),
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
                            "mtr.gestao-documento.credencial-container.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "infrastructure",
                                    "componente", "GestaoDocumentoGateway",
                                    "dependencia", "simtr-gestao-documento",
                                    "operacao", "gerar-credencial-container-v1",
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }

    private static String nomeContainer(GestaoDocumentoCredencialContainerDto resposta) {
        return resposta != null ? resposta.nomeContainer() : null;
    }

    private static void setStringAttribute(Span span, String nome, String valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
