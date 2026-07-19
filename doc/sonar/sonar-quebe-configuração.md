# Configuração local do SonarQube

Este guia descreve o uso do SonarQube Community Build local em Docker com o IntelliJ e com o
scanner Maven deste projeto.

## Gerar tokens

A geração é feita no próprio SonarQube local:

1. Confirme que o container está ativo e abra `http://localhost:9000`.
2. Entre com seu usuário.
3. Abra o menu do usuário e selecione `My Account > Security`, ou acesse diretamente
   `http://localhost:9000/account/security`.
4. Na área **Generate Tokens**, informe um nome que identifique o uso, por exemplo
   `simtr-hub-local-maven`.
5. Para executar `analisar-sonarqube.ps1`, selecione **Project analysis token** e associe-o ao
   projeto `simtr-hub-local`. Esse tipo é preferível porque limita a credencial a um único projeto.
6. Escolha uma expiração, quando essa opção estiver disponível, e clique em **Generate**.
7. Copie o valor imediatamente. Depois que a mensagem for fechada, o SonarQube não mostra o valor
   novamente.

Se a instalação não oferecer **Project analysis token**, gere um **User token** pertencente a um
usuário com permissão `Execute Analysis` no projeto. Não use um token de administrador quando uma
credencial com menos privilégios for suficiente.

Para o Connected Mode do IntelliJ, gere outro token com nome como `intellij-connected-mode` e
selecione **User token**. Tokens de análise de projeto ou globais não substituem o User token no
Connected Mode.

Documentação oficial:

- <https://docs.sonarsource.com/sonarqube-server/user-guide/managing-tokens>
- <https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-maven>

Não registre tokens neste documento, no Git, em scripts ou em parâmetros de linha de comando. Se
um token tiver sido exposto, abra novamente `My Account > Security`, revogue-o e gere outro.

## Configurar o SonarQube for IDE no IntelliJ

1. Instale o plugin **SonarQube for IDE**.
2. Abra `File > Settings > Tools > SonarQube for IDE`.
3. Crie uma conexão com estes valores:
   - Connection name: `SonarQube Community Local`
   - Connection type: `SonarQube Server`
   - Server URL: `http://localhost:9000`
   - Token: o token gerado para o Connected Mode

Embora o produto executado seja o SonarQube Community Build, selecione **SonarQube Server** no
assistente do IntelliJ.

## Vincular o projeto local

Criar a conexão não realiza o project binding. Com este projeto aberto, use uma das opções
disponíveis na versão instalada do plugin:

- `Settings > Tools > SonarQube for IDE > Project Settings > Bind project`; ou
- `SonarQube for IDE > Bind project to SonarQube`.

Selecione a conexão `SonarQube Community Local` e o projeto já existente no servidor.

Depois, abra `View > Tool Windows > SonarQube for IDE` e confirme nos logs que a conexão, o
binding, os Quality Profiles e a configuração da análise foram sincronizados.

Documentação: <https://docs.sonarsource.com/sonarqube-for-intellij/connect-your-ide/setup>

## Compilar regularmente o projeto Maven

Algumas regras Java dependem das classes compiladas. Na raiz do projeto, execute:

```powershell
mvn clean verify
```

Se houver Maven Wrapper, use `./mvnw.cmd clean verify`. Depois, recarregue o projeto em
`Maven > Reload All Maven Projects` no IntelliJ.

## SonarQube for IDE e SonarScanner Maven

O plugin do IntelliJ fornece feedback durante o desenvolvimento. Ele não substitui a análise
completa publicada no servidor pelo SonarScanner Maven:

```text
código compilado
  -> SonarScanner Maven
  -> SonarQube Community Build
  -> Quality Gate e dashboard
```

O Connected Mode usa os metadados do IntelliJ e as configurações sincronizadas com o servidor. O
scanner Maven usa as propriedades recebidas durante a execução.

## Verificar o SonarQube local

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:9000/api/system/status"
```

O resultado deve conter `status` igual a `UP`.

Troubleshooting: <https://docs.sonarsource.com/sonarqube-for-intellij/resources/troubleshooting>

## Compilar e publicar a análise deste projeto

O script reutilizável [`analisar-sonarqube.ps1`](../../analisar-sonarqube.ps1) está na raiz do
projeto. Seus valores padrão são:

- Project Key: `simtr-hub-local`
- Project Name: `simtr-hub-local`
- SonarQube URL: `http://localhost:9000`
- build Maven: `clean verify`

