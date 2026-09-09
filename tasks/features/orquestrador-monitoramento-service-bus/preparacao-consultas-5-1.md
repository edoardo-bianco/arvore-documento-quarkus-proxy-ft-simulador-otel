# Preparação das consultas — item 5.1

**Status:** C5.1 aprovado pelo usuário em 2026-09-08, mediante “go” após a apresentação
do recorte. Implementação de 5.1 concluída e verificada conforme os detalhes abaixo. O mapeamento dos
estados conclusivos continua pendente para 7.1; não foi inventada tabela id/nome → código.

## Evidência e divergência a resolver

A porta pública do Hub é
[ConsultarDossieProduto](../../../src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/porta/entrada/ConsultarDossieProduto.java):
recebe `IdentificadorDossieProduto(Long)` e retorna `Uni<DossieProdutoConsultado>`.
O resultado tem `situacaoAtual` com `id`, `nome`, data e matrícula.
A [fixture atual](../../../src/main/resources/mock/dossieproduto/4324680-v2-consulta-dossie-produto.md)
fornece situação `1 / Rascunho`. Não há evidência local de uma tabela oficial que relacione
esses valores aos códigos CONFORME, NAO_CONFORME e PENDENTE_INFORMACAO do monitoramento.

**Decisão aprovada e implementada:** a ACL preserva id/nome originais em modelo mínimo próprio, sem normalizar texto,
inventar IDs ou classificar conclusão. A tabela de correspondência será confirmada antes de
7.1; 5.1 pode ser verificado como consulta/tradução independente dessa classificação.
A pergunta sobre a tabela oficial foi apresentada ao usuário; sua ausência não será tratada
como aprovação de um mapeamento.

## Detalhes aprovados no C5.1

| Ponto | Decisão implementada |
|---|---|
| Modelo da pré-validação | `PreValidacaoConsultada(String situacao, boolean simulada)`; situação não vazia; origem simulada identificada no dado retornado |
| Modelo da consulta ao Hub | `SituacaoDossieConsultada(Integer id, String nome)`; preservar situação original, sem dados pessoais, data, matrícula ou objeto completo do Hub |
| Portas | Manter `executar(String)` e os retornos `Uni` existentes; detalhar conclusão/falha no Javadoc |
| Ativação do simulador | Nova property `monitoramento.simulador.prevalidacao.habilitado=false`; testes específicos habilitam por profile e execução local exige ativação explícita |
| Simulador desabilitado | A consulta falha explicitamente; não retorna cenário fictício, não ativa fallback e não impede sozinho a inicialização das capacidades atuais do Hub |
| Cenários de sucesso | `pre-em-analise` → EM_ANALISE_ENVIO_MTR; `pre-conforme` → CONFORME; `pre-nao-conforme` → NAO_CONFORME |
| Pré-validação ausente | Identificador não cadastrado produz falha de consulta, distinta de uma situação não elegível; não gera no-op nem quarentena nesta etapa |
| Dados inválidos do Hub | Resposta/situação ausente ou nome vazio produz falha explícita; não é convertida em resultado funcional |
| Falha do Hub | A ACL mantém falha no fluxo assíncrono, sem fallback, retry adicional ou classificação de settlement; não importa os erros/implementações internos do Hub |
| Diagnóstico | Falhas locais usam texto controlado sem valor rejeitado; nenhum log novo do Hub, body, credencial ou mensagem externa é copiado para diagnóstico novo |

A identificação de origem no modelo não equivale à implementação dos spans/eventos de 10.1.
Os erros existentes do Hub continuam preservados. A futura borda consumidora deverá aplicar
o diagnóstico sanitizado já aprovado e detalhar a classificação de falha em 7.1.

## Sequência de implementação após o checkpoint

1. **5.1a — consulta simulada:** completar o modelo e a borda própria do simulador
   (DTO/mapper e adapter), com cenários determinísticos e testes de resultado/ausência.
2. **5.1b — ativação explícita:** ligar a porta por CDI, introduzir a property proposta e
   provar ativação/desativação por teste com configuração, sem alterar flags do Hub.
