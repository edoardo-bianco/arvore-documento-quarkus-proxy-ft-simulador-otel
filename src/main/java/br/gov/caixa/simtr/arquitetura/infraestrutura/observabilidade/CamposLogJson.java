package br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade;

import jakarta.json.JsonObject;
import java.util.Objects;

/** Campos tecnicos ja sanitizados pela borda; nao recebe contratos ou excecoes de negocio. */
public record CamposLogJson(JsonObject campos) {

    public CamposLogJson {
        Objects.requireNonNull(campos, "Campos JSON obrigatorios.");
    }
}
