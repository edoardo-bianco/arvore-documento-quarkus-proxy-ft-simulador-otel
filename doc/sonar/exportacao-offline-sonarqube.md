# Exportação offline de issues e duplicações do SonarQube

Este guia explica como usar
[`exportar-relatorios-sonarqube.ps1`](../../exportar-relatorios-sonarqube.ps1) para gerar um pacote
destinado a uma pessoa ou agente sem acesso ao servidor SonarQube.

O pacote preserva issues, métricas por arquivo, blocos e intervalos de linhas duplicadas, versão do
servidor, análise de referência, revisão Git local e catálogo da Web API.

## Quando usar

- O agente não consegue acessar a rede ou autenticar no SonarQube.
- O servidor pertence a outro ambiente.
- A análise precisa ser auditável contra uma revisão específica.
- Um CSV de issues não contém os detalhes necessários sobre duplicações.

O pacote representa um instante do servidor. Ele não substitui o código-fonte da mesma revisão e
não prova sozinho que uma issue foi corrigida.

## Compatibilidade da Web API

O script usa estes endpoints da Web API clássica:

- `GET api/issues/search`;
- `GET api/measures/component_tree`;
- `GET api/duplications/show`;
- `GET api/project_analyses/search`;
- `GET api/server/version`;
- `GET api/webservices/list`.

Como a Web API V2 substitui gradualmente a API clássica, confirme os endpoints e parâmetros na
documentação embutida da instância antes de executar contra outra versão:

```text
<SONAR_URL>/web_api
```

O próprio catálogo da Web API é incluído no pacote como `web-api-catalog.json`.

Fontes oficiais:

- <https://docs.sonarsource.com/sonarqube-server/extension-guide/web-api>
- <https://docs.sonarsource.com/sonarqube-community-build/user-guide/code-metrics/metrics-definition>
- <https://docs.sonarsource.com/sonarqube-server/user-guide/managing-tokens>

## Pré-requisitos

- Windows PowerShell 5.1 ou PowerShell 7.1 ou superior;
- acesso HTTP ou HTTPS ao SonarQube;
- Git disponível para registrar a revisão local, quando possível;
- User token de uma conta com permissão `Browse` no projeto.

Não use o Project analysis token do scanner para esta exportação. Não use uma conta administradora
quando uma conta somente com `Browse` for suficiente.

## Relação com a análise Maven

O exportador consulta uma análise que já existe no servidor. Ele não executa Maven, não ativa
profiles, não gera cobertura JaCoCo e não publica uma nova análise.

Quando precisar de dados atualizados:

1. Execute `./analisar-sonarqube.ps1` diretamente, conforme
   [`sonar-quebe-configuração.md`](sonar-quebe-configuração.md#executar-diretamente-ou-usar-um-profile-maven).
2. Confirme que a análise de `simtr-hub-local` terminou e aparece no dashboard.
3. Verifique a geração e a importação do JaCoCo conforme o guia de configuração.
4. Troque o Project analysis token pelo User token com `Browse` somente na memória da sessão.
5. Execute `./exportar-relatorios-sonarqube.ps1`.

Não use um profile Maven para a etapa de exportação, pois ela utiliza apenas a Web API do
SonarQube.

## Configurar o token sem gravá-lo no histórico

Abra o PowerShell na raiz do repositório e confirme a versão:

```powershell
$PSVersionTable.PSVersion
```

No PowerShell 7.1 ou superior, use `-MaskInput`:

```powershell
$env:SONAR_TOKEN = Read-Host -Prompt "Cole o User token com Browse" -MaskInput
```

O valor de `-Prompt` é apenas uma mensagem. Não coloque o token dentro desse texto; cole-o
somente quando o PowerShell solicitar a entrada.

No Windows PowerShell 5.1, que não possui `-MaskInput`, use:

```powershell
$tokenSeguro = Read-Host -Prompt "Cole o User token com Browse" -AsSecureString
$env:SONAR_TOKEN = [System.Net.NetworkCredential]::new("", $tokenSeguro).Password
Remove-Variable tokenSeguro
```

O script envia o token no header `Authorization: Bearer`. Ele não aceita token por parâmetro, não o
coloca na URL e não o grava nos arquivos. A documentação oficial recomenda um token do tipo
**User** no header Bearer para a Web API; use uma conta limitada à permissão `Browse` no projeto.

## Executar com os valores padrão

Para o projeto `simtr-hub-local` no SonarQube local:

```powershell
./exportar-relatorios-sonarqube.ps1
```

O diretório padrão é criado na forma:

```text
target/sonar-export/simtr-hub-local-AAAAmmdd-HHMMSS/
```

Ao final, o script cria também um ZIP ao lado desse diretório. Um caminho existente não é
sobrescrito.

## Parâmetros

| Parâmetro | Padrão | Finalidade |
|---|---|---|
| `ProjectKey` | `simtr-hub-local` | Chave do projeto no SonarQube |
| `SonarUrl` | `http://localhost:9000` | URL absoluta HTTP ou HTTPS |
| `Branch` | vazio | Branch analisada, quando suportada |
| `OutputDirectory` | timestamp em `target/sonar-export` | Diretório novo de saída |
| `IncludeResolved` | desativado | Inclui também issues resolvidas |
| `NoArchive` | desativado | Mantém somente o diretório, útil para diagnóstico e testes |

URLs contendo usuário ou senha são rejeitadas. Para um servidor remoto em HTTP, o script emite um
warning porque o token não estará protegido por TLS.

## Exemplos

Outro projeto ou servidor:

```powershell
./exportar-relatorios-sonarqube.ps1 `
    -ProjectKey "outro-projeto" `
    -SonarUrl "https://sonarqube.exemplo.interno"
```

Branch específica:

```powershell
./exportar-relatorios-sonarqube.ps1 `
    -ProjectKey "simtr-hub-local" `
    -Branch "develop"
```

Use `Branch` somente quando a edição e a versão do servidor documentarem o parâmetro no
`<SONAR_URL>/web_api`.

Incluir issues resolvidas:

```powershell
./exportar-relatorios-sonarqube.ps1 -IncludeResolved
```

Diretório explícito sem gerar ZIP:

```powershell
./exportar-relatorios-sonarqube.ps1 `
    -OutputDirectory "C:\temp\sonar-export-simtr-hub-local" `
    -NoArchive
```

## O que o script exporta

| Arquivo | Conteúdo |
|---|---|
| `manifest.json` | Projeto, servidor, versão, branch, data, revisão e contagens |
| `latest-analysis.json` | Metadados da análise mais recente |
| `web-api-catalog.json` | Catálogo dos endpoints da versão consultada |
| `issues.json` | Issues completas; fonte primária |
| `issues.csv` | Visão tabular auxiliar das issues |
| `issues-page-*.json` | Respostas brutas paginadas |
| `duplication-files.json` | Métricas de duplicação por arquivo |
| `duplication-files.csv` | Visão tabular das métricas |
| `duplication-measures-page-*.json` | Respostas brutas das métricas |
| `duplication-blocks.json` | Respostas consolidadas dos grupos de clones |
| `duplication-lines.csv` | Arquivos e intervalos exatos de cada bloco |
| `duplications-file-*.json` | Resposta bruta por arquivo duplicado |

O script interrompe a execução se a quantidade paginada de issues ou componentes não corresponder
ao total informado pelo servidor.

## Verificar o pacote antes de compartilhar

- [ ] `issueExportedCount` é igual a `issueTotalReportedByServer`.
- [ ] O código-fonte está na revisão analisada ou a divergência está documentada.
- [ ] Os JSONs brutos foram preservados.
- [ ] `duplication-lines.csv` contém arquivos e intervalos de linhas.
- [ ] O ZIP não contém token, header `Authorization`, cookies, `.env` ou logs da sessão.
- [ ] Paths ausentes não foram automaticamente declarados resolvidos.

O campo `localGitRevision` registra o commit aberto durante a exportação. Compare-o com
`latest-analysis.json`; não presuma que as revisões são iguais.

## Remover o token da sessão

Depois da exportação:

```powershell
Remove-Item Env:SONAR_TOKEN
```

Se o token tiver sido escrito diretamente em um comando, usado como valor de `-Prompt` ou exibido
no terminal, revogue-o imediatamente em `My Account > Security` e gere outro. `Clear-History`
limpa a sessão atual, mas o PSReadLine também pode manter histórico em arquivo; consulte o caminho
e remova dele a linha exposta:

```powershell
Clear-History
(Get-PSReadLineOption).HistorySavePath
```

Não compartilhe transcrições do terminal, capturas de tela ou o arquivo de histórico junto com o
ZIP. O procedimento completo também está em
[`sonar-quebe-configuração.md`](sonar-quebe-configuração.md#uso-recomendado-no-powershell).

## Teste local sem servidor

O teste usa respostas simuladas e não acessa rede nem SonarQube:

```powershell
./test/powershell/ExportarRelatoriosSonarQubeTest.ps1
```

Ele valida sintaxe, autenticação somente em header, paginação, arquivos gerados, contagens,
intervalos de linhas duplicadas e ausência da credencial no pacote.

## Prompt sugerido para o agente

Anexe o ZIP e o código-fonte correspondente:

```text
Analise o pacote SonarQube offline junto com este código-fonte.
Use manifest.json e latest-analysis.json para verificar projeto, data, branch e revisão.
Trate issues.json e os JSONs brutos como fontes primárias; os CSVs são auxiliares.
Reconcilie cada issue por key, rule, component e intervalo de linhas.
Agrupe duplication-lines.csv por group e compare todos os blocos envolvidos.
Não declare resolvido um path ausente sem verificar revisão, histórico e referências.
Classifique cada caso como resolvido, acionável, falso positivo provável, obsoleto ou inconclusivo.
Não altere o código antes de apresentar o inventário e receber autorização.
```

## Falhas comuns

- `401` ou `403`: token ausente, expirado ou sem `Browse`.
- `404`: confirme endpoint e parâmetros no `<SONAR_URL>/web_api`.
- exportação incompleta: preserve as páginas brutas e subdivida a consulta pelos filtros
  documentados pela versão.
- duplicação sem linhas: confirme `duplicated_lines > 0` e acesso ao componente.
- diretório já existe: informe outro `OutputDirectory` ou use o diretório timestamp padrão.
- revisão ausente: classifique a comparação como inconclusiva quando não for possível provar que
  o código corresponde à análise.
