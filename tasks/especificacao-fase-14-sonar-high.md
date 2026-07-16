# Especificacao: Fase 14 - remediacao segura de issues HIGH do SonarQube

## Status

- **Estado:** rascunho aguardando C14-SPEC
- **Branch:** `refactor/sonar-high-fase-14-baseline`
- **Origem:** projeto `simtr-hub-local`, analise SonarQube local 26.7
- **Producao:** bloqueada ate C14-SPEC, C14-PLAN, C14-TASKS e aceite de cada lote

## Premissas explicitas

1. A prioridade e reduzir os issues com impacto HIGH sem alterar comportamento observavel.
2. A analise possui 189 issues HIGH abertos/confirmados: 169 `java:S1192`, 9 `java:S1948`, 9
   `java:S1186` e 2 `java:S3776`.
3. `S1192` trata strings literais repetidas e nao e a mesma metrica dos clones CPD da Fase 13.
4. A remediacao sera feita em lotes pequenos, com caracterizacao e GO humano entre lotes.
5. Nenhum issue sera marcado como resolvido no Sonar apenas para reduzir contador.

## Objetivo

Reduzir os issues HIGH de manutencao mantendo paths, JSON, validacoes, erros, fault tolerance,
simulador, spans, atributos, logs, nulabilidade, ordem de efeitos e fronteiras DDD equivalentes.

## Baseline observada

| Regra | Quantidade | Escopo |
|---|---:|---|
| `java:S1192` | 169 | Resources, adapters MTR e observabilidade |
| `java:S1948` | 9 | excecoes MTR e `MtrRestClientException` |
| `java:S1186` | 9 | handlers e fixtures de testes |
| `java:S3776` | 2 | observabilidade e adapter MTR de documento |

## Estrategia para revisao

### Lote A - S1192 local

Substituir literais repetidos por constantes `private static final` dentro da propria classe,
mantendo os valores byte a byte. Nao criar catalogo compartilhado entre dominios, Resources, MTR
e simulador. Priorizar uma classe por vez e validar logs, spans e atributos.

### Lote B - S1186 intencional

Revisar os nove metodos vazios. Nos fixtures de ArchUnit e handlers de log que precisam ser no-op,
adicionar comentario local explicando o ciclo de vida intencional. Nao lancar excecao nem alterar
ordem de setup/teardown. Se houver comportamento faltante, criar teste antes de corrigi-lo.

### Lote C - S3776

Extrair somente metodos privados locais para separar captura de atributos, log de inicio, sucesso e
falha. Preservar ordem dos efeitos, nomes de spans, chaves de log, valores e nulos.

### Lote D - S1948

Caracterizar primeiro se excecoes sao serializadas fora do processo. Se a serializacao for
observavel, tornar serializavel todo o grafo de erro e testar round-trip, preservando `status`,
`erro`, `tipoErro` e mensagens. Usar `transient` somente com evidencia de que perder `erro()` apos
serializacao nao quebra contrato. Alteracoes em `arquitetura.excecao.dto` exigem aprovacao
arquitetural explicita.

## Comandos de verificacao

```powershell
git rev-parse --abbrev-ref HEAD
git status -sb
mvn -q -Dtest=ArchUnitProgressivoTest test
mvn -q clean test
$env:SONAR_TOKEN = Read-Host "Token do SonarQube" -MaskInput
mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar `
  "-Dsonar.projectKey=simtr-hub-local" `
  "-Dsonar.projectName=simtr-hub-local" `
  "-Dsonar.host.url=http://localhost:9000" `
  "-Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-report/jacoco.xml"
Remove-Item Env:SONAR_TOKEN
git diff --check
```

## Estrutura, estilo e testes

- producao permanece em `src/main/java` organizada por dominio e borda;
- testes permanecem em `src/test/java` sem remocao, ignoracao ou enfraquecimento;
- governanca e evidencia ficam em `tasks/`;
- constantes devem ser locais e semanticas, por exemplo `private static final String CAMADA =
  "camada";`;
- sinais de observabilidade, contratos HTTP/MTR, simulador e erros sao contratos de compatibilidade;
- cada lote exige testes focados, ArchUnit quando aplicavel, suite limpa e nova analise Sonar.

## Limites

### Sempre fazer

- preservar contratos HTTP/JSON, MTR, simulador, FT, erros e observabilidade;
- registrar issue keys, arquivos, linhas e evidencia antes/depois;
- manter credenciais somente em memoria e revoga-las depois da analise.

### Pedir aprovacao antes

- alterar `arquitetura.excecao.dto` ou contrato compartilhado;
- mudar strings de spans, atributos, logs, paths ou mensagens de erro;
- adicionar dependencia, alterar configuracao ou tocar mais de cinco arquivos por lote;
- marcar issue como falso positivo ou aceitar queda de cobertura.

### Nunca fazer

- compartilhar DTOs entre REST, MTR e simulador;
- criar utilitario global apenas para satisfazer S1192;
- usar `transient` para esconder perda de erro sem teste;
- remover testes, alterar status de issue sem evidencia ou gravar tokens no repositorio.

## Criterios de sucesso

- issues HIGH resolvidos em lotes rastreaveis ou justificados com aprovacao humana;
- `mvn -q clean test` e ArchUnit verdes;
- cobertura nao abaixo de 80,0% geral, 88,4% de linhas e 60,0% de branches;
- complexidade cognitiva dos dois S3776 dentro do limite sem deslocamento indevido;
- contratos, erros, FT, simulador e observabilidade equivalentes;
- nenhuma fronteira DDD ou independencia de DTO alterada;
- token ausente de arquivos, diff e logs versionados.

## Questoes para C14-SPEC

- confirmar ordem Lote A, B, C, D;
- confirmar que correcoes test-only S1186 entram na prioridade HIGH;
- decidir, com teste, serializacao completa versus outra estrategia para S1948;
- confirmar limite de cinco arquivos por lote e GO humano entre lotes.
