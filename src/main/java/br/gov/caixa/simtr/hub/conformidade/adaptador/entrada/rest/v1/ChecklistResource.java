package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
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

@Path("/simtr-hub/v1/checklist")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(
        name = "Parametrização - Checklist",
        description = "Proxy de consulta de checklist parametrizado no MTR"
)
public class ChecklistResource {

    private static final Logger LOG = Logger.getLogger(ChecklistResource.class);
    private static final String CAMADA = "api";
    private static final String COMPONENTE = "ChecklistResource";
    private static final String OPERACAO = "consultar-checklist";
    private static final String CAMADA_KEY = "camada";
    private static final String COMPONENTE_KEY = "componente";
    private static final String OPERACAO_KEY = "operacao";
    private static final String IDENTIFICADOR_NEGOCIAL_KEY = "identificador_negocial";
    private static final String VERSAO_KEY = "versao";

    private final ConsultarChecklist consultarChecklist;

    @Inject
    public ChecklistResource(ConsultarChecklist consultarChecklist) {
        this.consultarChecklist = consultarChecklist;
    }

    @GET
    @Path("/identificador-negocial/{identificador}/versao/{versao}")
    @WithSpan(value = "simtr-hub.api.checklist.consultar", kind = SpanKind.SERVER)
    @Operation(
            summary = "Consulta checklist por identificador negocial e versão",
            description = "Recebe a chamada no contrato do simtr-hub, aciona o serviço de aplicação e consulta o endpoint v1 de checklist do simtr-parametrizacao."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Checklist localizado com sucesso.",
                    content = @Content(schema = @Schema(implementation = ChecklistDto.class))
            ),
            @APIResponse(
                    responseCode = "204",
                    description = "Checklist não localizado."
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Erro interno.",
                    content = @Content(schema = @Schema(implementation = ErroPadraoDto.class))
            )
    })
    public Uni<ChecklistDto> consultarPorIdentificadorNegocialEVersao(
            @PathParam("identificador")
            @SpanAttribute("checklist.identificador_negocial")
            @Min(value = 1, message = "O identificador negocial deve ser maior que zero.")
            Long identificador,
            @PathParam("versao")
            @SpanAttribute("checklist.versao")
            @Min(value = 1, message = "A versão do checklist deve ser maior que zero.")
            Integer versao
    ) {
        Span span = Span.current();
        span.setAttribute(
                "http.route",
                "/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}"
        );
        span.setAttribute("simtr_hub.api", "parametrizacao-checklist-v1");

        ObservabilityLog.info(
                LOG,
                "simtr-hub.checklist.requisicao.recebida",
                ObservabilityLog.fields(
                        CAMADA_KEY, CAMADA,
                        COMPONENTE_KEY, COMPONENTE,
                        OPERACAO_KEY, OPERACAO,
                        IDENTIFICADOR_NEGOCIAL_KEY, identificador,
                        VERSAO_KEY, versao
                )
        );

        return consultarChecklist.executar(new ComandoConsultaChecklist(identificador, versao))
                .onFailure(FalhaConsultaChecklist.class)
                .transform(erro -> ChecklistRestMapper.paraExcecaoRest(
                        (FalhaConsultaChecklist) erro))
                .map(ChecklistRestMapper::paraResposta)
                .invoke(checklist -> {
                    span.setAttribute("checklist.encontrado", checklist != null);
                    if (checklist != null && checklist.nome() != null) {
                        span.setAttribute("checklist.nome", checklist.nome());
                    }

                    ObservabilityLog.info(
                            LOG,
                            "simtr-hub.checklist.resposta.enviada",
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    OPERACAO_KEY, OPERACAO,
                                    IDENTIFICADOR_NEGOCIAL_KEY, identificador,
                                    VERSAO_KEY, versao,
                                    "resultado", "sucesso",
                                    "checklist_nome", checklist != null
                                            ? checklist.nome()
                                            : null
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));

                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.checklist.requisicao.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    OPERACAO_KEY, OPERACAO,
                                    IDENTIFICADOR_NEGOCIAL_KEY, identificador,
                                    VERSAO_KEY, versao,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"
                            )
                    );
                });
    }
}
