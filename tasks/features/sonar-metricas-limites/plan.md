# Plano: adequar métricas SonarQube aos limites do agente

## Status

- **Estado:** concluído; commit criado e branch enviada ao remoto
- **Data do baseline:** 2026-07-19
- **Branch:** `refactor/sonar-metricas-limites`
- **Commit do baseline:** `ff770271041e5dafd1af31241362e7d0261fd12b`
- **GO C0:** 2026-07-19 — usuário
- **GO C1:** 2026-07-20 — usuário; deduplicação interna aprovada conforme desenho registrado
- **Escopo revisado:** 2026-07-20 — encerrar após conformidade conjunta de cobertura e duplicação
- **Aceite excepcional CMET:** 2026-07-20 — usuário; cinco `java:S1185` LOW mantidas visíveis
- **GO CF:** 2026-07-20 — usuário; encerramento, commit e push autorizados

## Intenção

Elevar a cobertura do projeto para pelo menos 85% e reduzir a densidade de duplicação para no
máximo 5%. Assim que as duas métricas forem comprovadas no mesmo checkpoint, encerrar esta feature,
revisar o diff e, após GO humano final, fazer commit e push. O trabalho preserva contratos,
fronteiras arquiteturais, segurança, fault tolerance e comportamento observável. A estratégia não
altera limites, exclusões ou Quality Gate para fazer a medição passar.

## Critérios de sucesso

- `coverage >= 85.0%` no SonarQube Docker local;
- `duplicated_lines_density <= 5.0%` no SonarQube Docker local;
- zero issue `HIGH`, `BLOCKER` ou `CRITICAL`; as únicas issues novas admitidas são as cinco
  `java:S1185` de impacto `MAINTAINABILITY: LOW`, mantidas visíveis e aceitas excepcionalmente
  nos checkpoints SDUP e CMET;
- suíte completa sem falhas, erros ou testes ignorados;
- nenhum path, verbo, status, JSON, validação, DTO MTR ou sinal observável alterado;
- nenhuma credencial, relatório de `target/` ou estado de `.codex/.state/` versionado;
- interrupção dos ajustes assim que todos os limites forem comprovados pelo checkpoint;
- revisão final, GO humano de encerramento, commit e push da branch concluídos nessa ordem.

## Escopo

- acrescentar cenários de teste para branches defensivos já existentes nos adapters prioritários;
- caracterizar as cinco exceções de domínio de `dossieproduto` antes de qualquer refatoração;
- propor e, somente após checkpoint arquitetural, extrair estrutura comum privada ao próprio
  domínio `dossieproduto`;
- executar testes focados, `clean verify` com JaCoCo e checkpoints Sonar em incrementos coerentes;
- usar métricas por arquivo e blocos de duplicação da análise local para escolher o próximo lote;
- registrar o checkpoint conjunto, concluir o planejamento, revisar o diff e publicar a branch
  somente após o GO humano final.

## Fora de escopo

- corrigir issues preexistentes, inclusive as de impacto `MEDIUM`, salvo se uma alteração já
  necessária para cobertura ou duplicação as resolver incidentalmente;
- compartilhar erros, modelos, DTOs ou mappers entre domínios ou entre REST, MTR e simulador;
- remover duplicação estrutural intencional entre fronteiras;
- alterar Quality Gate, limites de 85%/5%, exclusões Sonar/JaCoCo ou classificação de código;
- remover ou desabilitar testes, adicionar código morto ou testar implementação sem comportamento;
- alterar contratos públicos/externos, configuração Quarkus, logs, spans, atributos, timeout,
  retry ou circuit breaker;
- tratar neste trabalho os avisos atuais sobre
  `quarkus.log.console.json.mdc.flat-fields` e `quarkus.log.file.json.mdc.flat-fields`;
- ler ou associar pacote offline: o diretório `sonar/` estava ausente na verificação inicial.

## Contexto verificado

- arquitetura consolidada e índice de ADRs lidos;
- ADRs aplicáveis lidos integralmente: ADR-0001, ADR-0002, ADR-0004 e ADR-0006;
- código inspecionado:
  - `src/main/java/br/gov/caixa/simtr/hub/*/adaptador/saida/mtr/adapter/`;
  - `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/`;
  - hotspots adicionais em `DossieProdutoResource`, `RestClientObservabilityFilter` e mappers de
    processo;
