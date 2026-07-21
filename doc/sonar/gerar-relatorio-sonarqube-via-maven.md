# Gerar e transportar um relatório SonarQube de um projeto Maven

Este roteiro mostra como executar a análise na máquina de trabalho que possui acesso ao
SonarQube, exportar um pacote offline e colocá-lo na pasta `sonar/` deste repositório.

## Entenda as duas etapas

O SonarScanner Maven **não gera sozinho um relatório portátil completo**. Ele compila, testa e
publica a análise no servidor. Depois que o Compute Engine concluir essa análise, outro script
consulta a Web API e cria o pacote que poderá ser transportado.

| Etapa | Script | Resultado |
|---|---|---|
| Compilar, testar e publicar | [`analisar-sonarqube.ps1`](../../analisar-sonarqube.ps1) | Análise enviada ao SonarQube pelo Maven |
| Exportar a análise concluída | [`exportar-relatorios-sonarqube.ps1`](../../exportar-relatorios-sonarqube.ps1) | Diretório e ZIP com manifesto, issues e evidências de duplicação |

Não use um profile Maven para a exportação. O segundo script acessa somente a Web API do
SonarQube.

O pacote não é um backup completo do servidor: ele preserva as issues e os dados de duplicação
necessários à análise offline, mas não comprova cobertura atual nem Quality Gate aprovado.

## Pré-requisitos na máquina de trabalho

- PowerShell 7.1 ou superior, ou Windows PowerShell 5.1;
- JDK compatível com o projeto;
- Maven Wrapper no projeto ou `mvn` disponível no `PATH`;
- Git disponível para registrar a revisão local no pacote;
- acesso HTTPS ao SonarQube da empresa;
- permissão para executar análise e consultar o projeto;
- relatório XML JaCoCo gerado pelo build, quando a cobertura fizer parte da análise.

Copie os dois scripts para a raiz do projeto de trabalho, ao lado do `pom.xml`, e revise-os conforme
as políticas da empresa antes de executar. Essa localização é importante: o analisador procura o
`pom.xml` junto ao script, e o exportador usa essa pasta para registrar a revisão Git e criar a
saída padrão.

O script de análise deste repositório espera o JaCoCo em
`target/jacoco-report/jacoco.xml`. Confirme o caminho usado pelo projeto de trabalho. Se ele for
diferente, use a execução Maven manual descrita adiante ou adapte uma cópia local revisada do
script. Não apresente cobertura como válida quando o scanner não tiver importado o XML.

## 1. Defina os dados não sensíveis

Abra o PowerShell na raiz do projeto de trabalho:

```powershell
$projectKey = "chave-do-projeto-no-sonarqube"
$projectName = "nome-do-projeto"
$sonarUrl = "https://sonarqube.exemplo.interno"
```

Use a chave e a URL reais da instância. Não coloque usuário, senha ou token em `$sonarUrl`.

## 2. Carregue o token de análise somente na sessão

Para publicar, prefira um **Project analysis token** limitado ao projeto ou outra credencial com
`Execute Analysis`, conforme a política da instância. Nunca envie o token por chat, grave-o no
`pom.xml`, inclua-o em parâmetros Maven ou deixe-o no script.

No PowerShell 7.1 ou superior:

```powershell
$env:SONAR_TOKEN = Read-Host -Prompt "Cole o token de análise" -MaskInput
```

No Windows PowerShell 5.1:

```powershell
$tokenSeguro = Read-Host -Prompt "Cole o token de análise" -AsSecureString
$env:SONAR_TOKEN = [System.Net.NetworkCredential]::new("", $tokenSeguro).Password
Remove-Variable tokenSeguro
```

O texto de `-Prompt` é apenas a mensagem. Cole o token somente quando o PowerShell solicitar a
entrada.

## 3. Execute a análise pelo Maven

Com os scripts copiados para a raiz do projeto:

```powershell
./analisar-sonarqube.ps1 `
    -ProjectKey $projectKey `
    -ProjectName $projectName `
    -SonarUrl $sonarUrl
