package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

@FunctionalInterface
interface RegistrarEventoFlowOut {

    void registrar(EventoFlowOutRecebido evento);
}