- testes inspecionados:
  - testes unitários dos oito adapters MTR;
  - testes dos mappers REST de dossiê;
  - testes de contrato, observabilidade e guardrails arquiteturais relacionados;
- baseline criado somente com SonarQube Docker local, sem pacote offline;
- decisão humana `ContinuarAjustes` registrada após o baseline `NON_COMPLIANT`;
- não há divergência conhecida entre a arquitetura consolidada e o código relevante.

## Evidência inicial

Análise local `faca645b-5b1c-4e6c-9ee9-c19d9da04d43`, processada sobre o commit acima:

| Evidência | Resultado inicial | Limite |
|---|---:|---:|
| Quality Gate Sonar | `OK` | `OK` |
| Testes | 348; zero falha/erro/ignorado | todos aprovados |
| Issues abertas | 214 | informativo |
| Issues novas | 0 | 0 |
| Issues `HIGH`/`BLOCKER`/`CRITICAL` | 0 | 0 |
| Issues `MEDIUM` | 137 | informativo; fora do escopo |
| Cobertura geral | 80,0% | >= 85,0% |
| Cobertura de linhas | 88,5% | informativo |
| Cobertura de branches | 60,2% | informativo |
| Duplicação | 5,9% | <= 5,0% |

O denominador atual combina 3.534 linhas e 1.506 condições. Há 407 linhas e 600 condições não
cobertas. Mantido esse denominador, são necessários pelo menos 251 novos pontos cobertos para
alcançar 85%; por isso o plano prioriza branches, que são o maior déficit.

### Hotspots de cobertura

| Grupo | Branches não cobertos | Prioridade |
|---|---:|---|
| `Documento`, `Formulario` e `Criacao` — adapters MTR de dossiê | 75 | lote COV-A |
| `Processo`, `Checklist` e `GestaoDocumento` — adapters MTR | 69 | lote COV-B |
| `ValidacaoNegocial` e `Workflow` — adapters MTR de dossiê | 32 | contingência COV-C |
| `DossieProdutoResource` | 20 | contingência COV-C |
| `RestClientObservabilityFilter` | 16 | contingência COV-D |
| `ProcessoParametrizadoMtrMapper` | 16 | contingência COV-D |

Os gaps predominantes são payload/comando/resposta nulos, campos opcionais, corpo/lista/item de
erro ausente, subclasses de falha MTR, timeout, falha inesperada e atributos condicionais de
telemetria. Os testes devem provar esses comportamentos; não devem apenas executar linhas.

### Hotspots de duplicação

O projeto possui 735 linhas duplicadas. O maior grupo repete duas estruturas de exceção entre oito
classes de quatro domínios. A soma por arquivo das cinco classes de `dossieproduto` nesse grupo é
298 linhas duplicadas. As demais repetições entre domínios são intencionais segundo ADR-0002 e
ADR-0004 e não serão consolidadas.

A redução proposta fica restrita a uma estrutura package-private dentro de
`dossieproduto.dominio.erro`. Nenhuma das três classes equivalentes de outros domínios será
alterada, pois a duplicação entre esses limites é intencional segundo ADR-0002 e ADR-0004.

#### Desenho submetido ao C1

**Estrutura interna proposta:**

- criar `FalhaDossieProduto`, não genérica e package-private, no arquivo
  `dossieproduto/dominio/erro/FalhaDossieProduto.java`;
- a estrutura estende `RuntimeException`, concentra os oito campos atuais, a escolha da mensagem
  e os sete accessors comuns que não dependem do enum específico;
- o construtor interno recebe o tipo como `Enum<?>`, um `Dados` aninhado, causa e fallback. `Dados`
  é um record package-private com os outros sete valores atuais, exatamente no limite aceito pela
  regra `java:S107`;
- cada uma das cinco classes públicas finais passa a estender a estrutura com seu próprio enum
  `Tipo`, conserva o construtor público atual e fornece o fallback específico;
- `tipo()` permanece declarado em cada classe, com retorno no enum próprio, para preservar seu
  descritor JVM; um método protegido tipado converte com `Class.cast` o enum que o próprio
  construtor recebeu. `mensagens()` também permanece declarado para preservar a assinatura
  genérica `List<String>`. O checkpoint S2.2 confirmou que o Sonar classifica esse override de
  compatibilidade como `java:S1185`; por decisão explícita do usuário, nenhuma supressão será
  usada e as issues permanecerão visíveis. Por serem de impacto `LOW`, podem ser submetidas à
  decisão humana excepcional prevista no checkpoint, sem alterar o status técnico produzido pelo
  script. Os outros seis accessors públicos passam a ser herdados com os mesmos nomes, retornos e
  descritores;
