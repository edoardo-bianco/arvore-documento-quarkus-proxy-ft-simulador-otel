package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import io.quarkiverse.flow.messaging.FlowDomainEventsPublisher;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Shim temporário para registrar a saída da ponte do Flow 0.10.2 sem connector.
 */
@ApplicationScoped
public class FlowDomainEventsPublisherSemConnector extends FlowDomainEventsPublisher {
}
