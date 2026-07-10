package br.gov.caixa.simtr.arvoredocumento.api.gestaodocumento;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.gestaodocumento.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.arvoredocumento.application.gestaodocumento.GestaoDocumentoService;
import br.gov.caixa.simtr.arvoredocumento.mapper.gestaodocumento.GestaoDocumentoMapper;
import br.gov.caixa.simtr.arvoredocumento.shared.observability.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/arvore-documento/v1/storage/container")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.WILDCARD)
@Tag(name = "Negocio - Storage", description = "Proxy da API de Gestao de Documentos do SIMTR")
public class GestaoDocumentoResource {

    private static final Logger LOG = Logger.getLogger(GestaoDocumentoResource.class);

    private final GestaoDocumentoService gestaoDocumentoService;
    private final GestaoDocumentoMapper gestaoDocumentoMapper;

    @Inject
    public GestaoDocumentoResource(GestaoDocumentoService gestaoDocumentoService,
                                   GestaoDocumentoMapper gestaoDocumentoMapper) {
        this.gestaoDocumentoService = gestaoDocumentoService;
        this.gestaoDocumentoMapper = gestaoDocumentoMapper;
    }

    @POST
    @Path("/credencial")
    @WithSpan(value = "arvore-documento.api.gestao-documento.credencial-container.gerar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Gera credencial SAS para container de documentos",
            description = "Recebe a chamada no contrato do arvore-documento, aciona o servico de aplicacao e gera credencial SAS no simtr-gestao-documento v1."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Credencial SAS gerada com sucesso.",
                    content = @Content(schema = @Schema(implementation = GestaoDocumentoCredencialContainerDto.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Nao autorizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Canal ou usuario sem permissao.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "503",
                    description = "Servico de terceiros indisponivel no momento.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<Response> gerarCredencialContainer() {
        Span span = Span.current();
        span.setAttribute("http.route", "/arvore-documento/v1/storage/container/credencial");
        span.setAttribute("arvore_documento.api", "gestao-documento-v1");

        ObservabilityLog.info(
                LOG,
                "arvore-documento.gestao-documento.credencial-container.requisicao.recebida",
                ObservabilityLog.fields(
                        "camada", "api",
                        "componente", "GestaoDocumentoResource",
                        "operacao", "gerar-credencial-container"
                )
        );

        return gestaoDocumentoService.gerarCredencialContainer()
                .map(gestaoDocumentoMapper::toDto)
                .invoke(resposta -> {
                    setStringAttribute(span, "gestao_documento.container.nome", nomeContainer(resposta));

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.gestao-documento.credencial-container.resposta.enviada",
                            ObservabilityLog.fields(
                                    "camada", "api",
                                    "componente", "GestaoDocumentoResource",
                                    "operacao", "gerar-credencial-container",
                                    "nome_container", nomeContainer(resposta),
                                    "resultado", "sucesso"
                            )
                    );
                })
                .map(resposta -> Response.ok(resposta).build())
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.gestao-documento.credencial-container.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "api",
                                    "componente", "GestaoDocumentoResource",
                                    "operacao", "gerar-credencial-container",
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