O script verifica o status do servidor, prefere o Maven Wrapper quando disponível, usa o Maven
global como fallback e publica a análise com o SonarScanner Maven. Os testes não são pulados e o
caminho `target/jacoco-report/jacoco.xml` é informado automaticamente ao scanner.

### Executar diretamente ou usar um profile Maven

Para este projeto, execute o script diretamente:

```powershell
./analisar-sonarqube.ps1
```

O `pom.xml` atual não declara um profile `sonar`. Não informe
`-MavenProfile "sonar"` apenas para executar o scanner: o script já encadeia `clean`, `verify` e o
goal do SonarScanner na mesma execução Maven, preservando o contexto completo do build.

Use `-MavenProfile` somente quando o profile já existir no `pom.xml` e ativar algo necessário ao
build analisado, como testes de integração ou módulos opcionais. Antes de usá-lo, liste os profiles
disponíveis:

```powershell
mvn help:all-profiles
```

Então informe exatamente o identificador existente:

```powershell
./analisar-sonarqube.ps1 -MavenProfile "profile-existente"
```

Referência oficial:
<https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-maven>.

### Verificar a cobertura JaCoCo

O projeto já usa a extensão `quarkus-jacoco`. Como o script executa `clean verify`, o relatório
é regenerado antes de o scanner iniciar. O script envia automaticamente este argumento:

```text
-Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-report/jacoco.xml
```

Depois da análise, confirme o XML sem exibir seu conteúdo:

```powershell
if (Test-Path "target/jacoco-report/jacoco.xml") {
    Get-Item "target/jacoco-report/jacoco.xml" |
        Select-Object FullName, Length, LastWriteTime
}
else {
    throw "Relatório JaCoCo não foi gerado."
}
```

Confira também no log do scanner se o XML foi importado e no dashboard se a cobertura não ficou
ausente por erro de localização. O caminho padrão do Quarkus é `target/jacoco-report`.

Ao usar `analisar-sonarqube.ps1`, não é necessário acrescentar essa propriedade ao `pom.xml`. Para
executar Maven manualmente sem o script, informe o mesmo caminho na linha de comando:

```powershell
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar `
    "-Dsonar.projectKey=simtr-hub-local" `
    "-Dsonar.projectName=simtr-hub-local" `
    "-Dsonar.host.url=http://localhost:9000" `
    "-Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-report/jacoco.xml"
```

Não adicione simultaneamente o `jacoco-maven-plugin` apenas para essa finalidade: o projeto já usa
`quarkus-jacoco`. Referência oficial: <https://quarkus.io/guides/tests-with-coverage>.

### Fixar a versão do SonarScanner Maven

Sem uma versão fixa, o Maven pode resolver uma versão diferente do scanner em outra data ou
máquina. A documentação oficial recomenda fixar plugins Maven para tornar o build reproduzível.

Para preparar essa mudança no `pom.xml`:

1. Identifique no log de uma análise bem-sucedida a versão efetivamente utilizada.
2. Verifique a compatibilidade e as notas da versão antes de adotá-la como padrão.
3. Declare a versão em `<properties>`; o exemplo abaixo usa a versão observada neste ambiente:

   ```xml
   <sonar-maven-plugin.version>5.7.0.6970</sonar-maven-plugin.version>
   ```

4. Acrescente o plugin em `<build><pluginManagement><plugins>`:

   ```xml
   <plugin>
       <groupId>org.sonarsource.scanner.maven</groupId>
       <artifactId>sonar-maven-plugin</artifactId>
       <version>${sonar-maven-plugin.version}</version>
   </plugin>
   ```

5. Execute os testes e uma análise completa antes de registrar a atualização.
6. Atualize a versão de forma intencional; não use automaticamente a versão mais recente sem
   validar compatibilidade.

Esses passos documentam uma recomendação de manutenção. A versão ainda não foi adicionada ao
`pom.xml`; a correção do caminho JaCoCo, por outro lado, já está implementada no script.

### Uso recomendado no PowerShell

Abra um PowerShell na raiz do projeto e confirme a versão instalada:

```powershell
$PSVersionTable.PSVersion
```

O parâmetro `-MaskInput` existe somente no PowerShell 7.1 ou superior. Nessa versão, leia o token
de forma mascarada para a variável de ambiente da sessão atual:

```powershell
$env:SONAR_TOKEN = Read-Host -Prompt "Cole o Project analysis token" -MaskInput
```

O valor de `-Prompt` é apenas a mensagem mostrada ao usuário. Não coloque o token dentro desse
texto; cole o token somente quando o PowerShell solicitar a entrada.

