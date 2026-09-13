# Execução de 10.1-B1.1 — lifecycle e preparação das regressões

## Estado — 2026-09-11

Pedidos humanos **"10.1-B1"** e **"go"**. GO registrado para o
[desenho da B1 e seus detalhes CP-B1](desenho-10-1-b1.md), incluindo carrier W3C,
atributos/erros controlados e política de falha dos novos logs de publicação.
Executado somente o próximo incremento do checklist: **B1.1, testes**.
B1.2–B1.5 ainda não implementadas; C0.4/C9.1-L preservados.

**B1.1 concluída tecnicamente:** 18 testes focados, 2 integrações A2 e 1.352 testes
padrão em 192 classes passaram, sem falhas/erros/ignorados. Build aprovado e Sonar
COMPLIANT / NOT_REQUIRED. Nenhuma implementação B1.2–B1.5 foi antecipada.

## Referência preservada antes da edição

Branch `feature/orquestrador-monitoramento-service-bus`, com alterações locais anteriores
preservadas. Baseline existente LOCAL_SONAR / READY, original de 217 issues;
baseline e assessment comparados integralmente com o snapshot final da A2 e idênticos.
Fingerprint executável inicial igual ao da A2:

`c82d12e2cdc14d7afaa4ab828690898b4eca95cdff4d4784327ccc980a768ee1`.

Credencial disponível somente por herança do processo. Diretório `sonar/` ausente;
não houve escolha nova de fonte nem reinicialização do baseline.

## Entrega em cinco arquivos executáveis

| Arquivo de teste | Mudança |
|---|---|
| `IniciarMonitoramentoUseCaseTest` | Dois casos de sucesso com cancelamento de um/todos os assinantes; falha depois do cancelamento de todos e repetição tardia; prova de invocações independentes agora com confirmações intercaladas. |
| `MonitoramentoEntradaPublisherTest` | Mesmos limites de cancelamento na fronteira SDK, falha segura memorizada e duas invocações pendentes concluídas fora de ordem. |
| `SuporteTelemetriaMonitoramento` | Centraliza contagem exata da cadeia inicial e dos logs, acrescentando ausência explícita dos eventos de publicação no estado atual. |
| `MonitoramentoTelemetriaEmuladorTest` | Delega as contagens iniciais ao suporte; preserva consulta controlada, modelo final, contexto, carrier, logs e filas vazias. |
| `MonitoramentoTelemetriaHubEmuladorTest` | Delega as contagens ao suporte; preserva três consultas reais, seus atributos/logs, reagendamentos e quarentena. |

Não houve alteração de produção, dependências, configuração operacional ou contratos.
Nenhum novo span/carrier/log foi introduzido. O suporte não aceita presença opcional de
instrumentação futura: terminal exige 1 span/2 logs; fixture exige 4 spans/15 logs.

## Contratos confirmados

- Cancelar um observador mantém o outro pendente até a confirmação e não cancela upstream.
- Cancelar todos os observadores mantém o envio em andamento. ACK posterior fica
  memorizado para assinatura tardia, sem gerar novos IDs ou republicação.
- Falha posterior ao cancelamento também fica memorizada. O caso de uso preserva a
  mesma exceção; o publisher preserva sua exceção segura, sem causa/suppressed originais.
- Duas invocações mantêm confirmações independentes mesmo quando a segunda conclui primeiro.
- As verificações usam futures controlados e observação de cancelamento na porta/SDK,
  sem sleeps ou broker nos testes unitários. Não modificam a política de cancelamento.

São provas do comportamento existente, conforme o fonte Mutiny analisado no desenho;
não houve RED artificial nem correção de produção. RED/GREEN da nova instrumentação
continua previsto para B1.2 em diante.

## Verificações executadas

```powershell
mvn -q "-Dtest=IniciarMonitoramentoUseCaseTest,MonitoramentoEntradaPublisherTest" test
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoTelemetriaEmuladorTest,MonitoramentoTelemetriaHubEmuladorTest" test
./validar-checkpoint-sonarqube.ps1
```

- Focados finais: **18 testes**, 9 em cada classe, sem falhas/erros/ignorados.
- A2 após o refactor: **2 testes**, sem falhas/erros/ignorados.
  Terminal 29,40s; fixture real 28,47s.
- A suíte opt-in completa de 40 cenários não foi repetida nesta subfatia. Foram
  executadas as duas classes afetadas; os 40 da A2 continuam como evidência histórica.
- Revisão local de correção, simplicidade, arquitetura, segurança, desempenho, testes
  e escopo concluída sem achados obrigatórios. Os cinco arquivos finais foram
  comparados com as alterações propostas; demais asserções A2 preservadas.
- Revisão independente solicitada conforme a skill de revisão, mas o agente não
  executou a análise por limite de uso. Não há parecer independente desta subfatia.

### Inventário A2 após o refactor

Os dois artefatos tinham estado VALIDADO ao final das asserções. O inventário foi
conferido antes do clean do checkpoint; este registro preserva a evidência relevante.

| Cenário | Spans / logs | Relações e efeito |
|---|---|---|
| Terminal | 1 / 2 | SERVER com parent remoto conhecido; resultado CONCLUSIVO, uma tentativa, CONFORME, log final sem contexto e filas vazias. |
| Fixture real | 4 / 15 | SERVER + 3 consultas Hub em traces próprios; duas mensagens agendadas, resultado QUARENTENA / MAXIMO_TENTATIVAS / 3 tentativas / Rascunho e filas vazias. |

HTTP terminal: trace `a875296f1b1341358704df30d22baa1d`, span `2840c2cf47739c88`.
HTTP fixture: trace `5230bca8848248e8bac697c060e55708`, span `fc5342d0e334b975`.
Parent remoto em ambos: `2222222222222222`.

Carrier inteiro vazio na entrada terminal e nas tentativas 2/3 observadas na fixture;
ambos os reagendamentos foram vistos em SCHEDULED. Os logs de settlement continuam
referenciando HTTP, sem comprovar cadeia ativa no consumo. As lacunas B2–B5 permanecem.
Não houve chamada a MTR externo ou Azure gerenciado.

## Checkpoint do incremento

Maven clean verify, SonarScanner e Compute Engine concluíram. **1.352 testes padrão
em 192 classes**, sem falhas/erros/ignorados; build SUCCESS. Sonar **COMPLIANT /
NOT_REQUIRED**: **213 issues**, nenhuma nova ou HIGH/BLOCKER/CRITICAL, cobertura
**88,1%**, duplicação **4,4%**. Baseline e assessment originais de **217 issues**
comparados integralmente com a referência A2 e preservados, sem reinicialização.

Checkpoint: 2026-09-11T11:10:19.4054233-03:00.
Análise: `56bc0b08-5023-4fae-a836-5482d4caa89c`.
Compute Engine: `8c72fa54-e6d7-4270-b074-6bb5ad2035ee`.
Fingerprint final conferido:
`d5462fde3863dfe04e3e3214122b076b6be7bed03cc6dc5cf8925dfa75596b05`.
Snapshot preservado:
`.codex/.state/session-after-10-1-b1-1-compliant-20260911.json`.

## Continuidade

Checklist, plano, guia e retomada alinhados ao fechamento técnico. Próximo incremento:
**10.1-B1.2**, usando o GO já recebido para o desenho/CP-B1.
Nenhum novo GO para os mesmos detalhes será solicitado. C3 e encerramento humano da
feature continuam pendentes. Sem staging/commit/push ou formatos derivados.