3. **5.1c — ACL do Hub:** completar o modelo mínimo, implementar `ConsultarSituacaoDossie`,
   injetar somente `ConsultarDossieProduto`, converter a string MTR validada para Long e
   preservar id/nome. Testar limites, zeros à esquerda, situação desconhecida e falhas.
4. **5.1d — integração e fechamento:** provar a ACL real pelo simulador já existente do Hub,
   retirar somente os quatro tipos implementados de `EstruturaPlanejada.ESQUELETOS`,
   verificar CDI/ArchUnit, regressão e checkpoint Sonar. Atualizar guia e consolidado
   somente conforme a implementação efetivamente verificada.

Cada subfatia deve permanecer pequena, com os arquivos de comportamento e teste indicados
antes da edição. Retirar `@Vetoed` e atualizar o inventário no mesmo incremento da ativação,
preservando a proteção dos outros esqueletos. Não iniciar 6.1 neste recorte.

## Critérios de aceitação e cobertura

- consultas retornam tipos próprios por `Uni`, sem bloqueio ou HTTP local;
- mock desabilitado nunca produz dados simulados; ativado identifica origem e cenário;
- consulta ausente é falha, distinta de consulta bem-sucedida não elegível;
- ACL aceita o intervalo MTR já aprovado, inclusive zeros à esquerda, e rejeita entradas
  inválidas sem chamar o Hub nem expor o valor em diagnóstico;
- ACL preserva id/nome desconhecidos, sem inferir conclusão; não transporta cliente/matrícula;
- falhas assíncronas e resposta inválida não se tornam sucesso;
- teste CDI com Hub real/simulador existente comprova tradução de `4324680 → 1 / Rascunho`;
- guardrails existentes passam sobre a ligação funcional, com isolamento entre bordas;
- cobertura mínima 85%, duplicação máxima 5%, nenhuma nova issue ou HIGH/BLOCKER/CRITICAL;
  qualquer violação requer a decisão humana prevista no processo Sonar.

Comandos após implementação: testes focados pertinentes com `mvn -q "-Dtest=..." test`,
incluindo FronteirasMonitoramentoArchUnitTest, EstruturaMonitoramentoArchUnitTest,
EsqueletosMonitoramentoCdiTest e ArchUnitProgressivoTest; depois
`./validar-checkpoint-sonarqube.ps1` no incremento coerente. Não reinicializar baseline.

## Estado técnico antes da produção

Baseline READY / LOCAL_SONAR, 217 issues, análise
`f6183a72-a2ea-44bc-9374-b2b064bdad55`, integralmente idêntico à referência original.
Credencial presente somente em memória, sem impressão de valor.
Último checkpoint preservado: COMPLIANT em `2026-09-08T11:12:19.3296159-03:00`,
87,0% de cobertura e 4,4% de duplicação; evidência de 1046 testes nas tasks.
Esta preparação não executou testes, análise Sonar ou alteração executável.

Plataforma conferida no pom: Quarkus 3.33.2.1, Java 25, Azure Services 1.2.5.
Referências oficiais consultadas:
[Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html),
[Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
e [CDI Quarkus](https://quarkus.io/guides/cdi-reference/).
Nenhuma dependência, configuração Service Bus, extensão ou recurso Azure novo é proposto.

## Limites

Hub, `dossie`, contratos REST/AMQP, políticas, logs, simuladores atuais e configuração do
emulador permanecem preservados. Factory, Resource, publishers, listeners, settlement,
reagendamento transacional e classificação terminal continuam nos itens posteriores.

## Conclusão técnica de 5.1 — 2026-09-08

Consultas implementadas conforme C5.1, com modelos mínimos próprios, mock desabilitado por
padrão e ACL pela API pública do Hub. Subfatias, 70 casos novos e revisão descritos na
[evidência final do checklist](todo.md#51--implementação-e-verificação-final-em-2026-09-08).
Suíte: 1113 testes/176 classes; checkpoint COMPLIANT, cobertura 87,2%, duplicação 4,4%,
nenhuma issue nova ou HIGH/BLOCKER/CRITICAL. As seis classes têm 100% das linhas/condições.
Baseline preservado. Guias e consolidado atualizados; próximo item 6.1 ainda não iniciado.
As alterações de 5.1 permanecem sem novo commit/push.
