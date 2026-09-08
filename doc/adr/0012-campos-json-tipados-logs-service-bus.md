# ADR-0012: campos JSON tipados nos logs das bordas Service Bus

- **Status:** Aceito em 2026-09-07, restrito ao novo desenvolvimento de monitoramento/orquestrador.
  O usuário autorizou a alteração preservando código, logs e erros de `simtr.hub`.
- **Decisão em uma frase:** acrescentar uma composição técnica de logging que preserve
  os formatters instalados e acrescente campos JSON tipados somente aos novos registros marcados
  pelas bordas Service Bus, mantendo DTOs, códigos e sanitização em cada borda.
- **Quando consultar:** implementar a emissão do erro JSON de monitoramento/orquestrador ou
  alterar a instalação, o marcador técnico e os limites dessa composição.
- **Relação:** complementa os limites transversais do ADR-0011 sem substituí-lo; preserva
  os contratos independentes do ADR-0004 e a compatibilidade observável do ADR-0006.

## Contexto

O formato de erro solicitado mantém os nomes e tipos do REST, incluindo
`erros: [{mensagem}]` e localização numérica do JSON. No runtime efetivo,
`quarkus-logging-json:3.33.2.1` usa `jboss-logmanager:3.2.1.Final`.
O formatter recebe o MDC convertido em strings: colocar lista ou número no MDC não preserva
seus tipos no JSON. Serializar o erro na mensagem também produz texto escapado.

As duas bordas Service Bus precisam dessa capacidade técnica. Seus DTOs e códigos continuam
independentes, e o helper de observabilidade do Hub não pode ser importado nem generalizado.
O ADR-0011 exige necessidade concreta, plano e checkpoint próprios para uma nova capacidade
transversal. A prova de viabilidade e a execução pertencem às tasks da feature.

## Decisão e limites

Criar `br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade` somente para:

1. transportar um marcador técnico imutável com campos JSON já sanitizados pela borda;
2. decorar os formatters JSON dos destinos já instalados pelo Quarkus;
3. instalar a decoração uma vez no lifecycle CDI e restaurar os delegates no encerramento.

O decorator limita sua seleção aos adapters Service Bus de `monitoramento` e `orquestrador`.
Categorias do Hub são sempre excluídas, inclusive se receberem o marcador por engano.
O decorator primeiro delega ao formatter instalado. Sem categoria elegível e marcador específico, devolve
a mesma saída, sem parse, mudança de texto ou metadados. Para registros marcados, acrescenta os
campos tipados ao objeto JSON produzido pelo delegate, preservando os metadados do logger.
Campos do marcador não podem sobrescrever timestamp, level, loggerName ou outros metadados.

A instalação é o ponto transversal e alcança os handlers JSON de console/arquivo existentes,
inclusive quando acessíveis por handlers intermediários. Não cria destino ou writer adicional,
não reabre o arquivo, não muda níveis, filtros, rotação, propagação ou configuração JSON.
Um evento pode ser entregue aos destinos já configurados; isso não significa registrar a falha
duas vezes no código.

Categorias, eventos, códigos, mensagens, DTO de erro, identidade da ocorrência e sanitização
pertencem aos adapters de monitoramento/orquestrador. O marcador não aceita Throwable nem
contrato de negócio. Somente esses adapters usam a nova capacidade neste recorte.
Hub, REST, domínio e aplicação não passam a depender dela.

## Critérios antes de considerar implementado

- Provar no runtime Quarkus a saída JSON de console/arquivo quando habilitados, com array,
  objetos e números reais, e o comportamento do console textual já usado nos testes.
- Provar delegação sem alteração para registros existentes, instalação idempotente,
  encerramento, isolamento concorrente de ocorrências e ausência de eventos duplicados.
- Preservar contexto/trace válido sem acumular MDC entre mensagens; não enviar a exceção
  original ou seu payload ao formatter.
- Proteger ausência de sobrescrita de metadados e comportamento explícito diante de
  incompatibilidade de formatter; não esconder falha de emissão ou declarar registro concluído.
- Provar por ArchUnit que DTOs e responsabilidade de classificação permanecem nas bordas.
- Executar regressão e checkpoint Sonar com o baseline preservado antes de encerrar a subfatia.

## Consequências e alternativas

A composição toca handlers compartilhados; o checkpoint C4.3 foi autorizado com a restrição
explícita de preservar código, logs e erros do Hub. Somente novos eventos elegíveis recebem campos adicionais. O parse/serialização adicional ocorre apenas nos eventos
marcados de erro. O lifecycle e handlers intermediários precisam de testes de integração;
a prova isolada não substitui esses testes.

Não trocar a extensão de logging nem adicionar dependência. Não criar DTO de erro compartilhado,
framework genérico de tratamento, persistência, consumidor de logs ou automação.
Não usar MDC como substituto de JSON tipado, JSON escapado na mensagem, escrita direta em stdout
ou segundo writer para o mesmo arquivo.

Referência oficial: [Logging no Quarkus 3.33](https://quarkus.io/version/3.33/guides/logging/).
O comportamento dos tipos foi conferido também no código-fonte e nos binários efetivos.
