# Relatorio de ajustes da maquina - Endpoint POST Dossie Produto

Este relatorio consolida os ajustes necessarios na maquina de trabalho depois da introducao do endpoint:

```http
POST /arvore-documento/v1/dossie-produto
```

O endpoint exposto pelo `arvore-documento` chama o MTR real em:

```http
POST /simtr/dossie-produto/v1/dossie-produto
```

## 1. Atualizar o checkout local

Garantir que a maquina esta na branch com a implementacao:

```powershell
git fetch origin
git switch feat/dossie-produto
git pull
```

Validar estado local:

```powershell
git status -sb
```

Observacao: no momento da geracao deste relatorio, este arquivo ainda esta novo no worktree e precisa ser commitado/pushado se for aprovado.

## 2. Java e Maven

O projeto usa Quarkus e Maven. Validar:

```powershell
java -version
mvn -version
```

Depois validar compilacao:

```powershell
mvn -q -DskipTests compile
```

## 3. Variaveis obrigatorias para chamada real

Como o dossie produto esta configurado para chamada real tambem em dev mode, a maquina precisa ter credenciais e conectividade com o ambiente CAIXA.

Configurar no PowerShell ou na Run Configuration do IntelliJ:

```powershell
$env:SIMTR_API_KEY="valor-da-apikey"
$env:SIMTR_OIDC_CLIENT_SECRET="valor-do-secret-intranet"
$env:SIMTR_OIDC_INTERNET_CLIENT_SECRET="valor-do-secret-internet"
```

Se quiser sobrescrever as URLs base:

```powershell
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL="https://api.des.caixa:8443/simtr"
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL="https://api.des.caixa:8443/simtr"
$env:QUARKUS_REST_CLIENT_DOSSIE_PRODUTO_URL="https://api.des.caixa:8443/simtr"
```

No IntelliJ, adicionar as mesmas variaveis em:

```text
Run/Debug Configuration -> Environment variables
```

## 4. Conectividade de rede

Para chamada real, validar acesso ao gateway:

```powershell
Test-NetConnection api.des.caixa -Port 8443
```

Se falhar, verificar:

- VPN corporativa;
- proxy corporativo;
- DNS interno;
- firewall local;
- permissao de acesso ao ambiente de desenvolvimento.

Em dev mode, o projeto ja possui:

```properties
%dev.quarkus.tls.trust-all=true
%dev.quarkus.oidc-client.tls.verification=none
```

Mesmo assim, a rede precisa permitir acesso ao host remoto.

## 5. Simulador do dossie produto

Configuracao atual em `application.properties`:

```properties
arvore-documento.simulador.dossie-produto.habilitado=false
%dev.arvore-documento.simulador.dossie-produto.habilitado=false
```

Resultado pratico:

- em modo normal, chama MTR real;
- em dev mode, tambem chama MTR real;
- para usar mock local, e necessario habilitar explicitamente.

Rodar com mock de dossie produto:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.dossie-produto.habilitado=true"
```

Rodar com chamada real:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.dossie-produto.habilitado=false"
```

Mock usado pelo runtime:

```text
src/main/resources/mock/dossieproduto/criacao-basica-dossie-produto.md
```

Copia documental:

```text
doc/mock/dossie-produto/criacao-basica-dossie-produto.md
```

## 6. Configuracao recomendada no IntelliJ

Criar uma Run Configuration Maven:

```text
Name: arvore-documento [dev real dossie-produto]
Working directory: C:\desenvolvimento\repositorio\arvore-documento-quarkus-proxy-ft-simulador-otel
Run: quarkus:dev -Ddebug=false "-Darvore-documento.simulador.dossie-produto.habilitado=false"
Environment variables:
  SIMTR_API_KEY=...
  SIMTR_OIDC_CLIENT_SECRET=...
  SIMTR_OIDC_INTERNET_CLIENT_SECRET=...
```

Para execucao com mock:

```text
Name: arvore-documento [dev mock dossie-produto]
Run: quarkus:dev -Ddebug=false "-Darvore-documento.simulador.dossie-produto.habilitado=true"
```

## 7. Como testar o endpoint

Subir a aplicacao:

```powershell
mvn quarkus:dev -Ddebug=false
```

Chamar o endpoint:

