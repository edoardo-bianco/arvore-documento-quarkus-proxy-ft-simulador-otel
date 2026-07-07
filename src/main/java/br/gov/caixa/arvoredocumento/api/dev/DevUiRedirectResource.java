package br.gov.caixa.arvoredocumento.api.dev;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@IfBuildProfile("dev")
@Path("/")
public class DevUiRedirectResource {

    private static final URI DEV_UI_URI = URI.create("/q/dev-ui/");

    @GET
    public Response redirectToDevUi() {
        return Response.seeOther(DEV_UI_URI).build();
    }
}
