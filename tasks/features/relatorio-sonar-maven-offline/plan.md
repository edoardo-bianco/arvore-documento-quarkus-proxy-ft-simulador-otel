# Plano: documentar geração de pacote SonarQube via Maven

## Intenção

Fornecer um roteiro seguro e reproduzível para executar a análise SonarQube de um projeto Maven na
máquina de trabalho, exportar as evidências do servidor e transportar o pacote para a pasta
`sonar/` deste repositório quando o agente não tiver acesso ao servidor de origem.

## Escopo

- criar um guia Markdown em `doc/sonar/` com o fluxo completo de análise, exportação e transporte;
- explicar o uso de `analisar-sonarqube.ps1` e `exportar-relatorios-sonarqube.ps1` em outro projeto
  Maven, incluindo parâmetros, pré-requisitos e verificações;
- incluir a alternativa Maven manual e cuidados para não expor tokens;
- documentar que o ZIP precisa ser extraído em um diretório dentro de `sonar/` antes de ser usado
  como `OfflineReportPath`.

## Fora de escopo

- alterar scripts PowerShell, `pom.xml`, hooks, código ou testes;
- executar Maven, acessar um SonarQube ou gerar um pacote real nesta sessão;
- copiar dados internos para `sonar/` ou versioná-los;
- atualizar formatos derivados `.ppt`, `.pptx`, `.pdf` ou `.html`.

## Contexto verificado

- arquitetura consolidada e índice de ADRs lidos;
- ADRs aplicáveis: nenhum, pois não há mudança de arquitetura, contrato ou comportamento;
- código, contratos e testes inspecionados: `analisar-sonarqube.ps1`,
  `exportar-relatorios-sonarqube.ps1`, `.codex/hooks/SonarQuality.psm1`, `pom.xml`,
  `test/powershell/AnalisarSonarQubeTest.ps1` e
  `test/powershell/ExportarRelatoriosSonarQubeTest.ps1`;
- documentação relacionada inspecionada: `doc/sonar/sonar-quebe-configuração.md` e
  `doc/sonar/exportacao-offline-sonarqube.md`;
- divergências: nenhuma identificada;
- restrição relevante: publicar a análise e exportar o pacote são etapas diferentes, com
  permissões de token diferentes.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Nenhum contrato público ou externo será alterado | Não |
| Arquitetura | Nenhuma responsabilidade, porta, dependência ou ADR será alterado | Não |
| Segurança | Apenas orientação documental para manter tokens fora de arquivos, argumentos e histórico | Não |
| Comportamento observável | Nenhum log, span, métrica, configuração ou política será alterado | Não |

## Tarefas

### Task 1 — Criar o guia operacional

**Descrição:** criar um documento curto, autocontido e ligado aos guias detalhados existentes,
permitindo que a análise e a exportação sejam executadas na máquina que possui acesso ao
SonarQube.

**Critérios de aceitação:**

- o fluxo distingue análise Maven, espera da análise no servidor, exportação Web API e transporte;
- os exemplos usam placeholders para projeto e URL e não contêm credenciais;
- o documento explica como carregar e remover tokens somente na sessão do PowerShell 7.1+ e do
  Windows PowerShell 5.1;
- o documento indica os arquivos mínimos esperados, a conferência de revisão e a extração do ZIP
  para `sonar/<pacote>`;
- o documento referencia os scripts e guias detalhados existentes sem duplicar toda a referência
  operacional.

**Verificação:**

- revisar comandos e links contra os scripts atuais;
- executar `git diff --check` e inspecionar o diff final;
- não executar Maven ou checkpoint Sonar, pois o fingerprint executável não será alterado.

**Dependências:** nenhuma.

**Arquivos prováveis:**

- `doc/sonar/gerar-relatorio-sonarqube-via-maven.md`;
- `tasks/features/relatorio-sonar-maven-offline/plan.md`;
- `tasks/features/relatorio-sonar-maven-offline/todo.md`.

### Checkpoint — Revisão documental final

- confirmar que nenhum token, URL interna ou dado de pacote foi incluído;
- confirmar que somente Markdown foi alterado;
- confirmar que o diretório extraído, e não o ZIP, é passado a `OfflineReportPath`.

## SonarQube

- fonte do baseline: não aplicável a uma mudança exclusivamente documental;
- pacote autorizado: nenhum; `sonar/` não será inspecionado nem alterado;
- checkpoint esperado: dispensado conforme `AGENTS.md`.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| Expor token no comando, arquivo ou histórico | Alto | usar leitura mascarada e variável de ambiente somente do processo |
| Exportar uma análise diferente da revisão entregue | Alto | conferir branch, revisão local e `latest-analysis.json` antes de transportar |
| Usar token de análise para consultar a Web API | Médio | documentar tokens e permissões separadamente |
| Copiar somente o ZIP e tentar usá-lo diretamente | Médio | exigir extração para um diretório dentro de `sonar/` |
| Web API variar entre versões do SonarQube | Médio | orientar conferência em `<SONAR_URL>/web_api` e preservar o catálogo exportado |

## GO necessário

Não haverá alteração de produção. O GO de produção não se aplica a este escopo exclusivamente
documental; qualquer expansão para scripts, build, hooks, código ou testes exigirá atualização do
plano, escolha do baseline e GO humano antes da primeira alteração executável.
