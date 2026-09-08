# ADR-0011: composição local do monitoramento e fábrica técnica Service Bus

- **Status:** Aceito.
- **Decisão em uma frase:** obter os parâmetros iniciais pela porta do consumidor e ACL
  local para o monitoramento, e concentrar os clientes da extensão em uma fábrica técnica acessível
  somente pelos adapters Service Bus dos dois componentes.
- **Quando consultar:** definir parâmetros da primeira mensagem, colaboração entre orquestrador e
  monitoramento, ownership do builder, injeção dos clientes ou guardrails da composição técnica.
- **Relação com ADR-0010:** detalha sua composição, sem substituir a extensão, a autenticação, os
  papéis dos componentes ou a hexagonal pragmática já aceitos.

## Contexto

A mensagem inicial exige `limiteEm` e `politicaMonitoramentoVersao` antes da publicação pelo
orquestrador. A política e sua configuração, porém, pertencem ao monitoramento. Importar a política
ou o config no núcleo do orquestrador violaria ownership; recalcular ali duplicaria a regra.

O ADR-0010 também exige uma única fábrica CDI para o builder produzido pela extensão. Colocá-la
como propriedade de um dos componentes obrigaria o outro a depender de seu adapter. Os dois
conjuntos de adapters precisam dos mesmos recursos técnicos, não de modelos de negócio comuns.

## Diretrizes de localização e responsabilidade

A capacidade técnica transversal Service Bus tem como destino
`br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`. Não é um componente de negócio nem
o destino de todo código assíncrono. Executar uma regra assincronamente não muda seu dono:

- políticas, tentativas funcionais, prazo e decisões de processamento pertencem a `monitoramento`;
- regras de início e tratamento do resultado da orquestração pertencem a `orquestrador`;
- listeners, publishers, DTOs e mappers permanecem nos adapters de seus componentes;
- somente composição técnica compartilhada dos clientes Service Bus pertence à infraestrutura.

Quarkus reativo, incluindo Mutiny (`Uni` e `Multi` quando necessários), e CDI podem ser usados
no domínio e na aplicação, conforme a hexagonal pragmática. Não se criam portas ou wrappers apenas
para esconder essas APIs. Essa permissão não leva SDK Azure, clientes ou contratos de transporte
ao núcleo e não autoriza bloqueio do event loop.

`br.gov.caixa.simtr.dossie` e `br.gov.caixa.simtr.hub` permanecem como estão; não recebem
os novos publishers/listeners. O `hub.arquitetura` conserva as capacidades
arquiteturais de suporte no âmbito do Hub; a nova raiz transversal não exige nem autoriza migrá-las.
Não generalizar segurança, erros, observabilidade ou outras capacidades do Hub nesta feature.
Novas capacidades transversais exigem necessidade concreta, plano e checkpoint próprios; não será
criado um framework assíncrono genérico ou barramento de negócio.

## Decisão

### Parâmetros iniciais por porta e ACL

O fluxo definido é:

```text
orquestrador.aplicacao (início, no item 6.1)
  -> orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento
  -> orquestrador.adaptador.saida.acl.monitoramento
  -> monitoramento.aplicacao.porta.entrada.PrepararMonitoramento
  -> monitoramento.aplicacao.casodeuso
  -> monitoramento.dominio.politica.PoliticaMonitoramento
```

- O orquestrador fornece um `Instant iniciadoEm`, gerado no servidor, à sua porta de saída;
- a porta pública de monitoramento recebe esse instante e devolve um resultado imutável com
  `limiteEm` e `politicaMonitoramentoVersao`, calculados pela política CDI já validada;
- a ACL traduz o resultado do fornecedor para um tipo próprio do orquestrador. Não existe DTO
  Java compartilhado, acesso à implementação concreta, REST local ou importação da configuração;
- a operação é local e síncrona, somente cálculo em memória: não consulta pré-validação/Hub,
  não gera IDs, não envia mensagens, não processa tentativas nem altera estado;
- geração de IDs, instante inicial e publicação pertencem ao caso de uso do item 6.1. A consulta
  local precede a publicação; o processamento e seu resultado continuam passando pelas duas filas;
- os valores retornados compõem a mensagem inicial. Reagendamento preserva o instante, o limite e
  a versão recebidos, sem recalcular a janela a cada tentativa;