```

O script:

- verifica se o servidor está `UP`;
- prefere `mvnw.cmd` ou `mvnw` e usa o Maven global como fallback;
- executa `clean verify` sem pular os testes;
- chama `org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`;
- envia o caminho JaCoCo configurado sem colocar o token nos argumentos Maven.

Use `-MavenProfile` apenas se o profile realmente existir no `pom.xml` e for necessário ao build.
Para forçar o Maven do `PATH`, acrescente `-UseGlobalMaven`.

### Alternativa Maven manual

Se o projeto usa outro caminho JaCoCo, execute o scanner com o caminho real. Use `mvn` no exemplo
abaixo ou troque-o por `./mvnw.cmd`:

```powershell
$jacocoXml = "target/site/jacoco/jacoco.xml"

mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar `
    "-Dsonar.projectKey=$projectKey" `
    "-Dsonar.projectName=$projectName" `
    "-Dsonar.host.url=$sonarUrl" `
    "-Dsonar.coverage.jacoco.xmlReportPaths=$jacocoXml"

if ($LASTEXITCODE -ne 0) {
    throw "A análise Maven falhou com código $LASTEXITCODE."
}
```

O SonarScanner Maven lê `SONAR_TOKEN` do ambiente do processo. Não acrescente
`-Dsonar.token=<token>` à linha de comando.

## 4. Confirme a análise antes de exportar

Só exporte depois que a análise nova aparecer como concluída no dashboard do projeto. Confirme:

- projeto e branch corretos;
- data e horário da análise;
- revisão analisada, quando o servidor a informar;
- testes Maven aprovados;
- XML JaCoCo existente e importado pelo scanner;
- cobertura, quando esperada, presente no dashboard.

Para localizar o XML sem mostrar seu conteúdo:

```powershell
Get-ChildItem -Path target -Recurse -File -Filter jacoco.xml |
    Select-Object FullName, Length, LastWriteTime
```

Se a publicação terminou, mas o servidor ainda processa a tarefa, aguarde o Compute Engine. Uma
exportação antecipada pode capturar a análise anterior.

## 5. Troque para um token de leitura e exporte

Remova primeiro a credencial de análise:

```powershell
Remove-Item Env:SONAR_TOKEN -ErrorAction SilentlyContinue
```

Para a exportação, use um **User token** de uma conta com permissão `Browse` no projeto. Não presuma
que um Project analysis token pode consultar a Web API. Use o mesmo método mascarado da etapa 2
para preencher novamente `SONAR_TOKEN`.

Escolha um diretório novo para tornar o pacote fácil de localizar:

```powershell
$exportDir = Join-Path $PWD ("target/sonar-export/pacote-" + (Get-Date -Format "yyyyMMdd-HHmmss"))

./exportar-relatorios-sonarqube.ps1 `
    -ProjectKey $projectKey `
    -SonarUrl $sonarUrl `
    -OutputDirectory $exportDir
```

Se a edição do servidor suportar análise por branch, acrescente `-Branch "nome-da-branch"`. Antes,
confirme o parâmetro na documentação embutida da instância em `<SONAR_URL>/web_api`.

O exportador deixa o diretório em `$exportDir` e cria `$exportDir.zip`. Ele não sobrescreve um
diretório existente.

## 6. Valide o pacote antes de compartilhar

Confira as contagens e as revisões:

```powershell
$manifest = Get-Content -Raw (Join-Path $exportDir "manifest.json") | ConvertFrom-Json
$latest = Get-Content -Raw (Join-Path $exportDir "latest-analysis.json") | ConvertFrom-Json

[pscustomobject]@{
    ProjectKey = $manifest.projectKey
    Branch = $manifest.branch
    ExportedAtUtc = $manifest.exportedAtUtc
    LocalGitRevision = $manifest.localGitRevision
    SonarRevision = $latest.analyses[0].revision
    IssuesReported = $manifest.issueTotalReportedByServer
    IssuesExported = $manifest.issueExportedCount
}

if ($manifest.issueExportedCount -ne $manifest.issueTotalReportedByServer) {
    throw "A quantidade exportada difere do total informado pelo SonarQube."
}
```

