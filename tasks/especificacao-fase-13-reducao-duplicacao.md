# Especificacao: Fase 13 - reducao segura de duplicacao

## Status

- **Estado:** aprovada no C13-SPEC
- **Abertura autorizada:** 2026-07-16, somente para branch e especificacao
- **Branch:** `refactor/ddd-fase-13-baseline`
- **Gates:** C13-SPEC, C13-PLAN e C13-TASKS em GO desde 2026-07-16
- **Producao:** bloqueada ate aprovacao das tasks e do piloto inventariado

## Premissas

1. O primeiro recorte cobre somente `src/main/java`.
2. O objetivo nao e 0%; duplicacao que preserve fronteiras pode permanecer.
3. O primeiro piloto remove um unico bloco dentro do mesmo dominio e da mesma borda.
4. Contratos e sinais observaveis permanecem congelados.

## Decisoes aprovadas no C13-SPEC

- o inventario detalhado usara User Token com permissao Browse, mantido somente em memoria;
- a fase executara um unico piloto antes de novo checkpoint humano;
- cobertura e criterio de nao regressao, nao objetivo adicional desta refatoracao.

## Objetivo

Reduzir duplicacao removivel sem transformar semelhanca textual em compartilhamento
arquitetural. O resultado deve facilitar manutencao sem criar dependencia entre dominios,
reutilizar DTOs entre bordas ou deslocar complexidade para abstracao generica.

Sucesso significa eliminar ao menos um bloco duplicado real e demonstrar, por testes, ArchUnit e
SonarQube, que comportamento e fronteiras permaneceram equivalentes.

## Baseline

Analise SonarQube de 2026-07-16 para `simtr-hub-local`:

| Metrica | Baseline |
|---|---:|
| Quality Gate | OK |
| Cobertura geral / linhas / branches | 80,0% / 88,4% / 60,0% |
| Densidade de duplicacao | 6,0% |
| Linhas / blocos duplicados | 735 / 26 |
| Complexidade ciclomatica / cognitiva | 1.352 / 837 |
| Linhas de codigo | 10.668 |

O Quality Gate atual verifica somente novas violacoes e nao substitui estes criterios.

## Stack tecnico e comandos

Java 25, Quarkus 3.33.2.1, Maven, JUnit 5, ArchUnit 1.4.2, JaCoCo, SonarQube local 26.7 e
SonarScanner for Maven 5.5.0.6356. Nenhuma dependencia nova esta prevista.

```powershell
git rev-parse --abbrev-ref HEAD
git status -sb
mvn -q clean test

$env:SONAR_TOKEN = Read-Host "Token do SonarQube" -MaskInput
mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar `
  "-Dsonar.projectKey=simtr-hub-local" `
  "-Dsonar.projectName=simtr-hub-local" `
  "-Dsonar.host.url=http://localhost:9000" `
  "-Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-report/jacoco.xml"
Remove-Item Env:SONAR_TOKEN

git diff --check
git status --short
```

## Estrutura do projeto

- `src/main/java/<dominio>/dominio`: modelos e falhas do dominio.
- `src/main/java/<dominio>/aplicacao`: portas e casos de uso.
- `src/main/java/<dominio>/adaptador`: REST, MTR, simulador e configuracao CDI.
- `src/main/java/.../arquitetura`: mecanismos tecnicos sem regra de negocio.
- `src/test/java`: caracterizacao, contratos, unitarios e ArchUnit.
- `tasks` e `doc`: governanca, evidencias e decisao arquitetural.

## Estilo de codigo

Manter funcoes pequenas, nomes do dominio, retornos antecipados para nulos contratuais e
transformacao explicita entre tipos. Exemplo existente:

```java
static GestaoDocumentoCredencialContainerResponse paraResposta(
        CredencialContainer credencial
) {
    if (credencial == null) {
        return null;
    }
    return new GestaoDocumentoCredencialContainerResponse(
            credencial.sas(),
            credencial.validade(),
            credencial.urlStorage(),
            credencial.nomeContainer()
    );
}
```

Helpers de passagem, hierarquias genericas e abstracoes criadas apenas para reduzir a metrica sao
proibidos. Uma extracao deve nomear um conceito real e diminuir os conceitos do fluxo.

## Classificacao obrigatoria

Cada um dos 26 blocos recebe uma classificacao antes de qualquer edicao:

1. **Removivel local:** mesma responsabilidade, dominio, borda e evolucao.
2. **Intencional de contrato:** DTO ou mapeamento independente de REST, MTR ou simulador.
3. **Intencional de dominio:** ownership ou evolucao independentes.
4. **Tecnica compartilhavel:** sem tipos/regras de negocio e com tres consumidores reais.
5. **Duvidosa:** ownership nao comprovado; permanece inalterada.

## Estrategia de testes

1. Capturar pares de arquivos e linhas reportados pelo SonarQube.
2. Selecionar um bloco classificado como removivel local.
3. Identificar ou fortalecer teste de caracterizacao antes da mudanca.
4. Aplicar refatoracao de no maximo cinco arquivos nao mecanicos.
5. Executar testes focados, ArchUnit e `mvn -q clean test`.
6. Reexecutar SonarQube e comparar com a baseline.

Testes existentes nao serao alterados para acomodar mudanca de comportamento.

## Limites

### Sempre fazer

- preservar HTTP, JSON, validacoes, nulos, wire MTR, erros e fault tolerance;
- preservar simulador, properties, CDI, spans, atributos e logs;
- respeitar ArchUnit e registrar metricas antes/depois.

### Pedir aprovacao antes

- extrair para `arquitetura` ou atravessar dominio/borda;
- alterar `arquitetura.excecao.dto`, teste contratual, dependencia ou configuracao;
- tocar mais de cinco arquivos nao mecanicos;
- aceitar aumento de complexidade ou queda de cobertura.

### Nunca fazer

- compartilhar DTO REST, MTR ou simulador;
- criar adapter, mapper, service ou modelo generico entre dominios;
- alterar endpoint, contrato, simulador, fault tolerance ou observabilidade;
- implementar workflow, upload, cache de SAS ou Quarkus Flow;
- gravar credencial no repositorio ou perseguir 0% de duplicacao.

## Criterios de sucesso

- os 26 blocos possuem arquivos, linhas, ownership e classificacao registrados;
- o piloto atua em um dominio e uma borda;
- `duplicated_lines < 735` e `duplicated_blocks < 26`;
- cobertura geral, de linhas e branches nao fica abaixo de 80,0%, 88,4% e 60,0%;
- complexidade cognitiva nao fica acima de 837; variacao ciclomatica exige justificativa;
- suite limpa, build e ArchUnit passam sem teste removido, ignorado ou enfraquecido;
- nenhuma dependencia nova atravessa dominios ou mistura bordas;
- o diff contem somente piloto, testes e governanca correspondente;
- um segundo bloco exige novo GO humano.

## Questoes para o plano e inventario

- os arquivos exatos do piloto permanecem indefinidos ate o inventario dos 26 blocos;
- um segundo bloco fica fora do primeiro piloto e exige nova decisao humana;
- qualquer necessidade de compartilhar mecanismo tecnico sera tratada como mudanca de escopo.