- a estratégia para uma mensagem antiga cuja versão de política não corresponda à seleção atual
  ainda precisa ser detalhada antes do processamento em 7.1/8.1; não haverá substituição silenciosa
  por uma política diferente.

### Fábrica técnica única

Na localização definida `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`, fora dos dois
núcleos e sem ser intermediária de seus packages, a composição definida é:

- um bean fábrica injeta o `ServiceBusClientBuilder` produzido pela extensão Quarkus;
- somente essa fábrica configura o builder e constrói os clientes, em inicialização centralizada;
- a fábrica disponibiliza sender e receiver assíncronos para cada uma das duas filas, de longa
  duração, diferenciados por qualifiers CDI de entrada/saída;
- publisher inicial e reagendador recebem o sender da fila de entrada; publisher de resultado
  recebe o sender da saída. Cada listener recebe o receiver da sua fila;
- os adapters recebem clientes, nunca o builder. Nenhum cliente é criado por requisição/mensagem;
- a fábrica é dona do fechamento dos clientes; adapters são donos das suas assinaturas de consumo
  e devem encerrá-las antes do fechamento dos clientes no shutdown;
- os adapters mantêm DTOs, mappers, serialização, validação de mensagem, propagação, settlement e
  mecanismos de publicação/agendamento. A decisão funcional de publicar ou reagendar permanece
  no componente de negócio; a fábrica não se torna um barramento ou serviço de negócio;
- a infraestrutura não importa `orquestrador`, `monitoramento` ou `hub`. Domínio e aplicação não
  importam a infraestrutura nem o SDK. O acesso à fábrica/clientes é limitado às bordas Service Bus;
- configuração de filas, transporte e profiles permanece a já aprovada. A extensão, o emulador,
  seus simuladores e `src/main/azure/servicebus-emulator/config.json` são preservados.

## Consequências e verificações

- A consulta de parâmetros cria uma dependência local explícita na API pública do monitoramento;
  a ACL permite trocar essa colaboração sem espalhar o modelo do fornecedor;
- o package técnico compartilhado é uma exceção delimitada à infraestrutura, não autorização para
  compartilhar contratos, regras, services genéricos ou helpers entre os contextos;
- Quarkus reativo/Mutiny e CDI permanecem permitidos no domínio e na aplicação; as portas acima
  representam a necessidade real de colaboração entre componentes, não isolamento do framework;
- testes devem provar tradução dos parâmetros e preservação dos valores, uso exclusivo do builder
  pela fábrica, identidade dos clientes, qualifiers, transporte, inicialização e shutdown;
- ArchUnit deve rejeitar acesso lateral aos adapters/casos de uso concretos, dependência dos
  núcleos na fábrica/SDK, dependência da infraestrutura nos componentes de negócio ou no Hub e
  DTO compartilhado, além de provar caminhos permitidos, inclusive Quarkus reativo no núcleo;
- o aceite desta decisão não autoriza implementar 5.1 nem 6.1 durante a conclusão de 4.1.
  Contratos e guardrails são incrementais, e a fábrica executável pertence a 6.1, conforme o plano.

## Alternativas consideradas

- **Duplicar configuração ou política no orquestrador:** cria divergência de prazo/versão;
- **Importar diretamente o producer ou a política de monitoramento:** atravessa ownership;
- **Usar REST local para preparar a mensagem:** adiciona rede e contrato sem necessidade;
- **Colocar a fábrica no adapter de um componente:** cria dependência lateral do outro;
- **Injetar o builder em cada adapter:** dispersa configuração, criação e lifecycle dos clientes;
- **Criar DTOs comuns junto da fábrica:** confunde integração técnica com contrato de negócio.

## Referências

- [ADR-0003 — portas e ACL](0003-orquestracao-e-colaboracao-por-portas.md);
- [ADR-0004 — contratos independentes](0004-contratos-independentes-por-borda.md);
- [ADR-0010 — extensão e papéis dos componentes](0010-extensao-quarkus-service-bus-connection-string-dev-services.md);
- [Entrada oficial Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html);
- [Quarkus Azure Service Bus — builder CDI e Dev Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html).

A localização e os limites da fábrica, a colaboração por ACL e os detalhes de composição são
decisões deste projeto, não imposições da extensão. O stack permanece
Quarkus `3.33.2.1`, extensão `1.2.5` e SDK `7.17.12`, já caracterizados.
