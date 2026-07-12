# Decisoes de arquitetura hexagonal e preparacao para Quarkus Flow

> **STATUS: SUBSTITUIDO.** Este documento preserva apenas o historico da entrevista
> anterior e nao deve orientar implementacoes. A decisao vigente esta em
> [`arquitetura-ddd-integracoes-atomicas.md`](arquitetura-ddd-integracoes-atomicas.md).
> Em caso de divergencia, o documento vigente prevalece.

## Status

Substituido em 2026-07-11 por uma nova entrevista iniciada sem assumir estas decisoes.

Este documento registra o ponto de retomada para uma nova sessao com os skills oficiais
`using-agent-skills` e `interview-me` do pacote `addyosmani/agent-skills`.

## Objetivo imediato

Preparar o `simtr-hub` para uma futura orquestracao stateful com Quarkus Flow, sem implementar o
workflow agora.

A refatoracao atual deve desacoplar completamente:

```text
DTO da API publica do simtr-hub
  -> mapper da borda REST
  -> VO/modelo interno
  -> caso de uso reativo
  -> porta de saida orientada a VO
  -> adapter MTR
  -> mapper da borda MTR
  -> DTO especifico do contrato MTR
```

O futuro Quarkus Flow deve trabalhar apenas com casos de uso, portas de entrada, VOs e estado do
fluxo. Ele nao deve conhecer REST Client, DTO MTR, paths, URLs ou detalhes do protocolo HTTP.

## Contexto futuro confirmado

- O `simtr-hub` podera orquestrar chamadas das APIs MTR por Quarkus Flow.
- O orquestrador mantera estado e decidira as transicoes do processo.
- Quarkus Flow gerenciara checkpoints tecnicos em Redis.
- O estado de negocio sera persistido futuramente em Azure Cosmos DB.
- Havera somente uma execucao ativa por proposta.
- Uma complementacao criara uma execucao filha, preservando historico e usando o mesmo dossie MTR.
- A necessidade de complementacao sera detectada quando o usuario consultar a proposta e o Hub
  reconciliar o estado com o MTR.
- Em indisponibilidade do MTR, a consulta devera retornar o ultimo estado conhecido com indicacao
  explicita de sincronizacao pendente.
- O MTR apontara pendencias, e o Hub recalculara a arvore documental com o Core IA.
- O calculo da arvore sera assincrono em um fluxo especifico do Quarkus Flow.
- Atualizacoes ao front-end serao notificadas por WebSocket; eventos serao pequenos, e o estado ou
  a arvore completa sera recuperado por REST.
- A arvore calculada solicitara documentos imediatamente, sem aprovacao humana previa.
- Baixa confianca ou divergencia da IA nao bloqueara a solicitacao; os detalhes ficarao restritos
  a suporte e auditoria.
- Em esgotamento de tentativas, o suporte podera `REEXECUTAR` ou `CANCELAR`, sempre com auditoria;
  nao havera acao generica de pular etapa.
- A saga futura retomara a partir da operacao/documento que falhou, combinando retry automatico e
  intervencao manual depois do limite.

Essas decisoes futuras orientam o desacoplamento atual, mas nao autorizam implementar Cosmos DB,
Redis, WebSocket, Core IA ou o workflow nesta etapa.

## Contextos de dominio

### `prevalidacao` (futuro)

Responsavel futuramente por proposta, parametrizacao capturada, resultado da IA, arvore documental,
documentos, estado e orquestracao.

Devera manter um snapshot imutavel e versionado da parametrizacao usada no calculo da arvore.

O contexto `prevalidacao` chamara as portas de entrada de outros contextos. Ele nao acessara
diretamente adapters ou portas de saida do MTR.

### `dossieproduto`

Responsavel pelas capacidades atomicas de integracao do dossie:

- criar dossie;
- atualizar formulario;
- incluir documento;
- registrar validacao negocial;
- iniciar ou avancar workflow.

Os dados completos da proposta e o resultado da IA nao pertencem a `dossieproduto`. Esse contexto
recebera somente os VOs preparados para cada operacao.

### `parametrizacao` e `gestaodocumento`

