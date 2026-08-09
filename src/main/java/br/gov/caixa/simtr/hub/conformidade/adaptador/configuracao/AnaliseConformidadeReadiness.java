package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AnaliseConformidadeReadiness implements AsyncHealthCheck {

    static final String NOME = "simtr-hub-conformidade-documental";
    private static final String INSTANCE_ID_SONDA = "__readiness__";

    private final ArmazenarEstadoAnaliseConformidade armazenamento;
    private final String backend;

    public AnaliseConformidadeReadiness(
            ArmazenarEstadoAnaliseConformidade armazenamento,
            @ConfigProperty(name = "conformidade.persistencia.backend")
            String backend) {
        this.armazenamento = java.util.Objects.requireNonNull(
                armazenamento,
                "armazenamento");
        this.backend = validarBackend(backend);
    }

    @Override
    public Uni<HealthCheckResponse> call() {
        return armazenamento.consultar(INSTANCE_ID_SONDA)
                .replaceWith(resposta(true))
                .onFailure()
                .recoverWithItem(resposta(false));
    }

    private HealthCheckResponse resposta(boolean disponivel) {
        var resposta = HealthCheckResponse.named(NOME)
                .withData("backend", backend);
        return (disponivel ? resposta.up() : resposta.down()).build();
    }

    private static String validarBackend(String backend) {
        if (backend == null || !backend.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException("Backend documental inválido");
        }
        return backend;
    }
}
