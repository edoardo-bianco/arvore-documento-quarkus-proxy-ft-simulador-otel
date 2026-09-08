# Pacote de commit — base do monitoramento Service Bus até 4.1

## Objetivo e autorização

O usuário solicitou commit e push nesta branch para revisar a solução com desenvolvedores,
depois de explicitar no guia a arquitetura, as decisões aceitas e as etapas implementadas/futuras.
O pacote reúne a base técnica e os contratos de 4.1 em um estado já verificado. A publicação
não significa merge, entrega produtiva ou encerramento da feature.

Branch: `feature/orquestrador-monitoramento-service-bus`.
Destino: `origin`, mesma branch, push normal sem force.

Mensagem preparada:

```text
feat: estabelecer base do monitoramento Service Bus ate 4.1

Adiciona politica e configuracao CDI, contratos e mappers independentes
REST/AMQP, logs JSON tipados e guardrails dos novos componentes.

Documenta as decisoes aceitas, o isolamento do Hub e os 19 esqueletos
inativos para continuidade a partir de 5.1. O fluxo completo permanece
pendente de consultas, publishers, listeners e casos de uso.

Validacao: 1046 testes aprovados; Sonar COMPLIANT, cobertura 87,0%
e duplicacao 4,4%. Revisao documental sem alteracao executavel.
```

## Conteúdo selecionado

| Conjunto | Conteúdo |
|---|---|
| Plataforma | `pom.xml`, propriedades principal/teste e `src/main/azure/servicebus-emulator/config.json` |
| Infraestrutura nova | Marcador/decorator/lifecycle dos logs JSON e estrutura inativa de `ClientesServiceBus` |
| Monitoramento/orquestrador | Política/configuração, modelos, contratos/mappers REST/entrada/reagendamento/resultado, erros próprios e estruturas/portas futuras |
| Testes espelhados | Contratos, logs reais, política/CDI/Dev Services, ArchUnit e fixtures positivas/negativas |
| Decisões | ADRs 0010–0012, índice, contextualização do ADR-0009 e consolidado arquitetural |
| Orientação e guias | AGENTS.md, guia Service Bus e guia de desenvolvimento com decisões explícitas |
| Histórico necessário | Tasks da feature, contexto nas tasks do guia anterior e documento amplo de origem referenciado pelos guias |

A seleção cobre os diretórios novos de `src/main/java/br/gov/caixa/simtr/{arquitetura,monitoramento,orquestrador}`
e os testes correspondentes; cada arquivo é conferido no diff preparado. Nenhum arquivo do Hub
ou do package dossie integra uma alteração de implementação neste pacote.

A orientação Azure acrescentada a [AGENTS.md](../../../AGENTS.md) foi revisada e incluída para
a continuidade. A [solução ampla de origem](../../../doc/feat/solucao-duas-filas-azure-service-bus-quarkus-azure-servicebus-reactive-jdk25.md)
também integra o pacote para preservar os links e o contexto. Ela inclui requisitos fora do
recorte; prevalecem o [guia atual](../../../doc/guias/guia-service-bus-amqp-dossie.md),
os ADRs aceitos e o [plano](plan.md).

## Estado que será compartilhado

- 4.1 tecnicamente concluído; contratos das duas filas e regra de quarentena/conclusivo verificados.
- 11 portas, dois records de parâmetros e 19 tipos inativos para as etapas futuras.
- 5.1 ainda não iniciado; não há endpoint, listener, publisher ou processamento funcional novo.
- Última evidência executável: 1046 testes em 171 classes, sem falhas/erros/ignorados;
  checkpoint COMPLIANT de 2026-09-08T11:12:19.3296159-03:00, cobertura 87,0%, duplicação 4,4%.
- A revisão para o commit altera somente Markdown. O [checklist](todo.md) mantém as evidências
  e decisões; o [inventário](guia-desenvolvimento.md) detalha cada classe.

## Conteúdo local preservado fora do commit

O arquivo temporário `.codex-doc-alignment.patch` permanece no workspace. Não é parte da
entrega e não deve ser aplicado por quem clonar a branch. Estado de sessão, token/credenciais,
relatórios/pacotes Sonar, saídas de build e formatos derivados não entram na seleção.

## Verificação e publicação

Conferir os arquivos preparados, referências locais, JSON de exemplo e ausência de credenciais
nos acréscimos; confirmar que os fontes continuam idênticos ao estado já verificado. Depois
criar o commit e executar push normal para a branch acima. Se houver divergência remota,
revisar a história antes de resolver; não sobrescrever alterações de terceiros.

O identificador do commit e a confirmação do push pertencem ao histórico Git. Este documento
descreve o pacote; não antecipa resultado de operação remota.