- adapters, casos de uso, recursos, DTOs, mappers e classes de outros domínios não mudam.

**Comparação de API e comportamento:**

| Elemento | Antes | Depois proposto | Compatibilidade |
|---|---|---|---|
| Classes | cinco classes públicas finais | mesmos nomes e modificadores | preservada |
| `Tipo` | enum público aninhado com `NEGOCIO`, `TECNICA_CLIENTE`, `DEPENDENCIA_INDISPONIVEL`, `TIMEOUT` | mesmos enums, constantes e ordem | preservada |
| Construtor | público, nove parâmetros na ordem atual | mesma assinatura e ordem | preservada |
| `tipo()` | declarado em cada classe e retorna seu `Tipo` | continua declarado e retorna o mesmo `Tipo` | fonte e binário preservados |
| `mensagens()` | declarado em cada classe com retorno `List<String>` | continua declarado com a mesma assinatura genérica | fonte, binário e metadados genéricos preservados |
| `status()`, `recurso()`, `idErro()`, `codigoErro()`, `detalhe()`, `stacktraceExterno()` | declarados em cada classe | públicos e herdados; `javac` gera bridges nos subtipos públicos | chamadas fonte e binárias preservadas |
| Mensagem | primeira mensagem não nula; depois mensagem da causa; depois fallback específico | mesma ordem e mesmos fallbacks literais | preservada pelos testes de caracterização |
| Dados e causa | valores e referência da lista mantidos; causa passada a `RuntimeException` | mesmos valores, identidade da lista e causa | preservada pelos testes de caracterização |

A superclasse direta deixa de ser `RuntimeException` e passa a ser a estrutura interna, ainda
subtipo de `RuntimeException`. Um protótipo descartável confirmou compilação de um consumidor novo
e execução, sem recompilação, de um consumidor compilado contra a forma antiga. `javap -v`
confirmou os descritores anteriores e mostrou bridges públicos sintéticos para os seis accessors
herdados. Assim, reflexão que filtre `Method.isSynthetic()` pode observar diferença. Não foi
encontrado uso reflexivo nem serialização dessas cinco classes no repositório. Compatibilidade de
instâncias serializadas entre versões não será prometida. Se superclass direta, metadados
reflexivos ou serialização histórica forem considerados contrato externo, o C1 deve ser `NO-GO`,
pois manter toda a estrutura declarada manualmente em cada classe não oferece redução previsível
suficiente sem mascarar o CPD.

**Previsão de redução:**

- snapshot SCOV: 735 linhas duplicadas em 12.447 linhas, densidade 5,9%;
- limite aproximado no denominador atual: no máximo 622 linhas duplicadas, redução mínima de 113;
- as cinco classes candidatas somam 298 linhas duplicadas, em blocos de 15 e de 25 a 50 linhas;
- mantendo uma ocorrência comum e admitindo blocos pequenos nos construtores públicos, a redução
  conservadora esperada é de cerca de 175 linhas duplicadas, levando a densidade estimada para a
  faixa de 4,5% a 4,8%; somente o checkpoint Sonar será conclusivo.

**Fatias e proteção:**

1. criar a estrutura e migrar criação/formulário; executar testes focados, suíte e checkpoint
   Sonar da fatia;
2. se os limites ainda não estiverem conformes, migrar documento/validação/workflow, manter os
   cinco overrides de compatibilidade sem suprimir `java:S1185` e executar testes focados, suíte e
   SDUP;
3. comparar a API compilada, manter `FalhasDossieProdutoTest` verde e interromper ajustes assim que
   cobertura e duplicação estiverem conformes.

O desenho respeita ADR-0001 por manter a dependência dentro do domínio, ADR-0002 por não compartilhar
tipos entre domínios e ADR-0004 por não tocar contratos ou traduções de borda.

