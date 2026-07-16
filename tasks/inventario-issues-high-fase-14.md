# Inventário de issues HIGH — Fase 14

Status: Task 14.1 concluída; piloto 14.2 validado localmente.

## Baseline

A consulta autenticada ao SonarQube local identificou 189 issues com impacto HIGH:

| Regra | Quantidade | Tratamento previsto |
|---|---:|---|
| `java:S1192` — literais duplicados | 169 | constantes privadas locais, por família |
| `java:S1948` — campo de exceção não serializável | 9 | caracterização antes de alteração |
| `java:S1186` — método vazio | 9 | somente testes/fixtures, após revisão |
| `java:S3776` — complexidade cognitiva | 2 | refatoração incremental, sem mudança de fluxo |

Os maiores agrupamentos S1192 estão em `DossieProdutoResource` (19), `ChecklistMtrAdapter` (10), `DocumentoDossieProdutoMtrAdapter` (9), `CriacaoDossieProdutoMtrAdapter` (10), além das famílias de observabilidade e recursos.

## Verificações

- `mvn -q clean test`: aprovado (exit code 0).
- `mvn -q -Dtest=ArchUnitProgressivoTest test`: aprovado (exit code 0).
- Piloto `GestaoDocumentoObservabilidade`: `mvn -q -Dtest=GestaoDocumentoObservabilidadeTest test` aprovado (exit code 0).
- `git diff --check`: aprovado; somente avisos normais de conversão LF/CRLF.
- Nenhum token do Sonar foi persistido em arquivo ou configuração.

## Primeiro piloto S1192

Foram extraídas constantes `private static final` apenas para chaves e valores repetidos dos três eventos de observabilidade de `GestaoDocumentoObservabilidade`. Os valores emitidos (`camada`, `application`, `componente`, `operacao` e seus conteúdos) permanecem idênticos. Não houve alteração de endpoint, contrato, fluxo, tratamento de erro ou telemetria.

