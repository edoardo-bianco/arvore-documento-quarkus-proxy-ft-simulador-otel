package br.gov.caixa.simtr.monitoramento.aplicacao.falso;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.spi.BeanManager;

/** Fixture positiva da hexagonal pragmatica; nao cria bean nem executa logica. */
@Unremovable
public record QuarkusReativoPermitido(Uni<String> resultado, BeanManager beans) {
}