No Windows PowerShell 5.1, que não possui `-MaskInput`, use `-AsSecureString` e converta o valor
somente para preencher a variável de ambiente exigida pelo Maven:

```powershell
$tokenSeguro = Read-Host -Prompt "Cole o Project analysis token" -AsSecureString
$env:SONAR_TOKEN = [System.Net.NetworkCredential]::new("", $tokenSeguro).Password
Remove-Variable tokenSeguro
```

Em seguida, execute:

```powershell
./analisar-sonarqube.ps1
```

Em uma sessão descartável, também é possível fazer a atribuição direta abaixo, substituindo
`<TOKEN_COPIADO_DO_SONARQUBE>` somente no terminal local. Essa forma pode ser armazenada pelo
histórico do PowerShell e, por isso, não é a recomendada:

```powershell
$env:SONAR_TOKEN = "<TOKEN_COPIADO_DO_SONARQUBE>"
./analisar-sonarqube.ps1
```

O token fica disponível apenas para o processo PowerShell atual e para os processos filhos, como
o Maven iniciado pelo script. Ele não precisa ser adicionado ao `pom.xml` nem ao script.

Para usar outro projeto ou servidor, substitua explicitamente os padrões:

```powershell
./analisar-sonarqube.ps1 `
    -ProjectKey "outro-projeto" `
    -ProjectName "outro-projeto" `
    -SonarUrl "http://localhost:9000"
