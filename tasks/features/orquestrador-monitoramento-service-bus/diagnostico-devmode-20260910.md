# Diagnóstico do startup em dev mode — 2026-09-10

## Pedido e escopo

Após concluir a publicação de 9.1, o usuário pediu verificar o startup que parecia não subir
nem encerrar, com avisos JDBC `Prelogin error ... Unexpected end of prelogin response after
0 bytes read`, incluindo a configuração da emulação do Service Bus.

Diagnóstico operacional na branch `feature/orquestrador-monitoramento-service-bus`, sobre
`0ec09ac46ef5e2e6ee849d9d87384ff788fd30b2`. Sem alteração de código, dependências, configuração
versionada, baseline ou aceite C9.1-L. Não iniciar 10.1. Os únicos arquivos editados nesta
atividade são registros Markdown do diagnóstico e da retomada.

## Conclusão e limites

A configuração versionada permitiu duas inicializações reais do Quarkus em dev mode, com
SQL Server, emulador e ambos os listeners. Os avisos de prelogin foram reproduzidos durante
a preparação do SQL e cessaram. A indisponibilidade persistente da tentativa original não foi
reproduzida, portanto não se atribui a ela uma causa definitiva nem se altera timeout, senha,
imagem, transporte ou logging para ocultar os avisos.

No relato original, os eventos Docker preservados mostraram SQL iniciado às 20:13:22 e
encerrado/removido às 20:14:04, antes de criar o emulador. Não havia evento OOM nesse intervalo.
O container já havia sido removido; seus logs internos não estavam mais disponíveis.
Um exit 137 precedido por kill não prova falta de memória nem identifica quem interrompeu.

## Configuração conferida

| Item | Estado verificado |
|---|---|
| Plataforma | Quarkus 3.33.2.1, extensão Azure Services 1.2.5, Java 25.0.3 e Maven 3.9.16 |
| Imagens | Emulador 1.1.2 e SQL Server 2022-CU14-ubuntu-22.04, iguais às recomendadas pela extensão |
| Dev Services | Habilitado em dev, licenças aceitas, sem conexão/namespace Azure no ambiente herdado |
| Transporte local | AMQP/TCP; AMQP_WEB_SOCKETS restrito ao profile azure |
| Filas | config.json no caminho esperado; ambas as filas de entrada/saída foram criadas pelo emulador |
| Listeners | Flags independentes; ambos ativados explicitamente nas duas reproduções |
| Pré-validação/Hub | Pré-validação simulada explicitamente; simulador do Hub habilitado pelo profile dev |
| Configuração externa do dev | .env local presente com as três chaves exigidas; conferida somente presença, sem copiar valores |
| Isolamento | Nenhuma fila Azure/MTR real acessada; infraestrutura de diagnóstico criada pelo Dev Services |

A ausência de variáveis no ambiente do shell não significa ausência na configuração efetiva:
o Quarkus também lê o .env local. Esse arquivo foi preservado e permanece fora do Git.

## Evidência das reproduções

1. Inicialização com o comando dos dois listeners do guia, defaults da política:
   - SQL iniciou às 20:31:58; completou recuperação e criou os bancos do emulador;
   - o emulador confirmou criação das duas filas e inicialização;
   - Quarkus registrou startup em **24,416 s**, às 20:32:19, escutando localhost:8080;
   - GET /q/health retornou 200/UP; Swagger retornou 200.
2. Nova inicialização, após encerrar a primeira, com a política local documentada
   PT5S/PT1M/máximo3 e ambos os listeners:
   - Quarkus iniciou em **27,213 s**; os avisos de prelogin cessaram;
   - GET /q/health retornou 200;
   - POST com pré-validação pre-em-analise e fixture MTR 4324680 retornou 202;
   - houve um evento final orquestrador.monitoramento-dossie.resultado.registrado
     às 20:39:51.8544851-03:00, com os mesmos IDs em mdc:
     monitoramentoId 035bb4d0-92e1-42e8-a752-d015d3a358b5 e
     orquestracaoId 92d00014-447f-44ab-bcca-7684784ae589.

O POST dessa prova usou o simulador real do Hub, sem substituir sua porta por mock.
Health sozinho não comprova consumo; a evidência adicional é a correlação do POST ao log final.
Não houve um segundo receiver de diagnóstico disputando as filas nem inspeção separada do
settlement da saída. As provas automatizadas de Complete/Abandon/DLQ permanecem as de 9.1-B/C.
O logging continua best-effort por C9.1-L.

Enquanto a primeira aplicação ficou ociosa, o emulador fechou links AMQP e o SDK os reabriu,
classificando a falha como recuperável. Isso ocorreu depois do startup; não é o aviso JDBC
de prelogin. Não foi introduzido retry da aplicação ou ampliado o escopo para 10.1/C3.

## Operação e encerramento

mvn quarkus:dev é um processo contínuo: depois de iniciar, permanece servindo requisições
e aguardando alterações. O terminal não deve voltar ao prompt espontaneamente.
Confirmar a linha started/Listening on e o health; para encerrar, usar Ctrl+C no terminal
dono do processo. Na segunda reprodução, a aplicação encerrou após o primeiro Ctrl+C e
um segundo encerrou o terminal cmd remanescente.

As duas reproduções foram encerradas. Conferência final: sem Java da aplicação, sem listener
HTTP na porta 8080 e sem containers SQL/emulador/Ryuk criados pelo diagnóstico. Os containers
preexistentes de Kubernetes, SonarQube e Jaeger foram preservados.

Não houve nova suíte Maven/Sonar: foram executadas duas inicializações dev e uma prova HTTP
funcional, sem mudança executável. O checkpoint de código de 9.1-C permanece a referência.
O diagnóstico foi concluído antes da publicação documental. Depois da verificação, o usuário
autorizou explicitamente commit e push dos quatro Markdown; o código validado permanece inalterado.

## Se o problema voltar

Antes de interromper, conferir em outro terminal:

```powershell
docker ps -a
Invoke-RestMethod -Uri 'http://localhost:8080/q/health' -TimeoutSec 5
```

Se ainda não houver HTTP, capturar localmente o log do container SQL daquela tentativa e o
erro final do Maven/Quarkus. Portas como 64792 são mapeamentos dinâmicos do Docker, não valores
para fixar em application.properties. Preservar o processo tempo suficiente para distinguir
inicialização de erro definitivo; não inferir sucesso somente pela ausência de erro.
Sanitizar logs antes de compartilhar: a própria imagem do emulador pode escrever sua conexão
local nos logs de startup. Não copiar credenciais para chat, documentação ou commits.

## Referências

- [Extensão Quarkus Service Bus e Dev Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html#dev-services)
- [Emulador do Service Bus](https://learn.microsoft.com/en-us/azure/service-bus-messaging/test-locally-with-service-bus-emulator)
- [Guia de desenvolvimento](guia-desenvolvimento.md)
