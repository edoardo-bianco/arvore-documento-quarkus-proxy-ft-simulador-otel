# Pacote de commit — monitoramento Service Bus até 7.1-A

## Objetivo e estado da preparação

Preparar a entrega parcial para o desenvolvedor revisar a arquitetura, reproduzir as provas
já disponíveis e continuar a partir de 7.1-B. O usuário solicitou esta atualização documental
em 2026-09-09, incluindo o guia de desenvolvimento.

**Ponto estável:** 5.1 e 6.1 concluídos, C2-R1 corrigido, C2 aceito e 7.1-A concluída.
O pacote está descrito para revisão; nesta atualização não houve staging, commit ou push.
A publicação da branch não representa merge, fluxo completo ou encerramento da feature.

Branch: `feature/orquestrador-monitoramento-service-bus`.
Destino previsto: `origin`, mesma branch, push normal sem force.

## Base anterior e conteúdo incremental

A entrega anterior até 4.1 está registrada em `84fca5c`, com complemento documental em
`266092f`. Política/configuração v1, contratos, mappers, logs tipados e ADRs 0010–0012
já pertencem a essa base. Não apresentá-los como implementação nova de 7.1-A.

| Incremento acumulado | Conteúdo a conferir no pacote |
|---|---|
| 5.1 — consultas | ACL local do Hub, modelos mínimos, simulador de pré-validação com DTO/mapper próprios e flag de ativação; testes de comportamento, CDI e fronteiras |
| 6.1 — REST → entrada | Parâmetros pela porta/ACL, fábrica de quatro clientes e qualifiers, Resource, iniciação, publisher inicial e DTO próprio de erro REST; testes espelhados |
| Separação de testes | Seleção da tag/profile `servicebus-integration` no `pom.xml`; suíte padrão sem broker e provas explícitas com emulador |
| C2-R1 — isolamento | Profile de integração valida fontes efetivas antes do bootstrap; 11 regressões puras contra configuração externa |
| 7.1-A — publicação da saída | Publisher de resultado conectado à porta CDI, confirmação e falhas sanitizadas; seis testes puros, um teste CDI e duas integrações |
| Guardrails | Inventário atualizado: 11 portas, sete implementações conectadas, dois records de parâmetros funcionais e oito esqueletos inativos |
| Documentação | Consolidado arquitetural, guia Service Bus e tasks da feature: plano, checklist, retomada, guia do dev e evidências de 5.1/6.1/C2/7.1-A |

A conferência deve incluir arquivos rastreados e novos de produção/teste em
`src/main/java/br/gov/caixa/simtr/{arquitetura,monitoramento,orquestrador}` e sua árvore
espelhada de testes. A lista acima orienta a seleção; não equivale a staging de diretórios
inteiros. Hub, package `dossie`, dependências e configuração do emulador permanecem preservados.

## Mensagem proposta

```text
feat: implementar consultas e publicacoes do monitoramento ate 7.1-A

Conecta consultas de pre-validacao/Hub, parametros pela politica e
POST REST com resposta 202 apos confirmacao na fila de entrada.
Implementa fabrica compartilhada e publicacao de resultados na saida.

Separa testes sem broker da integracao explicita e corrige isolamento
do profile em C2-R1. Documenta continuidade em 7.1-B; processamento,
listeners, reagendamento e log final permanecem pendentes.

Validacao: 1172 testes padrao sem broker; seis integracoes separadas.
Sonar COMPLIANT, cobertura 87,4%, duplicacao 4,3%, nenhuma issue nova.
```

A mensagem descreve o conjunto acumulado desde a base publicada. Se a seleção for dividida
em commits coerentes, cada mensagem deve corresponder aos arquivos efetivamente preparados.

## O que o desenvolvedor consegue executar

- Política v1: seleção `padrao`, intervalos de 30 minutos, 3 horas, 4 horas e 6 horas;
  o último se repete, duração máxima de 24 horas e máximo de tentativas não configurado.
- Consultas isoladas de pré-validação simulada e do Hub pela ACL, sem classificação terminal.
- REST → parâmetros → primeira mensagem na entrada, com `202` após confirmação.
- Publicação de resultado conclusivo/quarentena pela porta de saída, demonstrada em testes
  próprios; o processamento automático que a acionará ainda não existe.

O guia operacional para assumir o trabalho é o
[guia de desenvolvimento](guia-desenvolvimento.md#roteiro-para-assumir-a-entrega).
O [guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md) explica o fluxo e os
contratos. O [checklist](todo.md) identifica o progresso e a
[continuidade de 7.1](continuidade-7-1.md) reúne a evidência atual.

## Evidência e limites

| Verificação já executada | Resultado |
|---|---|
| Suíte padrão | 1.172 testes em 184 classes, zero falhas/erros/ignorados, sem broker |
| Integração explícita | Seis testes em três classes no emulador, aprovados separadamente |
| Sonar final de 7.1-A | COMPLIANT / NOT_REQUIRED; cobertura 87,4%, duplicação 4,3%; 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL |
| Checkpoint | 2026-09-09T12:05:06.5544417+00:00; análise `aab863f9-bbc0-4088-aace-e28abdf6382c` |
| Baseline | Original preservado, sem reinicialização |

As integrações passaram antes do ajuste exclusivo de S5778 no teste CDI; esse teste e o
checkpoint completo foram repetidos após a correção. Não houve alteração de produção entre
essas verificações. Esta atualização somente Markdown não executa Maven ou Sonar e não
constitui medição nova.

7.1-B/C/D, 8.1, 9.1, 10.1/C3 e fechamento permanecem pendentes. Não foram verificados fluxo
completo, filas Azure reais ou transação de reagendamento. A publicação de resultado isolada
não prova atomicidade entre envio na saída e Complete da entrada.

## Conteúdo local preservado fora do commit

`.codex-doc-alignment.patch` permanece no workspace e não deve integrar o pacote nem ser
aplicado por quem clonar a branch. Estado local de sessão e baseline, credenciais,
relatórios/pacotes Sonar, saídas de build e formatos derivados não entram na seleção.

O baseline preservado pertence ao workspace desta execução. Quem retomar em outro checkout
deve seguir [AGENTS.md](../../../AGENTS.md) para sua sessão; não tratar o relatório desta
entrega como baseline automaticamente inicializado no novo ambiente.

## Conferência antes da publicação

1. Conferir a branch, o diff e os arquivos novos; preservar alterações locais fora do pacote.
2. Selecionar somente os arquivos correspondentes aos incrementos descritos, incluindo testes
   e documentação. Revisar o diff preparado e os links; excluir credenciais e artefatos locais.
3. Confirmar que o código selecionado corresponde ao estado verificado. Mudança executável
   posterior exige as verificações do incremento; edição somente Markdown exige revisão documental.
4. Ao executar a publicação, criar commit e fazer push normal para a mesma branch. Havendo
   divergência remota, revisar a história antes de resolver; não sobrescrever trabalho alheio.
5. Fornecer ao dev a branch e o hash efetivamente publicado, com links para o guia e a
   continuidade de 7.1. O estado da operação deve vir do resultado real do Git.

O próximo trabalho funcional é 7.1-B, após as definições pendentes de códigos e versão.
O consumo geral precisa de tratamento explícito do ramo não conclusivo antes de ser
habilitado; seu reagendamento será implementado em 8.1. Compartilhar este ponto permite
revisão e continuidade; a demonstração funcional completa depende também de 9.1, e a
verificação de correlação/cenários integrados pertence a 10.1/C3.