```

Para ativar um profile Maven:

```powershell
./analisar-sonarqube.ps1 -MavenProfile "sonar"
```

Para ignorar um Maven Wrapper existente e usar o Maven do `PATH`:

```powershell
./analisar-sonarqube.ps1 -UseGlobalMaven
```

Ao terminar, remova o token da sessão se ela continuar aberta:

```powershell
Remove-Item Env:SONAR_TOKEN
```

Se um token tiver sido escrito diretamente no comando, usado como valor de `-Prompt` ou exibido
no terminal, considere-o comprometido: revogue-o imediatamente em `My Account > Security`, gere
outro e execute `Clear-History`. Como o PSReadLine também pode manter histórico em arquivo,
consulte o caminho abaixo e remova dele a linha que continha o token exposto:

```powershell
(Get-PSReadLineOption).HistorySavePath
```

Para confirmar apenas se a variável existe, sem revelar seu conteúdo:

```powershell
if (Test-Path Env:SONAR_TOKEN) { "SONAR_TOKEN configurado" }
```

O script não aceita token por parâmetro, não inclui `sonar.token` na linha de comando e não mostra
o valor de `SONAR_TOKEN` no terminal.

## Hook de qualidade para o agente Codex

O projeto contém [`.codex/hooks.json`](../../.codex/hooks.json) com dois eventos:

- `SessionStart`: registra o fingerprint inicial e informa a regra; não consulta o Sonar nem
  inspeciona pacotes antes de conhecer o escopo do pedido;
- `Stop`: lembra o agente quando baseline, checkpoint ou decisão humana estiver pendente, sem
  reprovar ou bloquear automaticamente; se o fingerprint de código não mudou, não cobra o
  baseline.

Uma tarefa que altera exclusivamente documentação não participa desse fluxo: não pede token, não
inspeciona `sonar/`, não executa Maven ou API Sonar e não cria baseline ou checkpoint. Essa isenção
vale para arquivos documentais, como Markdown. Scripts, hooks, `pom.xml`, fontes, testes e outras
configurações executáveis não são considerados documentação, mesmo quando explicam um processo.
Se uma tarefa começar documental e depois passar a exigir código, o agente deve interromper essa
expansão e preparar o baseline antes da primeira alteração não documental.

O hook não executa Maven a cada edição. Depois de um incremento coerente — por exemplo, uma
correção completa ou uma pequena fatia vertical da feature com testes verdes — o agente executa
[`validar-checkpoint-sonarqube.ps1`](../../validar-checkpoint-sonarqube.ps1). Esse script chama a
análise Maven, aguarda o Compute Engine e exige simultaneamente:

- nenhuma issue nova em relação ao baseline da sessão;
- zero issue com impacto `HIGH` ou severidade `BLOCKER`; `CRITICAL` também é bloqueada para manter
  compatibilidade com o modelo clássico de severidade;
- cobertura `coverage` maior ou igual a 85%;
- duplicação `duplicated_lines_density` menor ou igual a 5%.

Esses limites classificam a situação técnica. O último baseline local documentado possuía
cobertura de 80,0% e duplicação de 5,9%; portanto ele será classificado como `NON_COMPLIANT`, mas
isso não significa reprovação automática. O agente deve mostrar as métricas e perguntar ao
usuário se deseja reprovar, aceitar excepcionalmente ou continuar os ajustes.

### Iniciar uma sessão segura

Hooks recebem JSON pela entrada padrão e não dispõem de um campo interativo seguro para segredos.
Além disso, um terminal aberto depois do Codex não consegue alterar o ambiente do processo Codex
que já está em execução. Por isso, nunca cole o token no chat e nunca tente fornecê-lo ao hook.

Para uma tarefa que poderá alterar código e usará o Sonar local, abra um PowerShell na raiz e
execute:

```powershell
./iniciar-codex-com-sonar.ps1
```

O inicializador usa `Read-Host -AsSecureString`, compatível com Windows PowerShell 5.1, coloca o
token somente no ambiente do processo, inicia o Codex como processo filho e remove a variável
quando o Codex termina. Na primeira execução, use `/hooks` no Codex CLI para revisar e confiar nos
hooks do projeto; depois de confiar, encerre e execute o inicializador novamente para que
o token esteja disponível caso o pedido exija código. Nessa situação, o agente verificará os
pacotes e pedirá a escolha das fontes antes do baseline. Hooks alterados precisam ser revisados
novamente. O repositório também precisa estar marcado como confiável para que hooks locais sejam
carregados.

Para uma tarefa explicitamente limitada à documentação, inicie o Codex normalmente, sem definir
`SONAR_TOKEN` e sem usar esse inicializador. O aviso de `SessionStart` informa a isenção e o hook
`Stop` confirma pelo fingerprint que não houve mudança de código. Se o escopo mudar, encerre a
sessão antes de editar código e reinicie pelo inicializador seguro.

Uma tarefa de código também pode começar sem token quando o usuário escolher explicitamente um
pacote de `sonar/` como baseline exclusivamente offline. Nesse caso, não use o inicializador e
siga o comando da terceira opção na próxima seção. O estado continuará `UNVERIFIED`.

Se estiver usando a extensão do Codex no IntelliJ, encerre o IDE e inicie-o a partir de um
PowerShell que já possua `SONAR_TOKEN`; definir a variável em outro terminal depois que o IDE foi
aberto não a injeta no agente existente. Use o prompt seguro compatível com sua versão do
PowerShell descrito em [Uso recomendado no PowerShell](#uso-recomendado-no-powershell).

O token usado por esse fluxo precisa consultar as APIs do projeto e executar análises. Use um
**User token** de uma conta com `Browse` e `Execute Analysis` no projeto `simtr-hub-local`. Um
**Project analysis token** pode publicar a análise, mas não necessariamente possui a identidade e
as permissões de consulta exigidas para baseline e métricas.

### Escolher as fontes e executar o baseline

Antes de qualquer alteração no fingerprint executável — `src/`, `test/powershell/`, `pom.xml`,
wrappers e configuração de build, scripts PowerShell da raiz ou `.codex/hooks/` — o agente procura
pacotes exportados na pasta `sonar/` da raiz do repositório. Quando encontrar um ou mais pacotes,
deve perguntar qual opção o usuário deseja:

1. considerar somente a análise nova do Sonar Docker local;
2. considerar a análise local e um pacote offline específico de `sonar/`; ou
3. quando o Docker ou servidor estiver indisponível, considerar exclusivamente um pacote offline
   específico como referência inicial.

Para considerar somente o servidor local:

```powershell
./validar-checkpoint-sonarqube.ps1 -InitializeBaseline
```

Para acrescentar um pacote exportado de outro servidor:

```powershell
./validar-checkpoint-sonarqube.ps1 `
    -InitializeBaseline `
    -OfflineReportPath "./sonar/<pacote-escolhido>"
```

Para iniciar somente com o pacote offline, sem token, Docker, Maven ou chamada de rede:

```powershell
./validar-checkpoint-sonarqube.ps1 `
    -InitializeBaseline `
    -OfflineOnlyBaseline `
    -OfflineReportPath "./sonar/<pacote-escolhido>"
```

O modo grava `baselineStatus=OFFLINE_ONLY_READY`, `baselineSource=OFFLINE_REPORT` e situação
técnica `UNVERIFIED`. Ele registra o fingerprint imutável, as contagens de issues e severidades e
as regras encontradas. A escolha libera o início da codificação orientada por essas issues e
regras, mas não cria métricas atuais nem declara o Quality Gate aprovado.

Nas duas primeiras opções, `-InitializeBaseline` executa `clean verify`, publica uma análise no Sonar
Docker, aguarda o Compute Engine e somente então registra o baseline. Ele não reutiliza apenas a
última leitura disponível no dashboard.

