package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.falso;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ClientesServiceBus;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;

/** Dependencias proibidas usadas somente pela prova negativa de arquitetura. */
public final class AcessosServiceBusProibidos {
    private AcessosServiceBusProibidos() {
    }

    public record BuilderForaFabrica(ServiceBusClientBuilder builder) {
    }

    public record FabricaForaBordaServiceBus(ClientesServiceBus clientes) {
    }
}
