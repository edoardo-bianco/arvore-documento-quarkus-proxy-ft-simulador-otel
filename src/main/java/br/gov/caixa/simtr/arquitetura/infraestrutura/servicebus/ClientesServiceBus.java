package br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus;

import jakarta.enterprise.inject.Vetoed;

/**
 * Concentrar builder da extensao, clientes duradouros das duas filas, qualifiers e fechamento.
 *
 * <p><strong>Estado:</strong> estrutura sem lógica, mantida fora do CDI por {@link jakarta.enterprise.inject.Vetoed}.
 * Completar no item 6.1 do checklist da feature antes de habilitar o componente.
 *
 * <p><strong>Implementação e verificação previstas:</strong>
 * <ul>
 * <li>Injetar o builder fornecido pela extensão Quarkus em uma única fábrica e criar clientes duradouros para as duas filas.</li>
 * <li>Implementar qualifiers de entrada/saída, inicialização centralizada, transporte por profile e ownership do fechamento.</li>
 * <li>Restringir consumidores às bordas Service Bus; não importar domínio, DTO ou regra de orquestrador, monitoramento ou Hub.</li>
 * <li>Provar identidade/injeção dos clientes e shutdown após encerramento das assinaturas; preservar o config.json do emulador.</li>
 * </ul>
 *
 * <p><strong>Fluxo aprovado a implementar:</strong> Usar exclusivamente o {@code ServiceBusClientBuilder} produzido pela extensão {@code quarkus-azure-servicebus:1.2.5}. Em ambiente real a credencial vem de {@code QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING}; em dev/test o Dev Services fornece a connection string do emulador. Não construir ManagedIdentityCredential/DefaultAzureCredential. Manter {@code src/main/azure/servicebus-emulator/config.json}, transporte por profile e clientes independentes por fila conforme ADR-0011.
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md}.
 *
 * <p>As referências abaixo indicam dependências previstas; ainda não há injeção, chamada ou
 * implementação de interface. Não usar a classe vazia como retorno fictício de sucesso.
 * Consultar {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
@Vetoed
public final class ClientesServiceBus {
}