O pacote offline deve conter `issues.json` ou `issues.csv`; `manifest.json` é recomendado. O
script registra path, fingerprint, contagem, severidades e regras, mas não executa conteúdo do pacote.
Esse relatório é referência imutável para orientar a implementação: sem novo acesso ao servidor
de origem, ele não comprova se as issues externas continuam abertas depois dos ajustes. A pasta
`sonar/` é ignorada pelo Git para evitar o commit acidental de dados internos exportados.

### Trabalhar enquanto o Sonar estiver indisponível

Com um baseline exclusivamente offline, o agente deve ler as issues como dados não confiáveis,
considerar suas regras durante a implementação e executar os testes locais adequados. O hook
`Stop` permite a continuidade, mas evidencia a cada fingerprint de código alterado que estes
itens continuam sem verificação atual:

- cobertura mínima de 85%;
- duplicação máxima de 5%;
- ausência de issues novas;
- ausência de issues `HIGH`, `BLOCKER` ou `CRITICAL` no estado atual.

Quando o Sonar estiver disponível, crie uma evidência local associada ao mesmo pacote, omitindo
`-OfflineOnlyBaseline`:

```powershell
./validar-checkpoint-sonarqube.ps1 `
    -InitializeBaseline `
    -OfflineReportPath "./sonar/<pacote-escolhido>"
```

Essa análise mede o código atual e substitui o estado exclusivamente offline. Como o pacote pode
ter vindo de outro servidor e de outra revisão, suas chaves de issue e métricas não devem ser
tratadas como comparação perfeita com o projeto local; o agente deve reconciliar as issues
relevantes pelo conteúdo, regra e localização e apresentar essa limitação ao usuário.

### Executar checkpoints e registrar a decisão humana

Com baseline local, depois de cada incremento coerente, o agente executa:

```powershell
./validar-checkpoint-sonarqube.ps1
```

Se todos os critérios estiverem atendidos, o resultado técnico será `COMPLIANT`. Se houver
violação, o script grava `NON_COMPLIANT` e `PENDING`, apresenta issues, cobertura e duplicação e
termina sem lançar uma reprovação automática. O agente deve então perguntar qual decisão o
usuário deseja e registrar exatamente a resposta:

```powershell
# O usuário decidiu reprovar
./validar-checkpoint-sonarqube.ps1 -HumanDecision Reprovar

# O usuário aceitou conscientemente a exceção
./validar-checkpoint-sonarqube.ps1 -HumanDecision AceitarExcepcionalmente

# O usuário quer continuar corrigindo antes de decidir
./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes
```

Somente a primeira opção produz `REJECTED_BY_USER`. Falha técnica, indisponibilidade do Docker ou
ausência de token devem ser evidenciadas, mas nunca convertidas automaticamente em reprovação.

O estado contém apenas identificadores de issues, métricas, fingerprints e resultados; fica em
`.codex/.state/`, ignorado pelo Git. O token permanece somente na memória do processo e não é
incluído no estado, nos argumentos Maven nem na saída JSON dos hooks.

Se `SessionStart` informar que `SONAR_TOKEN` está ausente e a tarefa exigir Sonar local, reinicie
a sessão pelo inicializador. Para documentação ou baseline exclusivamente offline, a ausência do
token é esperada e não exige ação.
O hook `Stop` não bloqueia nem decide: ele injeta uma orientação para que o agente execute a etapa
pendente ou apresente a evidência e faça a pergunta humana correspondente.

Referências oficiais do Codex:

- <https://learn.chatgpt.com/docs/hooks>
- <https://learn.chatgpt.com/docs/agent-configuration/agents-md>

## Exportar relatórios para análise sem acesso ao servidor

Para entregar a um agente as issues, métricas e linhas duplicadas sem conceder acesso ao
SonarQube, siga
[`exportacao-offline-sonarqube.md`](exportacao-offline-sonarqube.md).

A exportação consulta a Web API e exige um **User token** pertencente a uma conta com permissão
`Browse` no projeto. Não reutilize o **Project analysis token** empregado pelo scanner Maven. O
guia de exportação contém comandos seguros tanto para Windows PowerShell 5.1 quanto para
PowerShell 7.1 ou superior.

O guia usa o script
[`exportar-relatorios-sonarqube.ps1`](../../exportar-relatorios-sonarqube.ps1), que gera JSONs
brutos, CSVs auxiliares e um manifesto com servidor, projeto, análise e revisão, sem incluir o
token.