A estrutura não pode ser genérica porque Java proíbe subclasses genéricas de `Throwable`. O tipo
comum usa `Enum<?>` apenas como estado privado; cada classe pública mantém seu próprio `tipo()` com
o enum específico, sem expor wildcard no contrato.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Nenhuma mudança planejada; testes de contrato devem permanecer equivalentes | Não; qualquer necessidade interrompe o plano |
| Arquitetura | Possível extração package-private somente dentro de `dossieproduto` | Sim, C1 antes da extração |
| Segurança | Nenhuma mudança planejada; credenciais permanecem apenas em memória | Não; qualquer necessidade interrompe o plano |
| Comportamento observável | Somente caracterização dos branches existentes; nenhum nome/atributo será alterado | Não; qualquer mudança exige novo checkpoint |

## Estratégia de execução

```text
baseline local registrado
    -> C0: GO do plano
        -> incremento COV: somente testes de comportamento
            -> verify/JaCoCo e checkpoint Sonar
                -> C1: aprovar desenho arquitetural mínimo
                    -> incremento DUP: refatoração interna de dossieproduto
                        -> verify/JaCoCo e checkpoint Sonar
                            -> C2 condicional se outra extração arquitetural for necessária
                                -> CMET: coverage >= 85 e duplication <= 5 no mesmo checkpoint
                                    -> revisão e registro final
                                        -> GO humano de encerramento
                                            -> commit e push
```

## Tarefas

### Task 1 — Caracterizar as falhas de domínio de dossiê

**Descrição:** adicionar um teste dedicado que congele o comportamento das cinco exceções antes da
deduplicação e cubra as alternativas de seleção da mensagem.

**Critérios de aceitação:**

- nomes das cinco classes e enums `Tipo` permanecem utilizáveis;
- accessors, causa, primeira mensagem válida, mensagem da causa e fallback específico são provados;
- nenhum arquivo de produção é alterado nesta task.

**Verificação:**

- `mvn -q -Dtest=FalhasDossieProdutoTest test`;
- inspeção de que as asserções verificam valores, não apenas ausência de exceção.

**Dependências:** C0.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhasDossieProdutoTest.java`.

**Escopo estimado:** S, um arquivo.

### Task 2 — Cobrir os adapters MTR de dossiê com maior déficit

**Descrição:** ampliar as matrizes dos adapters de documento, formulário e criação para cobrir
payloads opcionais/nulos, resposta ausente/parcial, erro MTR incompleto e falha inesperada.

**Critérios de aceitação:**

- cada novo cenário prova resultado, classificação de erro ou atributo observável já existente;
- contratos e políticas de fault tolerance não mudam;
- os 75 branches inicialmente ausentes são reduzidos de forma mensurável.

**Verificação:**

- testes focados das três classes;
- comparação do JaCoCo por classe depois do lote.

**Dependências:** Task 1.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/DocumentoDossieProdutoMtrAdapterTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/FormularioDossieProdutoMtrAdapterTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/CriacaoDossieProdutoMtrAdapterTest.java`.

**Escopo estimado:** M, três arquivos.

### Task 3 — Cobrir os adapters MTR dos demais domínios prioritários

**Descrição:** completar as matrizes de processo, checklist e gestão de documento com os mesmos
tipos de branch, respeitando os modelos e erros próprios de cada domínio.

**Critérios de aceitação:**

- não é criado helper de produção nem contrato compartilhado entre domínios;
- corpo/lista/item de erro ausente, subtipo técnico, timeout e falha inesperada ficam caracterizados;
- os 69 branches inicialmente ausentes são reduzidos de forma mensurável.

**Verificação:**

- testes focados das três classes;
- comparação do JaCoCo por classe depois do lote.

**Dependências:** Task 2.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/arvoredocumento/adaptador/saida/mtr/adapter/ProcessoParametrizadoMtrAdapterTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/conformidade/adaptador/saida/mtr/adapter/ChecklistMtrAdapterTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/gestaodocumento/adaptador/saida/mtr/adapter/GestaoDocumentoMtrAdapterTest.java`.

**Escopo estimado:** M, três arquivos.

### Task 4 — Completar cobertura somente se ainda necessária

**Descrição:** usar o JaCoCo atualizado para escolher o menor lote adicional. A ordem é
`ValidacaoNegocial`/`Workflow`/`DossieProdutoResource`; filtro de observabilidade e mapper de
processo são contingência posterior, nunca mudança de comportamento.

**Critérios de aceitação:**

- somente classes ainda materialmente relevantes são tocadas;
- o lote para assim que a cobertura calculada atingir pelo menos 85%;
- nenhuma asserção depende de detalhes acidentais nem altera sinais observáveis.

**Verificação:**

- testes focados do lote selecionado;
- `mvn -q clean verify` e leitura de `target/jacoco-report/jacoco.xml`.

**Dependências:** Task 3; execução condicional ao resultado do JaCoCo.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/ValidacaoNegocialDossieProdutoMtrAdapterTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/WorkflowDossieProdutoMtrAdapterTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceBeanCoverageTest.java`;
- contingencialmente, testes existentes do filtro de observabilidade e mapper de processo.

