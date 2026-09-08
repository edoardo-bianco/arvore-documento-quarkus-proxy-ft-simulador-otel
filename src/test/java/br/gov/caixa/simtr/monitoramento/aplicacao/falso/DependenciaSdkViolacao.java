package br.gov.caixa.simtr.monitoramento.aplicacao.falso;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;

/** Fixture negativa; nunca faz parte do artefato de producao. */
public record DependenciaSdkViolacao(ServiceBusClientBuilder builder) {
}
