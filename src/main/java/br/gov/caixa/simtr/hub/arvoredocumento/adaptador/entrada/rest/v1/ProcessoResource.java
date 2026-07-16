package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.entrada.ConsultarProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.erro.FalhaConsultaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
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

@Path("/simtr-hub/v1/processo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(
        name = "Parametrização - Processo",
        description = "Proxy de consulta de processo parametrizado no MTR"
)
public class ProcessoResource {

    private static final Logger LOG = Logger.getLogger(ProcessoResource.class);
    private static final String CAMADA = "api";
    private static final String COMPONENTE = "ProcessoResource";
    private static final String OPERACAO = "consultar-processo";

    private final ConsultarProcessoParametrizado consultarProcesso;

    @Inject
    public ProcessoResource(ConsultarProcessoParametrizado consultarProcesso) {
        this.consultarProcesso = consultarProcesso;
    }

    @GET
    @Path("/identificador-negocial/{identificador}")
    @WithSpan(value = "simtr-hub.api.processo.consultar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Consulta processo por identificador negocial",
            description = "Recebe a chamada no contrato do simtr-hub, aciona o serviço de aplicação e consulta o endpoint v2 de processo do simtr-parametrizacao."
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
            @Min(value = 1, message = "O identificador negocial deve ser maior que zero.")
            Long identificador
    ) {
        Span span = Span.current();
        span.setAttribute(
                "http.route",
                "/simtr-hub/v1/processo/identificador-negocial/{identificador}");
        span.setAttribute("simtr_hub.api", "parametrizacao-processo-v1");

        ObservabilityLog.info(
                LOG,
                "simtr-hub.processo.requisicao.recebida",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "operacao", OPERACAO,
                        "identificador_negocial", identificador
                )
        );

        return consultarProcesso.executar(new IdentificadorNegocialProcesso(identificador))
                .onFailure(FalhaConsultaProcessoParametrizado.class)
                .transform(erro -> ProcessoParametrizadoRestMapper.paraExcecaoRest(
                        (FalhaConsultaProcessoParametrizado) erro))
                .map(ProcessoParametrizadoRestMapper::paraResposta)
                .invoke(processo -> {
                    span.setAttribute("processo.encontrado", processo != null);
                    if (processo != null && processo.nome() != null) {
                        span.setAttribute("processo.nome", processo.nome());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.processo.resposta.enviada",
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", OPERACAO,
                                    "identificador_negocial", identificador,
                                    "resultado", "sucesso",
                                    "processo_nome", processo != null ? processo.nome() : null
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.processo.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "operacao", OPERACAO,
                                    "identificador_negocial", identificador,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }
}