**Escopo estimado:** M, no máximo três arquivos por lote.

### Checkpoint SCOV — Validar o incremento de cobertura

- executar suíte completa e confirmar ausência de ignorados;
- executar `./validar-checkpoint-sonarqube.ps1` uma vez para o incremento COV coerente;
- exigir `coverage >= 85%`, zero issue nova e zero issue severa;
- apresentar cobertura, duplicação e issues; se a duplicação ainda mantiver o checkpoint
  `NON_COMPLIANT`, obter e registrar nova decisão humana antes de prosseguir.

### Task 5 — Preparar a deduplicação interna de dossieproduto

**Descrição:** com os testes de caracterização verdes, apresentar a estrutura package-private
proposta, a lista exata de símbolos preservados e o impacto esperado no CPD, sem modificar
produção.

**Critérios de aceitação:**

- proposta limitada a `dossieproduto.dominio.erro`;
- classes públicas finais, enums aninhados e comportamento permanecem compatíveis;
- nenhuma dependência entre domínios ou bordas é criada.

**Verificação:**

- revisão contra ADR-0001, ADR-0002 e ADR-0004;
- comparação explícita de API e comportamento antes/depois proposto.

**Dependências:** SCOV.

**Arquivos prováveis:** nenhum arquivo de produção antes do C1.

**Escopo estimado:** S, análise e checkpoint.

### Checkpoint C1 — Autorizar a mudança arquitetural interna

- apresentar desenho, paths, testes de caracterização e previsão de redução;
- obter GO humano específico antes de criar a abstração ou alterar qualquer classe de produção;
- NO-GO mantém a duplicação intencional e encerra essa estratégia sem buscar atalho de métrica.

### Task 6 — Extrair a estrutura comum em duas fatias

**Descrição:** após C1, criar a abstração package-private e migrar primeiro criação/formulário;
depois migrar documento/validação/workflow. Cada fatia preserva API e mantém os testes verdes.

**Critérios de aceitação:**

- cada fatia migra no máximo três classes existentes, além da abstração na primeira fatia; a
  remoção das anotações transitórias nas duas classes da primeira fatia é um ajuste não
  comportamental determinado pelo usuário após o primeiro SDUP;
- mensagens, campos, tipos, causas e accessors permanecem equivalentes;
- ArchUnit continua aprovado e nenhum package compartilhado é criado.

**Verificação:**

- `FalhasDossieProdutoTest` e testes dos mappers/adapters de cada operação;
- suíte completa, inspeção de API compilada e checkpoint Sonar depois de cada fatia;
- comparação de duplicação por arquivo após as duas fatias.

**Dependências:** GO em C1.

**Arquivos prováveis:**

- nova abstração em `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/`;
- as cinco classes `Falha*DossieProduto.java` do mesmo package.

**Escopo estimado:** duas tasks M, até três classes existentes por fatia.

### Checkpoint SDUP — Validar o incremento de duplicação

- executar `mvn -q clean verify` e o checkpoint Sonar do incremento DUP;
- exigir duplicação <= 5%, cobertura >= 85%, zero issue nova e zero issue severa;
- encerrar a redução no primeiro resultado conforme;
- se duplicação continuar acima de 5%, apresentar os novos blocos e obter decisão humana; não
  avançar automaticamente para outro grupo.

### Task 7 — Contingência de duplicação, somente se SDUP não bastar

**Descrição:** avaliar exclusivamente as exceções MTR das cinco operações de `dossieproduto`, que
formam outro grupo dentro da mesma borda. Qualquer extração exige C2 antes de produção.

**Critérios de aceitação:**

- evidência pós-SDUP demonstra que o lote é necessário para cruzar 5%;
- desenho permanece privado a `dossieproduto.adaptador.saida.mtr.erro`;
- nenhuma exceção de outro domínio, DTO ou mapper é compartilhada.

