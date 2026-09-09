package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.ErroInicioMonitoramentoDto;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieRequest;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieResponse;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.IniciarMonitoramento;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Inicia a orquestracao pela porta e responde 202 somente apos confirmacao da publicacao. */
@Path("/simtr-hub/v1/monitoramentos-dossie")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Monitoramento de dossie")
public class MonitoramentoDossieResource {

    private final IniciarMonitoramento iniciar;

    @Inject
    public MonitoramentoDossieResource(IniciarMonitoramento iniciar) {
        this.iniciar = iniciar;
    }

    @POST
    @Operation(summary = "Iniciar monitoramento de dossie")
    @APIResponse(responseCode = "202", description = "Tentativa inicial confirmada pelo broker.",
            content = @Content(schema = @Schema(implementation = IniciarMonitoramentoDossieResponse.class)))
    @APIResponse(responseCode = "400", description = "Solicitacao invalida.",
            content = @Content(schema = @Schema(implementation = ErroInicioMonitoramentoDto.class)))
    @APIResponse(responseCode = "500", description = "Falha ao iniciar o monitoramento.",
            content = @Content(schema = @Schema(implementation = ErroInicioMonitoramentoDto.class)))
    public Uni<Response> iniciar(@NotNull(message = "A solicitacao deve ser informada.")
            @Valid IniciarMonitoramentoDossieRequest request) {
        return Uni.createFrom().deferred(() ->
                        iniciar.executar(MonitoramentoDossieRestMapper.paraSolicitacao(request)))
                .onItem().transform(resultado ->
                        Response.accepted(MonitoramentoDossieRestMapper.paraResposta(resultado)).build())
                .onFailure().recoverWithItem(_ -> falhaSegura());
    }

    private static Response falhaSegura() {
        var erro = new ErroInicioMonitoramentoDto(500, "simtr-hub", UUID.randomUUID().toString(),
                "ARVDOCP9999", List.of(new ErroInicioMonitoramentoDto.Mensagem(
                        "Erro interno ao processar a requisição.")));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(erro).build();
    }
}
