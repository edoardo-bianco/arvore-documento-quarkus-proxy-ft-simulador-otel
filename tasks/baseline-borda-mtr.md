# Baseline da borda MTR com stub local

> Documento historico de caracterizacao anterior a migracao. Para packages, adapters e gates
> finais, consulte `equivalencia-final.md`; evidencias quantitativas permanecem somente no JaCoCo.

## Escopo e resultado

Este documento registra a evidencia executavel da Task 0.3 da Fase 0. A criacao de dossie percorre
o caminho real `Resource -> fachada -> servico -> gateway -> REST Client -> stub HTTP`, com o
simulador desabilitado. O stub escuta somente em `127.0.0.1`, usa porta efemera e nao encaminha
chamadas para qualquer servico externo.

- stack exercitada: Quarkus `3.33.2.1`, REST Client reativo, OIDC Client reativo, OpenTelemetry e
  SmallRye Fault Tolerance;
- teste executavel: `DossieProdutoMtrContractTest`;
- infraestrutura: `DossieProdutoMtrStubTestResource`;
- testes focados: cinco, todos aprovados;
- suite completa apos a caracterizacao: 131 testes, zero falhas, zero erros e zero ignorados;
- codigo ou configuracao de producao alterados: nenhum;
- credenciais reais utilizadas: nenhuma.

O `QuarkusTestResourceLifecycleManager` inicia antes da aplicacao e fornece dinamicamente a URL do
REST Client e do token endpoint OIDC. O perfil padrao `test`, configurado em
`src/test/resources/application.properties`, habilita o OIDC Client; o recurso de teste desabilita
o simulador para as classes de contrato que percorrem o MTR. Nao existe `QuarkusTestProfile` nem
`@TestProfile`. O `connection-retry-count=1` evita a combinacao invalida
`retry().atMost(0)` observada no Quarkus `3.33.2.1` quando o OIDC Client, normalmente desligado nos
testes, e habilitado. Esse ajuste nao altera as politicas de fault tolerance do REST Client.

## Contratos protegidos

| Cenario | Evidencia protegida | Teste |
|---|---|---|
| Sucesso ponta a ponta | `POST /simtr/dossie-produto/v1/dossie-produto`; JSON MTR estruturalmente igual ao request publico atual; `Content-Type` e `Accept` JSON; `apikey=test-apikey`; `Authorization=Bearer stub-access-token`; `traceparent` W3C; resposta MTR `201 {"id":987}` convertida na resposta publica `201` equivalente | `percorreResourceAteStubMtrPreservandoWireHeadersEResposta` |
| Provider de observabilidade | atributos `rest_client.class=DossieProdutoClient`, `rest_client.operation=criarDossieProduto`, status `201` e eventos `mtr.rest-client.request.enviada`/`mtr.rest-client.response.recebida` presentes no span real | `percorreResourceAteStubMtrPreservandoWireHeadersEResposta` |
| Erro de negocio | corpo MTR completo `400` preservado na borda publica; `abortOn` impede retry e o stub recebe uma unica requisicao | `preservaErroDeNegocioCompletoSemRetry` |
| Retry recuperavel | primeira resposta MTR `500`, segunda `201`; o mesmo JSON e reenviado exatamente duas vezes e a API publica conclui com sucesso | `retryReenviaMesmoWireAposErroMtrRecuperavel` |
| Timeout | primeira resposta atrasa `2100 ms`, acima do `@Timeout(2000 ms)` e abaixo do read timeout de teste de `5000 ms`; a tentativa e cancelada, o retry recebe a segunda resposta e a API devolve somente o segundo resultado | `timeoutCancelaTentativaLentaEExecutaRetry` |
| Circuit breaker | falhas `500` enfileiradas para workflow; o limiar atual abre o circuito depois de 10 requisicoes remotas e chamadas seguintes nao alcancam o stub | `circuitBreakerAbertoInterrompeNovasChamadasAoStub` |

## Controles determinísticos reutilizáveis

O stub permite, por teste:

- limpar todas as requisicoes e respostas anteriores;
- enfileirar status, corpo e atraso por tentativa;
- repetir a mesma falha uma quantidade exata de vezes;
- capturar metodo, path, corpo e headers relevantes;
- atender simultaneamente a tentativa lenta e o retry em threads locais separadas.

Esses controles permitem reutilizar a infraestrutura nas caracterizacoes verticais das Fases 1 a
5. A matriz completa por operacao continua pertencendo a essas fases; a Task 0.3 prova o mecanismo
e congela uma operacao com payload, uma falha de negocio e as tres politicas de resiliencia.

## Limites preservados

- nenhum endpoint, workflow, upload ou cache de SAS foi implementado;
- nenhuma chamada sai de localhost;
- o token, API key e client secret usados no ambiente `test` sao valores falsos de teste;
- as annotations e os valores de retry, timeout e circuit breaker de producao nao foram alterados;
- o teste do workflow provoca somente chamadas HTTP locais e nao resolve o risco funcional de
  idempotencia das operacoes mutaveis;
- as demais operacoes e bordas MTR serao caracterizadas na fase de sua capacidade.

## Fontes oficiais

- Quarkus test resources:
  https://quarkus.io/guides/getting-started-testing#starting-services-before-the-quarkus-application-starts
- teste do REST Client com servidor HTTP local:
  https://quarkus.io/guides/rest-client#using-a-mock-http-server-for-tests
- OIDC Client reativo e injecao de bearer token:
  https://quarkus.io/guides/security-openid-connect-client-reference#use-oidcclient-in-microprofile-restclient-client-filter
- SmallRye Fault Tolerance:
  https://quarkus.io/guides/smallrye-fault-tolerance