```powershell
$body = @{
  processo = 0
  chave_correlacao_canal = 0
  numero_negocio = 0
  clientes = @(
    @{
      cpf = "string"
      cnpj = "string"
      tipo_vinculo = 0
      cliente_relacionado = @{
        cpf = "string"
        cnpj = "string"
      }
      sequencia_titularidade = 0
    }
  )
} | ConvertTo-Json -Depth 5

Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/dossie-produto" `
  -Method POST `
  -ContentType "application/json" `
  -Headers @{ Accept = "application/json" } `
  -Body $body
```

Resultado esperado com mock:

```json
{
  "id": 1
}
```

Resultado esperado com MTR real:

- HTTP `201`;
- corpo JSON com `id` do dossie produto criado;
- em caso de erro funcional, corpo no padrao `ErroPadraoDto`.

## 8. Swagger e OpenAPI

Swagger UI:

```http
http://localhost:8080/arvore-documento/doc
```

OpenAPI:

```http
http://localhost:8080/arvore-documento/openai
```

Observacao: o path atual do OpenAPI esta como `openai`, nao `openapi`.

## 9. Logs e diagnostico local

Arquivo de log local:

```text
target/logs/arvore-documento.json
```

Acompanhar logs:

```powershell
Get-Content -Tail 50 -Wait target/logs/arvore-documento.json
```

Filtrar eventos do dossie produto:

```powershell
Select-String -Path target/logs/arvore-documento.json -Pattern "dossie-produto"
```

Eventos esperados:

- `arvore-documento.dossie-produto.requisicao.recebida`;
- `arvore-documento.dossie-produto.service.iniciado`;
- `arvore-documento.dossie-produto.simulador.usado`, quando mock estiver ativo;
- `mtr.dossie-produto.criacao.chamada.iniciada`, quando chamada real estiver ativa;
- `mtr.rest-client.request.enviada`;
- `mtr.rest-client.response.recebida`;
- `arvore-documento.dossie-produto.resposta.enviada`.

## 10. Observabilidade opcional

Padrao local:

- logs JSON no console;
- logs JSON em `target/logs/arvore-documento.json`;
- sem exportacao OpenTelemetry externa.

Para Jaeger local:

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```

Requer Jaeger/OTLP em:

```text
localhost:4317
```

Para Grafana/Tempo/Loki via OpenTelemetry Collector:

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"
```

Requer collector OTLP em:

```text
localhost:4317
```

## 11. Pontos de atencao

1. O dossie produto esta configurado para chamada real em dev mode. Sem credenciais e rede corporativa, o POST tende a falhar.
2. Para trabalhar offline, iniciar com `-Darvore-documento.simulador.dossie-produto.habilitado=true`.
3. O payload do REST Client pode aparecer truncado nos logs conforme:

```properties
arvore-documento.observabilidade.rest-client.payload.input.max-length=2000
arvore-documento.observabilidade.rest-client.payload.output.max-length=4000
```

4. Nao versionar `target/logs/arvore-documento.json`; e arquivo gerado localmente.
5. O endpoint real exige `apikey` e token OIDC via filtro do REST Client.
6. A documentacao detalhada deve ficar alinhada com `application.properties`, principalmente quanto ao valor `%dev.arvore-documento.simulador.dossie-produto.habilitado`.

## 12. Checklist final da maquina

- [ ] Branch `feat/dossie-produto` atualizada.
- [ ] Relatorio local commitado/pushado, se for aprovado.
- [ ] Java e Maven funcionando.
- [ ] `mvn -q -DskipTests compile` executado com sucesso.
- [ ] `SIMTR_API_KEY` configurada.
- [ ] `SIMTR_OIDC_CLIENT_SECRET` configurada.
- [ ] `SIMTR_OIDC_INTERNET_CLIENT_SECRET` configurada.
- [ ] VPN/rede corporativa ativa para chamada real.
- [ ] `Test-NetConnection api.des.caixa -Port 8443` validado.
- [ ] Run Configuration do IntelliJ criada para chamada real.
- [ ] Run Configuration do IntelliJ criada para mock, se necessario.
- [ ] POST testado em `/arvore-documento/v1/dossie-produto`.
- [ ] Logs conferidos em `target/logs/arvore-documento.json`.
