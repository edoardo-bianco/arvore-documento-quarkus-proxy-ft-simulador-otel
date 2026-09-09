# Revisão C2 — primeira fatia vertical

## Aceite humano — 2026-09-09

O usuário declarou: "c2 aceito". Aceite de C2 registrado após a correção C2-R1 e suas
verificações. Antes de prosseguir à 7.1, solicitou explicação do trabalho restante e
estimativa de tempo. 7.1 permanece não iniciado; a feature ainda não foi encerrada.
Ver [estimativa e dependências](plan.md#aceite-c2-e-estimativa-do-trabalho-restante--2026-09-09).

## Correção concluída — 2026-09-09

**C2-R1 corrigido tecnicamente após autorização explícita do usuário.**
11 regressões puras e quatro testes explícitos no emulador passaram; checkpoint completo:
**1.166 testes/182 classes, COMPLIANT, cobertura 87,4%, duplicação 4,3%, nenhuma issue nova
ou grave**. Baseline original preservado. C2 aceito pelo usuário conforme registro acima.

O profile de integração fixa `test` e reconstrói as fontes de configuração do Quarkus
antes de cada bootstrap. Rejeita connection string/namespace efetivos, inclusive por arquivo
ou `%test`, sem expandir expressões; falha de leitura produz mensagem fixa sem causa externa.
A suíte padrão continua sem broker e a escolha emulador/Azure no desenvolvimento permanece igual.

A evidência final está no [checklist](todo.md#correção-c2-r1--evidência-final-em-2026-09-09).
Durante a integração, o SDK registrou erros de settlement com link fechado; os quatro testes
passaram. Essa limitação pertence à caracterização dos futuros listeners e está registrada
no checklist, sem ampliar o ajuste C2-R1.

## Registro da revisão anterior à correção


Revisão técnica em 2026-09-09. **C2 permanece pendente de decisão humana.**
Há um achado obrigatório de isolamento da integração; recomenda-se corrigi-lo antes do aceite.
A conclusão técnica de 6.1 e suas medições anteriores permanecem como registro histórico.
Esta revisão não representa reprovação humana nem novo resultado Sonar.

## C2-R1 — configuração externa pode direcionar a integração ao Azure

**Prioridade P1 / correção necessária.**
[ServiceBusEmuladorTestProfile.java](../../../src/test/java/br/gov/caixa/simtr/arquitetura/infraestrutura/servicebus/ServiceBusEmuladorTestProfile.java),
linhas 11–21, verifica somente `System.getenv` e `System.getProperty` para connection string
e namespace, e depois habilita a extensão e o Dev Services.

Uma connection string carregada de arquivo por `quarkus.config.locations`, sem os quatro
valores consultados diretamente pelo profile, passa por essa proteção. O Quarkus incorpora
essas fontes à configuração efetiva; uma conexão configurada impede o Dev Services de iniciar.
Assim, os clientes dos testes podem apontar para Azure real.

O impacto inclui envio, recebimento e `Complete`: os testes operam nas filas configuradas
e podem consumir mensagens preexistentes. A proteção precisa falhar antes dessas operações.
Não se afirma que isso ocorreu nos quatro testes anteriores.

Conclusão por inspeção de código e documentação oficial, sem reprodução contra Azure:
[Quarkus 3.33 — configuração](https://quarkus.io/version/3.33/guides/config-reference)
documenta fontes adicionais e que valores de `.env` não aparecem em `System.getenv`;
[Quarkus Azure Service Bus — Dev Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
documenta a desativação do emulador quando já existe conexão configurada.

**Divergência documental:** a afirmação de rejeição de configuração externa na
[continuidade de 6.1](continuidade-6-1.md), no [guia](guia-desenvolvimento.md) e no consolidado
é mais ampla que a proteção implementada. O risco e a correção proposta estão no plano;
nenhuma alteração executável ou correção arquitetural foi aplicada nesta revisão.

## Critérios conferidos

| Dimensão | Evidência e conclusão técnica |
|---|---|
| Contrato e confirmação | Resource, caso de uso e publisher aguardam conclusão assíncrona. Testes existentes controlam a confirmação, falhas e uma publicação por invocação compartilhada. |
| IDs e parâmetros | UUIDs/instante gerados no servidor, tentativa inicial 1, política no monitoramento e tradução pela ACL para tipo próprio. |
| Segurança REST | Validação/formato de erro preservados; resposta de falha sem causa do broker. C2-R1 é a ressalva de isolamento da integração. |
| Arquitetura e simplicidade | Portas/ACL preservadas, SDK nas bordas, builder exclusivo da fábrica; nenhum novo acoplamento de negócio identificado no recorte. |
| Lifecycle e desempenho | Quatro clientes duradouros, PEEK_LOCK sem auto-complete, fechamento completo/idempotente e limpeza após falha parcial. Sem bloqueio ou retry novo na publicação. |
| Testes comuns | Surefire exclui a tag de integração; configuração comum desabilita extensão/Dev Services. Escolha do ambiente de desenvolvimento independente da suíte padrão. |
| Telemetria | Sem instrumentação manual nova em 6.1. Caracterização do SDK e correlação completa permanecem em 10.1; sinais de Azure real não foram integralmente verificados. |

Não foram identificados outros defeitos de alta confiança no recorte. A revisão incluiu
uma perspectiva independente sobre fábrica, profiles e integração, conforme a skill
`code-review-and-quality`.

## Evidências e preservação

- Relatórios existentes de `target/surefire-reports` conferidos: **181 classes, 1.155 testes,
  zero falhas, erros ou ignorados**, sem relatórios das duas classes de emulador.
  Foi leitura dos resultados anteriores, sem nova execução.
- Quatro testes separados de integração: evidência anterior em
  [todo.md](todo.md#61--conclusão-técnica-e-ajuste-sonar-em-2026-09-08), não repetida.
- Sonar anterior: **COMPLIANT**, cobertura **87,4%**, duplicação **4,3%**,
  **NOT_REQUIRED**, análise `0b8a179d-1dd9-4e88-bbef-d1fefd914484` de 2026-09-08.
  Sem consulta ao servidor, análise nova ou alteração do baseline.
- Mesma branch e alterações anteriores preservadas. Somente Markdown da pasta da feature
  atualizado nesta sessão; sem Maven, broker, servidor, staging, commit ou push.

## Proposta registrada antes do GO

Corrigir C2-R1 na infraestrutura de testes, garantindo a origem efetiva da conexão do emulador
antes de qualquer operação de cliente, com regressão sem broker para fontes externas de arquivo.
Não basta ampliar uma lista de variáveis nem verificar somente após o envio.
O [plano](plan.md#revisão-c2-e-correção-proposta--2026-09-09) detalha escopo e verificações.

Após o ajuste e suas verificações, reapresentar C2 ao usuário. O item 7.1 continua não iniciado:
ainda depende da tabela oficial id/nome do Hub, da decisão sobre versão da política e do GO
da fatia correspondente.
