# Preparação da validação final de resultado em 4.1

Status atual: definição confirmada pelo usuário e implementada nas duas bordas.
Os testes de contrato/log e o checkpoint completo passaram; 4.1 está tecnicamente concluído.
Abaixo fica preservada a preparação anterior à confirmação, incluindo a análise de duplicação.
A extração transversal de logging continua sem implementação ou aprovação arquitetural.

## Registro da preparação anterior à confirmação

Status: proposta de contrato pendente de resposta humana. A direção "ok prosseguimos"
retoma a continuidade, mas não foi registrada como aceite da definição de nulidade/contador.
Plano e checklist continuam sendo as fontes de escopo e andamento.

## Evidência no código e no fluxo aprovado

O [plano](plan.md) verifica prazo e tentativas antes da consulta ao MTR. O contrato de entrada
não transporta uma situação MTR anterior. As duas bordas de resultado já validam estrutura,
identificadores, envelope e datas, mas ainda não validam a relação entre resultado, situação e
contador. O consumidor admite situacaoMtr nula/ausente e rejeita valor não textual quando presente.

A questão submetida é permitir QUARENTENA com situacaoMtr=null e tentativasRealizadas=0
quando nenhuma consulta ocorreu, mantendo CONCLUSIVO com uma das situações conclusivas
aprovadas e pelo menos uma tentativa. A resposta ainda é necessária antes de codificar
essas regras de validação do contrato.

## Matriz preparada para verificar a regra após a definição

| Caso | Evidência esperada nas duas bordas |
|---|---|
| CONCLUSIVO, CONFORME, pelo menos uma tentativa | Preservar situação MTR e pré-validação CONFORME |
| CONCLUSIVO, NAO_CONFORME, pelo menos uma tentativa | Preservar as duas situações NAO_CONFORME |
| CONCLUSIVO, PENDENTE_INFORMACAO, pelo menos uma tentativa | Preservar situação MTR e pré-validação calculada NAO_CONFORME |
| CONCLUSIVO sem situação MTR ou com contador zero | Rejeição tipada de contrato, conforme proposta pendente |
| QUARENTENA antes da primeira consulta, situação MTR nula e contador zero | JSON e round-trip preservam ausência de consulta, conforme proposta pendente |
| Contador negativo, string, decimal ou overflow | Rejeição tipada sem payload no log; tipos/overflow já cobertos |
| inputSequenceNumber zero, negativo ou Long.MAX_VALUE | Preservar sequência técnica; não usá-la como contador funcional |
| Erro reconhecido | Um log local tipado e exceção própria com os mesmos id/código |
| Cenários válidos | Nenhum log de erro, publicação, agendamento ou settlement |

Uma situação MTR não informada no resultado não permite ao mapper reconstruir consultas
anteriores. Não exigir uma consulta nova nem inventar estado histórico para satisfazer a validação.
Regras adicionais de combinação devem ser confrontadas com o contrato aprovado antes de restringi-lo.

## Arquivos e verificação do próximo incremento

Alteração restrita aos DTOs/mappers de resultado das próprias bordas e testes existentes de
contrato/log. Não criar DTO, enum ou validador compartilhado entre componentes. A implementação
pode preceder os novos testes, conforme direção humana, mas deve concluir regressão e cobertura.

Depois da resposta: registrar a definição em plan.md/todo.md antes da produção; implementar a
validação local; ampliar os testes parametrizados de contrato e a prova real de erro; executar
regressão focada e um checkpoint completo com o baseline preservado. As evidências anteriores
continuam históricas; nenhuma aprovação futura é presumida.

## Avaliação independente da duplicação

A repetição nos LogErroResultado inclui montagem dos campos JSON, correlação válida e emissão
de ExtLogRecord, além da sanitização local do diagnóstico. CamposLogJson atualmente apenas
transporta JsonObject imutável; o ADR-0012 limita a infraestrutura a marcador, decorator e lifecycle.

Uma proposta futura de emissão técnica deve receber somente campos já sanitizados e os
metadados técnicos necessários ao registro, preservar a categoria e o logger de origem e usar
os handlers existentes. Não deve receber Throwable, gerar identidade, interpretar DTO, escolher
código/mensagem ou sanitizar dados. Essas responsabilidades continuam nas próprias bordas.

A centralização exige contrato técnico concreto e checkpoint arquitetural; não foi implementada.
Não reduzir a métrica por exclusões, mudança do limiar ou alteração cosmética de blocos.