Serao migrados para o mesmo padrao depois da validacao do piloto em `dossieproduto`.

## Estrutura aprovada

```text
<dominio>/
|-- dominio/
|   |-- modelo/
|   `-- erro/
|-- aplicacao/
|   |-- porta/
|   |   |-- entrada/
|   |   `-- saida/
|   `-- casodeuso/
`-- adaptador/
    |-- entrada/
    |   |-- rest/v1/
    |   |   |-- dto/
    |   |   |-- mapper/
    |   |   `-- resource/
    |   `-- flow/                 # futuro
    `-- saida/
        |-- mtr/
        |   |-- dto/
        |   |   |-- v1/
        |   |   `-- v2/
        |   |-- mapper/
        |   |-- client/
        |   |-- erro/
        |   `-- adapter/
        `-- simulador/
```

## Regras de dependencia

- `dominio` nao importa Quarkus, Mutiny, Jackson, Jakarta, REST, adapters ou DTOs.
- `aplicacao` pode usar `Uni`, mas nao importa `adaptador`.
- Portas de entrada e saida trabalham exclusivamente com VOs e tipos internos.
- DTOs publicos existem apenas no adapter REST de entrada.
- DTOs MTR existem apenas no adapter MTR de saida.
- REST Clients existem apenas no adapter MTR.
- O futuro adapter Flow chama portas de entrada/casos de uso e nao conhece portas de saida ou MTR.
- Nenhum mapper converte diretamente DTO publico em DTO MTR.

Essas regras serao obrigatorias no build por testes ArchUnit.

## DTOs e mapeadores

Foram aprovados dois conjuntos independentes de DTOs, mesmo quando inicialmente identicos:

1. DTOs da API publica do `simtr-hub`;
2. DTOs privados de integracao com o MTR.

Tambem foram aprovados mapeadores separados por borda:

```text
ApiDossieProdutoMapper: DTO publico <-> VO
MtrDossieProdutoMapper: VO <-> DTO MTR
```

Os DTOs MTR serao isolados por versao e operacao. Exemplo:

```text
adaptador/saida/mtr/dto/
|-- v1/
|   |-- criacao/
|   |-- formulario/
|   |-- validacao/
|   `-- workflow/
`-- v2/
    `-- documento/
```

Estruturas semelhantes nao devem ser compartilhadas entre versoes sem confirmacao de que pertencem
ao mesmo contrato estavel.

## VOs e modelo interno

- O sufixo `Vo` sera mantido na primeira refatoracao para reduzir impacto.
- Os VOs continuarao inicialmente como `record` imutaveis.
- Eles permanecerao livres de annotations de framework.
- Nao serao inventadas regras de negocio nesta etapa.
- Invariantes e comportamentos poderao ser adicionados futuramente quando as regras forem conhecidas.
- A modelagem de conteudo e metadados de documentos foi explicitamente adiada para entrevista futura.

## Portas e casos de uso

Granularidade aprovada:

- uma porta de entrada por caso de uso;
- uma porta de saida consolidada por capacidade/sistema externo.

Exemplo:

```text
CriarDossieProdutoUseCase ---------+
AtualizarFormularioUseCase --------+
IncluirDocumentoUseCase -----------+--> DossieProdutoPort
RegistrarValidacaoNegocialUseCase -+
AvancarWorkflowUseCase ------------+
```

O caso de uso atual de criacao representa somente a operacao atomica no MTR. Uma futura
`OrquestrarPreValidacaoUseCase` pertencera ao contexto `prevalidacao` e coordenara os casos de uso
atomicos, sem montar DTOs MTR.

As fachadas delegadoras atuais poderao ser substituidas por portas de entrada/casos de uso
explicitos.

## Reatividade

- `Uni` e uma decisao arquitetural permitida na camada `aplicacao`.
- Casos de uso e portas serao naturalmente assincronos e nao bloqueantes.
- O futuro Flow podera compor casos de uso reativos.
- VOs nao carregarao efeitos assincronos.
- Qualquer adapter que execute operacao bloqueante devera desloca-la para worker thread sem expor
  esse detalhe ao caso de uso.

## MTR, simulador e roteamento

MTR e simulador implementarao a mesma porta orientada a VO.

Foi aprovado um adapter roteador por dominio:

```text
DossieProdutoPort
        ^
