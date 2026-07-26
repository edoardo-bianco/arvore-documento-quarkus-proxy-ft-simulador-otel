package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import io.smallrye.mutiny.Uni;

@FunctionalInterface
interface RegistrarEventoFlowOut {

    Uni<Void> registrar(EventoFlowOutRecebido evento);
}
