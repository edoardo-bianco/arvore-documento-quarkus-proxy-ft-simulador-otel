package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.IniciarAnaliseConformidadeRequest;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.IniciarAnaliseConformidadeResponse;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.RevisaoAnaliseConformidadeRequest;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.VisaoAnaliseConformidadeResponse;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.RevisarAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path(AnaliseConformidadeResource.BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(
        name = "Conformidade - Análise",
        description = "PoC de análise de conformidade com revisão humana")
public class AnaliseConformidadeResource {

    static final String BASE_PATH = "/simtr-hub/v1/conformidade/analises";

    private final IniciarAnaliseConformidade iniciar;
    private final ConsultarAnaliseConformidade consultar;
    private final RevisarAnaliseConformidade revisar;

    @Inject
    public AnaliseConformidadeResource(
            IniciarAnaliseConformidade iniciar,
            ConsultarAnaliseConformidade consultar,
            RevisarAnaliseConformidade revisar) {
        this.iniciar = iniciar;
        this.consultar = consultar;
        this.revisar = revisar;
    }

    @POST
    @Operation(summary = "Inicia uma análise de conformidade")
    @APIResponse(
            responseCode = "202",
            description = "Análise aceita.",
            content = @Content(schema = @Schema(
                    implementation = IniciarAnaliseConformidadeResponse.class)))
    @APIResponse(
            responseCode = "400",
            description = "Payload inválido.",
            content = @Content(schema = @Schema(implementation = ErroPadraoDto.class)))
    @APIResponse(
            responseCode = "503",
            description = "Serviço de análise temporariamente indisponível.",
            content = @Content(schema = @Schema(implementation = ErroPadraoDto.class)))
    public Response iniciar(
            @NotNull(message = "O corpo da requisição deve ser informado.")
            @Valid IniciarAnaliseConformidadeRequest request) {
        var visao = iniciar.executar(AnaliseConformidadeRestMapper.paraSolicitacao(request));
        var resposta = AnaliseConformidadeRestMapper.paraInicio(visao);
        return Response.accepted(resposta)
                .header(HttpHeaders.LOCATION, BASE_PATH + "/" + visao.instanceId())
                .build();
    }

    @GET
    @Path("/{instanceId}")
    @Operation(summary = "Consulta o estado atual de uma análise de conformidade")
    @APIResponse(
            responseCode = "200",
            description = "Estado atual da análise.",
            content = @Content(schema = @Schema(
                    implementation = VisaoAnaliseConformidadeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Instância não localizada.",
            content = @Content(schema = @Schema(implementation = ErroPadraoDto.class)))
    public VisaoAnaliseConformidadeResponse consultar(
            @PathParam("instanceId")
            @NotBlank(message = "O identificador da instância deve ser informado.")
            String instanceId) {
        return AnaliseConformidadeRestMapper.paraVisao(consultar.executar(instanceId));
    }

    @PUT
    @Path("/{instanceId}/revisao")
    @Operation(summary = "Envia a revisão humana de uma análise de conformidade")
    @APIResponse(responseCode = "202", description = "Revisão aceita.")
    @APIResponse(
            responseCode = "400",
            description = "Payload inválido.",
            content = @Content(schema = @Schema(implementation = ErroPadraoDto.class)))
    @APIResponse(
            responseCode = "404",
            description = "Instância não localizada.",
            content = @Content(schema = @Schema(implementation = ErroPadraoDto.class)))
    @APIResponse(
            responseCode = "409",
            description = "A instância não aguarda revisão.",
            content = @Content(schema = @Schema(implementation = ErroPadraoDto.class)))
    @APIResponse(
            responseCode = "422",
            description = "Revisão inconsistente com o resultado preliminar.",
            content = @Content(schema = @Schema(implementation = ErroPadraoDto.class)))
    public Uni<Response> revisar(
            @PathParam("instanceId")
            @NotBlank(message = "O identificador da instância deve ser informado.")
            String instanceId,
            @NotNull(message = "O corpo da requisição deve ser informado.")
            @Valid RevisaoAnaliseConformidadeRequest request) {
        return revisar.executar(
                        instanceId,
                        AnaliseConformidadeRestMapper.paraRevisao(request))
                .replaceWith(Response.accepted().build());
    }
}
