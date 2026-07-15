package br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.smallrye.mutiny.Uni;

public interface ObterCredencialContainer {

    Uni<CredencialContainer> executar();
}
