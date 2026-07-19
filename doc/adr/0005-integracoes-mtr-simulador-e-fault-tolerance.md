# ADR-0005: Integrações MTR, simulador e fault tolerance

- **Status:** Aceito
- **Decisão em uma frase:** MTR e simulador implementam as mesmas portas com contratos próprios;
  fault tolerance e classificação de falhas ficam na chamada MTR interceptada.
- **Quando consultar:** mudanças de REST Client, adapter MTR, simulador, seleção CDI, timeout,
  retry, circuit breaker, fallback ou tradução de erros.

## Contexto

O Hub precisa alternar entre integração real e simulada sem contaminar casos de uso. Ao mesmo
tempo, retry e circuit breaker dependem da classificação da falha vista pelo interceptor; traduzir
o erro cedo demais altera `retryOn`, `abortOn`, `failOn` e `skipOn`.

## Decisão

- portas de saída expressam necessidades da aplicação;
- adapters MTR e simulador implementam essas mesmas portas;
- qualifiers/producers CDI explícitos selecionam uma implementação sem injeção ambígua;
- MTR mantém DTO, mapper, REST Client, erro protocolar e política de fault tolerance próprios;
- simulador lê `JSON -> DTO do simulador -> modelo interno` e não recebe annotations de FT por
  imitação;
- erro MTR permanece protocolar durante a chamada interceptada e só depois é traduzido para falha
  interna lossless.

## Consequências

- o caso de uso ignora fornecedor, URL e modo simulado;
- a matriz de retry/circuit breaker permanece observável e testável;
- falhas internas conservam dados necessários ao contrato público sem expor DTO MTR;
- propriedades e defaults de seleção são parte do comportamento configurável.

## Alternativas rejeitadas

- **Roteador que se injeta ou escolhe a si próprio:** risco de ambiguidade/recursão CDI.
- **DTO MTR no simulador:** fixture passa a depender do fornecedor.
- **Traduzir exceção antes do interceptor:** muda silenciosamente as políticas de FT.
- **Aplicar FT automaticamente ao simulador:** cria comportamento que não representa a borda real.
