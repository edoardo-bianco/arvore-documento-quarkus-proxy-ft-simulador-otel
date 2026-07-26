# ADR-0009: PoC de conformidade com Quarkus Flow e HITL em memória

- **Status:** Aceito
- **Decisão em uma frase:** experimentar a análise de conformidade com Quarkus Flow,
  LangChain4j Agentic, Ollama local e revisão humana por canais internos, mantendo
  estado volátil e sem adicionar broker.
- **Quando consultar:** workflow ou agente no domínio de conformidade, Flow Messaging,
  CloudEvent interno, Human-in-the-Loop, Ollama, estado da PoC ou atualização das
  extensões Flow/LangChain4j.

## Contexto

A PoC precisa consultar o checklist pela porta existente, produzir uma análise
estruturada com um modelo local e sempre permitir revisão humana correlacionada à
instância do fluxo. O repositório permanece em Quarkus `3.33.2.1` e Java 25 e não
possui broker de mensagens.

O projeto de referência `newsletter-drafter` usa Quarkus `3.37.0`, mas resolve Flow
`0.10.2` e Quarkus LangChain4j `1.11.2`. Um spike local comprovou que essas duas
extensões compilam e iniciam sobre Quarkus `3.33.2.1`. Não existe o artefato
`io.quarkus.platform:quarkus-flow-bom:3.33.2.1`; portanto, os BOMs das extensões
precisam ser importados explicitamente.

Flow `0.10.2` só registra sua ponte padrão de Messaging quando há propriedades de
connector para `flow-in` e `flow-out`. Como broker e connector estão fora do escopo,
uma cadeia interna completa exigirá beans da aplicação para essa ponte. Esse detalhe é
específico da versão e não deve se espalhar pelo domínio nem pela aplicação.

## Decisão

Para esta PoC:

- manter Quarkus `3.33.2.1` e Java 25;
- importar `quarkus-flow-bom:0.10.2` e
  `quarkus-langchain4j-bom:1.11.2` ao lado do BOM principal;
- usar `quarkus-flow`, `quarkus-flow-langchain4j`,
  `quarkus-flow-messaging`, `quarkus-langchain4j-agentic`,
  `quarkus-langchain4j-ollama` e `quarkus-messaging`, sem Kafka;
- executar o modelo `llama3.2:3b` em um Ollama local administrado fora da aplicação,
  sem Ollama Dev Service;
- expor ao workflow uma única capacidade agentic sequencial, isolada por porta de
  aplicação, com saída tipada e validação determinística antes do domínio;
- aplicar timeout, retry, circuit breaker e fallback somente no adaptador Ollama,
  preservando o domínio e o workflow dessas dependências;
- manter logs integrais de prompt e resposta desabilitados por padrão e permiti-los
  somente no perfil explícito `%poc`;
- desabilitar o tracing integral do Flow por padrão e emitir somente logs, eventos,
  atributos e spans próprios que não carreguem conteúdo documental ou técnico
  sensível;
- ligar `flow-in` e `flow-out` por canais internos da aplicação, sem propriedade
  `mp.messaging.*.connector`;
- isolar em adaptador de Messaging o shim CDI mínimo que registra
  `FlowMessagingConsumer` e `FlowDomainEventsPublisher` na versão `0.10.2`;
- transportar os eventos de revisão como CloudEvent v1 estruturado e usar a extensão
  `flowinstanceid` como correlação obrigatória;
- manter espera, resultado e revisão somente em memória, sem promessa de retomada
  depois de reinício;
- manter domínio e portas independentes de Flow, LangChain4j, Messaging e Ollama;
- manter a suíte padrão offline, com integração externa do Ollama desabilitada e
  agente falso nos testes de comportamento.

O shim, os canais, o contrato CloudEvent, o store, o agente sequencial, sua política
de Fault Tolerance e a pausa/retomada HITL foram implementados incrementalmente. O
checkpoint C3 aprovou a política do agente, do prompt, do fallback e da telemetria
sanitizada. O incremento HITL complementa no adapter o `source` e o `time` não
fornecidos pelo `emitJson` da versão adotada, preserva a correlação nativa do Flow e
mantém a limitação aprovada de não oferecer entrega durável ou recuperação.

## Consequências

- a PoC exercita a capacidade agentic e o HITL do Flow sem infraestrutura de broker;
- a aplicação passa a depender de BOMs Quarkiverse fixados explicitamente;
- a compatibilidade precisa permanecer protegida por teste de bootstrap e dependency
  tree;
- o shim cria acoplamento controlado a classes do Flow `0.10.2` e deve ser removido
  quando houver suporte oficial equivalente para canais internos sem connector;
- reiniciar a aplicação perde instâncias e revisões pendentes, condição que deve ser
  visível na interface e na documentação da PoC;
- logs completos no perfil `%poc` exigem dados sintéticos e uso consciente porque
  podem conter entrada, prompt e resposta;
- até três tentativas do Ollama podem aproximar a chamada de 181 segundos antes do
  fallback;
- a combinação de entrada pública de até 20.000 caracteres com `num-ctx=2048` é uma
  limitação conhecida da PoC: não haverá truncamento ou particionamento silencioso e
  falha ou resposta truncada seguirá para revisão humana por fallback;
- atualizar Quarkus, Flow ou LangChain4j exige novo spike e revisão desta decisão.

## Alternativas rejeitadas

- **Atualizar o Quarkus para `3.37.0`:** amplia o escopo sem necessidade demonstrada
  pelo spike.
- **Adicionar Kafka ou outro broker:** contradiz o objetivo da PoC e introduz
  infraestrutura desnecessária.
- **Configurar connector apenas para ativar a ponte padrão:** cria uma dependência
  operacional falsa e deixa de validar a cadeia puramente interna.
- **Implementar agora todo o SPI `EventConsumer`/`EventPublisher`:** reduz o acoplamento
  ao shim, mas adiciona mais código do que a PoC precisa; permanece alternativa se o
  shim deixar de ser sustentável.
- **Orquestrar tudo fora do Flow:** não comprova a capacidade Flow/Agentic/HITL exigida
  pela PoC.

## Evidências

- relatório de compatibilidade da feature:
  `tasks/features/poc-conformidade-flow-ollama-hitl/compatibilidade.md`;
- integração oficial Flow–LangChain4j:
  <https://docs.quarkiverse.io/quarkus-flow/dev/langchain4j.html>;
- integração oficial Flow Messaging:
  <https://docs.quarkiverse.io/quarkus-flow/dev/messaging.html>;
- canais internos no Quarkus Messaging:
  <https://quarkus.io/guides/messaging>;
- provider Ollama:
  <https://docs.quarkiverse.io/quarkus-langchain4j/dev/ollama-chat-model.html>.

## Critério de aceitação

O checkpoint humano CA aceitou esta decisão em 2026-07-24, confirmando o limite de
PoC, a volatilidade, o shim versionado e a ausência de precedente automático para
produção. O checkpoint humano C3, também aceito em 2026-07-24, aprovou agente
sequencial, prompt e saída tipada, política de Fault Tolerance, fallback e
observabilidade sem conteúdo sensível.