DossieProdutoRoutingAdapter
   |-- MtrDossieProdutoAdapter
   `-- SimuladorDossieProdutoAdapter
```

O roteador preservara a configuracao atual de simulador, mas removera os condicionais dos casos de
uso. O futuro Flow enxergara somente a porta/caso de uso.

## Erros e fault tolerance

- Excecoes HTTP e tipos do REST Client nao atravessarao a porta.
- O adapter MTR traduzira falhas externas para erros internos tipados.
- Informacoes como status e codigo externo poderao ser preservadas internamente para diagnostico e
  classificacao, sem expor o protocolo ao nucleo.
- O Resource traduzira erros internos para o contrato HTTP publico.
- O futuro Flow decidira retry, suspensao ou intervencao usando erros internos.
- Timeout, retry curto e circuit breaker pertencem exclusivamente ao adapter MTR.
- O simulador nao herdara automaticamente essas politicas.
- O retry duravel do futuro workflow sera uma politica distinta do retry HTTP imediato.

## Validacao

- DTO publico: Jakarta Validation para o contrato HTTP.
- VO: invariantes de negocio futuras, sem annotations de framework.
- DTO MTR: Jackson e detalhes do contrato externo.
- Adapter MTR: validacao defensiva das respostas externas indispensaveis.
- Caso de uso: regras funcionais quando surgirem.
- As validacoes publicas atuais serao preservadas.
- O uso depreciado de `@Valid List<T>` sera corrigido para `List<@Valid T>` com testes de
  equivalencia.

## Observabilidade

Spans serao mantidos em tres niveis:

```text
adapter de entrada -> operacao recebida por REST ou Flow
caso de uso        -> operacao funcional interna
adapter MTR        -> chamada externa
```

- VOs e portas nao registram logs.
- O roteador registra apenas a selecao do adapter quando necessario.
- Duplicacoes de logs de inicio, sucesso e falha devem ser reduzidas.
- Eventos e atributos usados externamente por dashboards devem ser preservados.

## Compatibilidade obrigatoria

Esta etapa nao alterara:

- paths e verbos HTTP;
- status HTTP;
- campos e nomes JSON;
- validacoes observaveis;
- codigos e corpos de erro;
- configuracao e comportamento do simulador;
- comportamento de observabilidade, salvo detalhes internos nao contratuais.

## Estrategia de migracao

Ordem aprovada:

1. migrar `dossieproduto` como piloto;
2. executar e reforcar testes de equivalencia;
3. migrar `parametrizacao`;
4. migrar `gestaodocumento`.

Os testes existentes serao tratados como testes de caracterizacao. Devem ser adicionados testes de
equivalencia para:

```text
JSON publico -> DTO API -> VO -> DTO MTR
DTO MTR de resposta -> VO -> DTO API de resposta
```

Tambem devem cobrir nulos, listas, objetos aninhados, nomes JSON, validacoes, status e erros.

## Regras ArchUnit aprovadas

- `dominio` nao depende de frameworks ou adapters;
- `aplicacao` nao depende de `adaptador`;
- portas e casos de uso nao usam DTOs;
- DTOs da API nao aparecem no adapter MTR;
- DTOs MTR nao aparecem em Resource, aplicacao ou dominio;
- REST Clients ficam restritos a `adaptador.saida.mtr`.

## Decisoes adiadas

- Modelo detalhado dos documentos e referencias ao Blob Storage.
- Retencao no Cosmos DB.
- Desenho concreto dos flows, checkpoints e estados.
- Contratos WebSocket.
- Modelo de auditoria do Core IA.
- Implementacao de compensacoes e intervencao operacional.

## Ponto exato para retomar a entrevista

**CANCELADO.** Este era o ponto de retomada da entrevista substituida. Nao ha pergunta ou acao
pendente neste documento. Use exclusivamente `arquitetura-ddd-integracoes-atomicas.md` e
`../tasks/todo.md` para qualquer retomada.
