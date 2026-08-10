# Plano: remover `src/spike-test`

## Intenção

Eliminar o source set experimental criado para a investigação inicial da persistência durável,
sem perder as provas ainda úteis de compatibilidade com Quarkus Flow, Redis/Valkey e Kubernetes
Lease. Ao final, essas provas devem participar da suíte padrão e não depender de um profile Maven
manual.

## Escopo

- migrar para `src/test/java` as provas únicas de compatibilidade do SPI Flow, do provider
  Redis/Valkey e da associação Lease -> `WorkflowApplication`;
- reutilizar suporte de teste atual quando isso reduzir duplicação sem alterar o contrato provado;
- remover o mapper e os testes CouchDB experimentais já substituídos pela implementação e pelos
  testes definitivos;
- remover o profile Maven `spike-persistencia-duravel` e a pasta `src/spike-test`;
- comprovar que a suíte padrão executa as provas preservadas.

## Fora de escopo

- alterar código de produção, contratos REST/JSON/OpenAPI ou comportamento da PoC;
- alterar versões do Quarkus, Quarkus Flow, CouchDB ou Valkey;
- alterar persistência, feed documental, checkpoint, Lease, Compose ou Kubernetes;
- mudar o status do ADR-0010 ou reescrever o histórico da feature original.

## Contexto verificado

- arquitetura consolidada lida;
- ADRs aplicáveis: ADR-0006 e ADR-0010;
- código, contratos e testes inspecionados: `pom.xml`, `src/spike-test/java`, testes atuais de
  CouchDB/Flow em `src/test/java`, `tasks/features/poc-conformidade-flow-ollama-hitl/` e o guia da
  PoC;
- o profile opt-in troca integralmente o source set padrão por `src/spike-test/java` e não é
  invocado por script, CI ou guia operacional;
- as dependências `quarkus-flow-redis` e `quarkus-flow-durable-kubernetes` já pertencem ao runtime
  normal, embora o registro histórico da Task 7.1 descreva o estado anterior ao incremento que as
  promoveu;
- o protótipo `CouchDbChangeEventMapper` não possui consumidor de produção e seu cenário foi
  substituído pelo feed e pelo `CloudEventMapper` definitivos.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Nenhum path, status, JSON, DTO ou OpenAPI será alterado | Não |
| Arquitetura | Apenas consolidação da estratégia de testes prevista no ADR-0006 | Não |
| Segurança | Nenhuma credencial, entrada ou autorização será alterada | Não |
| Comportamento observável | Nenhum log, span, métrica, health ou configuração de runtime será alterado | Não |

## Tarefas

### Task 1 — Incorporar as provas úteis à suíte padrão

**Descrição:** mover as provas únicas de compatibilidade para `src/test/java`, preservando o
contrato comprovado e eliminando suporte duplicado quando houver equivalente atual.

**Critérios de aceitação:**

- a suíte padrão confirma as versões/assinaturas SPI efetivamente usadas;
- a suíte padrão executa o contrato do provider Redis contra Valkey;
- a suíte padrão confirma a associação do nome da Lease ao ID da `WorkflowApplication`;
- nenhum teste depende do profile `spike-persistencia-duravel` para ser compilado.

**Verificação:**

- testes focados das três provas migradas;
- `mvn -q test` permanece GREEN.

**Dependências:** baseline Sonar local e GO humano C0.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/conformidade/integracao/FlowPersistenciaSpiCompatibilidadeTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/conformidade/integracao/RedisCheckpointCompatibilidadeQuarkusTest.java`;
- suporte Valkey existente ou específico em `src/test/java`;
- `src/test/java/io/quarkiverse/flow/durable/kube/LeaseWorkflowApplicationCompatibilidadeTest.java`.

### Checkpoint C1 — Provas preservadas

- testes focados e suíte padrão GREEN;
- diff restrito a testes, sem alteração de produção;
- commit próprio da Task 1.

### Task 2 — Retirar o source set experimental

**Descrição:** remover os protótipos CouchDB superados, os arquivos já migrados, a pasta
`src/spike-test` e o profile Maven que a selecionava.

**Critérios de aceitação:**

- `src/spike-test` não existe mais;
- `pom.xml` não contém `spike-persistencia-duravel` nem troca de source set;
- nenhuma referência executável depende do profile removido;
- a suíte padrão continua exercitando as três garantias preservadas.

**Verificação:**

- busca por referências residuais;
- `mvn -q test` GREEN;
- checkpoint SonarQube do incremento GREEN ou situação técnica apresentada conforme `AGENTS.md`.

**Dependências:** Task 1 e checkpoint C1.

**Arquivos prováveis:**

- `pom.xml`;
- `src/spike-test/java/**`.

### Checkpoint CF — Encerramento

- revisão do diff quanto a correção, simplicidade, arquitetura, segurança, desempenho e escopo;
- worktree limpo após commits próprios das tasks;
- aceite humano de encerramento permanece externo à execução técnica.

## SonarQube

- fonte do baseline: SonarQube Docker local;
- pacote autorizado: nenhum, pois `sonar/` não existe;
- checkpoint esperado: baseline antes da primeira alteração executável e checkpoint depois da
  remoção do profile/source set.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| A suíte padrão ficar mais lenta por iniciar Valkey | Médio | Reutilizar recurso existente quando compatível e medir pela execução real |
| Perder uma garantia ao apagar o spike inteiro | Alto | Migrar e executar as três provas antes da remoção |
| Teste de Lease depender de classe interna da extensão | Médio | Manter o teste no mesmo package e tratá-lo explicitamente como teste de compatibilidade da versão fixada |
| Preservar um contrato CouchDB experimental divergente do runtime | Médio | Remover o mapper experimental e confiar nos testes definitivos do feed/mapper atual |

## GO necessário

Nenhuma alteração de testes executáveis ou `pom.xml` começa antes do GO humano registrado no
`todo.md`.