O pacote deve conter, no mínimo, `issues.json` ou `issues.csv`. `manifest.json` e
`latest-analysis.json` são recomendados para identificar a origem. O exportador também preserva
JSONs brutos, métricas e intervalos de duplicação.

Se `LocalGitRevision` e `SonarRevision` forem diferentes ou estiverem ausentes, documente a
limitação; não presuma que o pacote corresponde ao código entregue.

Embora os scripts não gravem o token no pacote, trate o conteúdo como dado interno: ele contém URL
do servidor, nomes de projeto, paths, regras e mensagens de issues. Compartilhe somente pelo canal
corporativo autorizado. Não envie histórico do terminal, logs, capturas de tela, `.env` ou
credenciais junto com o ZIP.

Ao terminar:

```powershell
Remove-Item Env:SONAR_TOKEN -ErrorAction SilentlyContinue
```

## 7. Coloque o pacote na pasta `sonar/`

Transfira o ZIP para a máquina que contém este repositório por um canal aprovado. O baseline
offline aceita um **diretório**, não o ZIP diretamente. Extraia para um nome novo dentro de
`sonar/`:

```powershell
$sourceZip = "C:\caminho-aprovado\pacote-AAAAmmdd-HHMMSS.zip"
$destination = Join-Path $PWD "sonar/projeto-AAAAmmdd-HHMMSS"

if (Test-Path -LiteralPath $destination) {
    throw "O destino já existe: $destination"
}

Expand-Archive -LiteralPath $sourceZip -DestinationPath $destination

$hasIssues = (Test-Path -LiteralPath (Join-Path $destination "issues.json")) -or
    (Test-Path -LiteralPath (Join-Path $destination "issues.csv"))
if (-not $hasIssues) {
    throw "O pacote extraído não contém issues.json nem issues.csv na raiz."
}
```

Os arquivos devem ficar diretamente em `sonar/projeto-AAAAmmdd-HHMMSS/`, e não em uma segunda
pasta aninhada. Não edite o conteúdo depois da exportação: o pacote é uma evidência imutável de um
instante do servidor. A pasta `sonar/` é ignorada pelo Git e não deve ser adicionada ao commit.

A simples presença do pacote não escolhe a fonte do baseline. Para uma tarefa de código, o agente
deverá apresentar as opções exigidas pelo `AGENTS.md`: Sonar local, Sonar local mais o pacote ou,
quando o servidor estiver indisponível, somente o pacote offline.

Exemplo de baseline exclusivamente offline:

```powershell
./validar-checkpoint-sonarqube.ps1 `
    -InitializeBaseline `
    -OfflineOnlyBaseline `
    -OfflineReportPath "./sonar/projeto-AAAAmmdd-HHMMSS"
```

Esse modo permanece `UNVERIFIED`: não comprova cobertura, duplicação, ausência de issues novas nem
Quality Gate atual. Quando houver acesso ao Sonar local, o pacote pode ser associado a uma análise
nova omitindo `-OfflineOnlyBaseline`.

## Falhas comuns

| Sintoma | Verificação |
|---|---|
| `pom.xml` não encontrado | coloque `analisar-sonarqube.ps1` na raiz do projeto Maven |
| Maven não encontrado | use o Maven Wrapper ou instale/configure `mvn` no `PATH` |
| `401` ou `403` | confira tipo do token, validade e permissões da etapa atual |
| Cobertura ausente | confirme que `verify` gerou o XML e que o caminho informado é o correto |
| Exportação contém análise antiga | aguarde o Compute Engine e confira projeto, branch, data e revisão |
| Branch rejeitada pela API | consulte `<SONAR_URL>/web_api` e a edição do servidor |
| `OfflineReportPath` não encontrado | extraia o ZIP dentro de `sonar/` e aponte para o diretório |
| Pacote recusado | deixe `issues.json` ou `issues.csv` diretamente na raiz do diretório extraído |

## Referências do repositório

- [Configuração local do SonarQube](sonar-quebe-configuração.md)
- [Exportação offline de issues e duplicações](exportacao-offline-sonarqube.md)
- [Script de análise Maven](../../analisar-sonarqube.ps1)
- [Script de exportação Web API](../../exportar-relatorios-sonarqube.ps1)
