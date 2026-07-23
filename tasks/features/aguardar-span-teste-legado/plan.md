# Plano: aguardar span no teste legado

## Intenção

Eliminar a flutuação do teste de contrato do processo parametrizado legado quando o span HTTP
assíncrono ainda não foi finalizado e exportado no momento da asserção, preservando integralmente
o comportamento de produção e o contrato observável existente.

## Escopo

- substituir a busca imediata do span do REST Client por uma espera limitada e orientada por
  condição no teste afetado;
- produzir mensagem diagnóstica com os nomes dos spans recebidos quando o timeout expirar;
- validar o teste focado, a suíte Maven e o checkpoint SonarQube aplicável.

## Fora de escopo

- alterar código em `src/main`;
- alterar nomes, atributos, eventos ou relacionamentos de spans;
- corrigir a propagação de `Span.current()` entre os filtros de request e response;
- adicionar dependências, paralelismo de testes ou esperas fixas longas;
- alterar outros testes de observabilidade.

## Contexto verificado

- arquitetura consolidada lida;
- ADR aplicável: ADR-0006 — Compatibilidade, observabilidade e testes;
- código, contratos e testes inspecionados:
  `ConsultarProcessoParametrizadoLegadoContractTest`, `RestClientObservabilityFilter`,
  `ProcessoParametrizadoMtrAdapter`, `ParametrizacaoProcessoClient`, configuração de testes e
  `pom.xml`;
- evidência de RED: falha da esteira corporativa na linha 134 com `NoSuchElementException`;
- reprodução local: teste focado e suíte completa passam; logging adicional comprovou que
  `rest_client.class` está em um span HTTP `SERVER` finalizado depois dos spans internos;
- divergências ou suposições: o comando e o ambiente completos da esteira corporativa não estão
  disponíveis neste checkout; a correção trata somente a espera assíncrona observada.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Nenhuma mudança de path, status, JSON, validação ou DTO | não |
| Arquitetura | Nenhuma mudança de package, dependência ou responsabilidade | não |
| Segurança | Nenhuma mudança de entrada, autenticação ou dado sensível | não |
| Comportamento observável | Spans permanecem inalterados; muda apenas a sincronização da asserção de teste | não |

## Tarefas

### Task 1 — Inicializar baseline técnico

**Descrição:** congelar o baseline SonarQube local antes da primeira alteração do teste.

**Critérios de aceitação:**

- baseline inicializado pelo script oficial do repositório;
- resultado e eventual indisponibilidade registrados no checklist.

**Verificação:**

- `./validar-checkpoint-sonarqube.ps1 -InitializeBaseline`.

**Dependências:** GO humano no checkpoint C0.

**Arquivos prováveis:** nenhum arquivo fonte.

### Task 2 — Aguardar exportação do span no teste

**Descrição:** implementar no próprio teste um polling limitado por tempo, que retorna assim que o
span esperado estiver disponível e falha com diagnóstico útil ao atingir o limite.

**Critérios de aceitação:**

- a busca por `rest_client.class=ParametrizacaoProcessoClient` não usa mais
  `findFirst().orElseThrow()` imediatamente;
- a espera termina imediatamente quando o span já existe;
- o timeout máximo é de cinco segundos e a falha informa os nomes dos spans recebidos;
- nenhum arquivo de produção ou contrato observável é alterado.

**Verificação:**

- teste focado `ConsultarProcessoParametrizadoLegadoContractTest`;
- suíte `mvn -q test`;
- inspeção do diff e checkpoint SonarQube do incremento.

**Dependências:** Task 1 concluída.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/arvoredocumento/caracterizacao/ConsultarProcessoParametrizadoLegadoContractTest.java`.

### Checkpoint — Encerramento técnico

- teste focado e suíte completos sem falhas;
- checkpoint SonarQube registrado;
- nenhuma mudança fora do escopo no diff;
- decisão humana solicitada somente se o checkpoint resultar em `NON_COMPLIANT`.

## SonarQube

- fonte do baseline: SonarQube Docker local;
- pacote autorizado: nenhum — o diretório `sonar/` não existe;
- checkpoint esperado: após o incremento coerente da Task 2.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| Apenas mascarar uma demora ilimitada | médio | timeout curto e mensagem diagnóstica explícita |
| Aumentar desnecessariamente a duração da suíte | baixo | polling retorna imediatamente quando a condição é atendida |
| Alterar o contrato observável ao corrigir o teste | alto | escopo proíbe mudanças em `src/main` e nos atributos/eventos |
| Falha depender de configuração não disponível localmente | médio | registrar a limitação e manter a mudança restrita à corrida comprovada |

## GO necessário

Nenhuma alteração no teste começa antes do GO humano registrado no `todo.md`.
