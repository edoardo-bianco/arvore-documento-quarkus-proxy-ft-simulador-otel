package br.gov.caixa.arvoredocumento.api.parametrizacao;

import br.gov.caixa.arvoredocumento.api.dto.erro.ErroPadraoDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.arvoredocumento.application.parametrizacao.ProcessoService;
import br.gov.caixa.arvoredocumento.mapper.parametrizacao.ProcessoMapper;
import br.gov.caixa.arvoredocumento.shared.observability.ObservabilityLog;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/arvore-documento/v1/processo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Parametrização - Processo", description = "Proxy de consulta de processo parametrizado no MTR")
public class ProcessoResource {

    private static final Logger LOG = Logger.getLogger(ProcessoResource.class);

    private final ProcessoService processoService;
    private final ProcessoMapper processoMapper;

    @Inject
    public ProcessoResource(ProcessoService processoService,
                            ProcessoMapper processoMapper) {
        this.processoService = processoService;
        this.processoMapper = processoMapper;
    }

    @GET
    @Path("/identificador-negocial/{identificador}")
    @WithSpan(value = "arvore-documento.api.processo.consultar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Consulta processo por identificador negocial",
            description = "Recebe a chamada no contrato do arvore-documento, aciona o serviço de aplicação e consulta o endpoint v2 de processo do simtr-parametrizacao."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Processo gerador localizado com sucesso.",
                    content = @Content(schema = @Schema(implementation = ProcessoDto.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Processo gerador não localizado.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<ProcessoDto> consultarPorIdentificadorNegocial(
            @PathParam("identificador")
            @SpanAttribute("processo.identificador_negocial")
            @Min(value = 1, message = "O identificador negocial deve ser maior que zero.") Long identificador) {

        Span span = Span.current();
        span.setAttribute("http.route", "/arvore-documento/v1/processo/identificador-negocial/{identificador}");
        span.setAttribute("arvore_documento.api", "parametrizacao-processo-v1");

        ObservabilityLog.info(
                LOG,
                "arvore-documento.processo.requisicao.recebida",
                ObservabilityLog.fields(
                        "camada", "api",
                        "componente", "ProcessoResource",
                        "operacao", "consultar-processo",
                        "identificador_negocial", identificador
                )
        );

        return processoService.consultarPorIdentificadorNegocial(identificador)
                .map(processoMapper::toDto)
                .invoke(processoDto -> {
                    span.setAttribute("processo.encontrado", processoDto != null);
                    if (processoDto != null && processoDto.nome() != null) {
                        span.setAttribute("processo.nome", processoDto.nome());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "arvore-documento.processo.resposta.enviada",
                            ObservabilityLog.fields(
                                    "camada", "api",
                                    "componente", "ProcessoResource",
                                    "operacao", "consultar-processo",
                                    "identificador_negocial", identificador,
                                    "resultado", "sucesso",
                                    "processo_nome", processoDto != null ? processoDto.nome() : null
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "arvore-documento.processo.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", "api",
                                    "componente", "ProcessoResource",
                                    "operacao", "consultar-processo",
                                    "identificador_negocial", identificador,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }
}