**Verificação:**

- checkpoint humano C2;
- caracterização, testes focados, suíte e checkpoint Sonar equivalentes aos da Task 6.

**Dependências:** SDUP ainda `NON_COMPLIANT` por duplicação e GO em C2.

**Arquivos prováveis:**

- classes `*DossieProdutoMtrException.java` do próprio adapter MTR de `dossieproduto`;
- testes focados dessas exceções/adapters.

**Escopo estimado:** condicional; deve ser novamente dividido em fatias M.

### Checkpoint CMET — Encerrar os ajustes de métricas

- confirmar no mesmo snapshot `coverage >= 85%` e `duplicated_lines_density <= 5%`;
- confirmar zero issue severa; o Quality Gate `ERROR` e as cinco issues novas
  `java:S1185`/`MAINTAINABILITY: LOW` são admitidos somente pela decisão humana
  `AceitarExcepcionalmente` registrada no SDUP, sem supressão no fonte;
- interromper novos ajustes de código assim que as duas métricas estiverem conformes;
- registrar as métricas e a análise que liberam o encerramento.

### Checkpoint CF — Encerrar e publicar a feature

- `mvn -q clean verify` aprovado;
- checkpoint Sonar conforme nos limites 85%/5%, admitindo situação técnica `NON_COMPLIANT`
  exclusivamente pelas cinco `java:S1185` LOW aceitas excepcionalmente no SDUP;
- zero issue severa; Quality Gate `ERROR` somente pelas cinco issues aceitas, mantidas visíveis e
  sem supressão;
- revisão multi-eixo, `git diff --check` e inspeção de segredos aprovados;
- `plan.md` e `todo.md` finalizados com as evidências do checkpoint;
- somente arquivos autorizados versionados; nenhum derivado, relatório ou token incluído;
- GO humano final registrado no `todo.md`;
- commit criado e branch enviada ao remoto somente depois do GO final.

## SonarQube

- **Fonte do baseline:** SonarQube Docker local `http://localhost:9000`;
- **Projeto:** `simtr-hub-local`;
- **Pacote autorizado:** nenhum; `sonar/` estava ausente;
- **Baseline:** análise `faca645b-5b1c-4e6c-9ee9-c19d9da04d43`;
- **Estado técnico inicial:** `NON_COMPLIANT`, com `ContinuarAjustes` registrado;
- **Checkpoint final:** análise `aa8083cf-27f1-4b54-80c2-96efd1b75ffe`, com 390 testes,
  cobertura de 87,8%, duplicação de 3,7%, zero issue severa e cinco `java:S1185` LOW aceitas
  excepcionalmente; situação técnica `NON_COMPLIANT` e Quality Gate `ERROR` exclusivamente por
  essas cinco issues;
- **Credencial:** somente `SONAR_TOKEN` herdado na memória do processo.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| Testes artificiais elevarem cobertura sem provar comportamento | Alto | exigir asserção de resultado, erro ou telemetria em cada cenário |
| Generalização apagar fronteiras de domínio/borda | Alto | proibir extração entre domínios e exigir C1/C2 para produção |
| Refatoração alterar enum, mensagem, causa ou accessor | Alto | caracterização antes da extração e comparação de API |
| Cobertura cair ao reduzir linhas duplicadas | Médio | verificar JaCoCo e Sonar após cada incremento coerente |
| Buscar margem com mudanças desnecessárias | Médio | parar no primeiro checkpoint que satisfizer 85%/5% |
| Alterar observabilidade para cobrir branch | Alto | somente testar sinais existentes; mudança interrompe e exige checkpoint |
| Token ou estado local entrar no Git | Alto | inspeção de staged; nunca adicionar `target/`, `.codex/.state/` ou token |
| Avisos Quarkus desviarem o escopo | Médio | registrar como fora de escopo e abrir feature própria se desejado |

## GO necessário

`ContinuarAjustes` resolveu apenas a decisão do baseline `NON_COMPLIANT`. O GO humano C0 já
registrado autoriza os incrementos de cobertura em `src/test`. C1 e, se necessário, C2 continuam
obrigatórios antes de qualquer deduplicação em produção. Depois de CMET comprovar cobertura e
duplicação dentro dos limites, o usuário registra o GO final antes do commit e do push.
