package br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.smallrye.mutiny.Uni;

public interface SolicitarCredencialContainer {

    Uni<CredencialContainer> obter();
}
