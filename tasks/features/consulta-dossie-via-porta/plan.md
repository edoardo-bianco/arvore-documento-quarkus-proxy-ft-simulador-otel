# Plano: implementar consulta de documentos do dossiê via porta CDI

## Intenção

Criar o primeiro componente do package irmão `br.gov.caixa.simtr.dossie`, no mesmo artifact e
runtime Quarkus, para consumir por CDI a porta `ConsultarDocumentosDossieProduto`. O caso de uso
concreto, as portas de saída, os adapters MTR/simulador e a observabilidade permanecem no Hub.

## Branch

- implementação: `feature/implementar-consulta-dossie-via-porta`;
- derivada da branch do guia, sem alterar a PR documental #17;
- implementação e guia terão commits e PRs separados.

## Escopo

- criar `br.gov.caixa.simtr.dossie.ConsultaDocumentosDossieProduto` como `@ApplicationScoped`;
- injetar somente a interface da porta de entrada do Hub;
- expor `consultar(CriteriosConsultaDocumentosDossieProduto)` retornando
  `Uni<List<DocumentoDossieProdutoConsultado>>`;
- testar delegação, lista vazia, falha e wiring CDI sem HTTP;
- ampliar o ArchUnit para todo `br.gov.caixa.simtr` e proibir acesso do package irmão a caso de
  uso, porta de saída, adapter, Resource e REST Client;
- atualizar a arquitetura consolidada após o estado implementado ficar verde.

## Fora de escopo

- alterar endpoint, OpenAPI, DTO REST/MTR, status HTTP ou integração externa;
- injetar ou tornar CDI `ConsultarDocumentosDossieProdutoCasoDeUso`;
- criar REST Client para o próprio Hub;
- alterar MTR/simulador, configuração, timeout, retry, circuit breaker ou dependências Maven;
- criar autenticação, autorização ou nova superfície externa;
- adicionar log/span ou registrar CPF, CNPJ, IP, URL, payload ou documentos;
- atualizar formatos derivados.

## Contrato CDI local proposto

| Elemento | Definição |
|---|---|
| Bean | `br.gov.caixa.simtr.dossie.ConsultaDocumentosDossieProduto` |
| Escopo | `@ApplicationScoped` |
| Dependência | interface `ConsultarDocumentosDossieProduto` do Hub |
| Operação | `consultar(CriteriosConsultaDocumentosDossieProduto)` |
| Retorno | `Uni<List<DocumentoDossieProdutoConsultado>>` |
| Semântica | preservar os critérios, a lista, a lista vazia e a falha da porta |

O CDI resolverá `ConsultaDocumentosDossieProdutoObservabilidade`, que envolve o caso de uso e
usa a saída selecionada pelo producer.

## Critérios de aceitação

- o package existe no mesmo módulo Maven e injeta apenas a porta de entrada;
- não há HTTP local, DTO REST, status HTTP ou bloqueio do `Uni`;
- o resultado e `FalhaConsultaDocumentosDossieProduto` não são convertidos nem mascarados;
- o wiring CDI não é ambíguo;
- o span INTERNAL existente continua observando a consulta, sem novo span SERVER;
- nenhuma PII ou payload é adicionado à telemetria;
- o ArchUnit cobre o package irmão e possui prova negativa do caso de uso concreto;
- endpoint e testes existentes permanecem inalterados e verdes;
- suíte completa e checkpoint SonarQube são executados.

## Checkpoints humanos antes de `src/`

| Dimensão | Proposta |
|---|---|
| Contrato | adição Java interna; REST, OpenAPI e MTR inalterados |
| Arquitetura | package irmão depende somente da porta e dos tipos da assinatura |
| Segurança | sem nova borda; validação fica na borda chamadora; nenhuma PII em logs |
| Observabilidade | reutilizar wrapper existente; não criar log/span; sem span SERVER local |

Todos os quatro checkpoints e o GO devem ser aprovados explicitamente antes do primeiro teste.

## Fatias

### 0 — Autorização e baseline

1. obter os checkpoints e o GO;
2. verificar pacotes em `sonar/` e obter escolha humana quando existirem;
3. inicializar o baseline conforme `AGENTS.md`.

### 1 — RED/GREEN do consumidor

1. escrever e executar teste RED de delegação, lista vazia e falha;
2. implementar o bean CDI mínimo;
3. executar o teste focado até GREEN e revisar o diff.

### 2 — Wiring e guardrail

1. escrever e obter GREEN no `@QuarkusTest` sem HTTP;
2. ampliar o ArchUnit e adicionar prova negativa;
3. executar testes focados de CDI, observabilidade e arquitetura.

### 3 — Verificação

1. executar a suíte Maven completa e checkpoint SonarQube;
2. revisar correção, simplicidade, arquitetura, segurança, desempenho e escopo;
3. atualizar a arquitetura consolidada;
4. apresentar evidências e aguardar encerramento humano.

## Verificações previstas

```powershell
mvn -q -Dtest=ConsultaDocumentosDossieProdutoTest test
mvn -q -Dtest=ConsultaDocumentosDossieProdutoQuarkusTest test
mvn -q -Dtest=ArchUnitProgressivoTest,ConsultaDocumentosDossieProdutoObservabilidadeTest test
mvn -q test
./validar-checkpoint-sonarqube.ps1
```

## Arquivos prováveis

- `src/main/java/br/gov/caixa/simtr/dossie/ConsultaDocumentosDossieProduto.java`;
- dois testes em `src/test/java/br/gov/caixa/simtr/dossie/`;
- fixture negativa em `src/test/java/br/gov/caixa/simtr/dossie/falso/`;
- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/guardrails/ArchUnitProgressivoTest.java`;
- `doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md`;
- `tasks/features/consulta-dossie-via-porta/plan.md` e `todo.md`.

## Riscos e controles

| Risco | Controle |
|---|---|
| acesso ao caso de uso/adapters | constructor injection da porta e prova ArchUnit |
| wrapper sem responsabilidade | componente mínimo, específico e sem generalizações futuras |
| perder validação REST | nenhuma borda nova; entrada bruta deve ser validada pelo chamador |
| perder rastreabilidade | manter o wrapper observável existente e testar o wiring |
| vazamento de PII | não adicionar telemetria e revisar o diff |
| contaminar a PR do guia | branch e PR separadas |

## Dependências

- checkpoints e GO humanos;
- baseline SonarQube conforme a fonte escolhida quando houver pacote offline.
