# ADR-0003: Orquestração e colaboração por portas

- **Status:** Aceito
- **Decisão em uma frase:** entradas e composições locais acionam portas de aplicação; chamadas
  entre domínios usam porta do consumidor e camada anticorrupção, nunca REST local.
- **Quando consultar:** criação de workflow, orquestrador, composição de capacidades, adapter local
  ou futura separação entre serviços.

## Contexto

REST é uma borda de entrada, não a API interna da aplicação. Fazer um componente local chamar um
Resource do mesmo processo adiciona serialização, validação duplicada e semântica de rede. Acesso
direto ao caso de uso concreto de outro domínio, por outro lado, atravessa a fronteira sem contrato
do consumidor.

## Decisão

- atores externos entram por adapters e portas de entrada;
- um orquestrador local pertence à aplicação e fica atrás de porta própria;
- capacidades do mesmo domínio são compostas por suas portas de entrada;
- outro domínio é acessado por porta de saída do consumidor e camada anticorrupção;
- no monólito, o adapter anticorrupção pode ser local e usar a API pública de aplicação do domínio
  fornecedor;
- após eventual extração, somente esse adapter muda para REST ou mensageria.

A API pública de aplicação contém portas de entrada e os comandos/resultados referenciados por
elas. Resources, REST Clients, adapters e casos de uso concretos não fazem parte dessa API.

## Consequências

- composição não depende de transporte;
- domínios preservam modelos e evolução independentes;
- uma futura extração troca o adapter, não o caso de uso consumidor;
- workflows exigem requisitos e idempotência antes de serem implementados.

## Alternativas rejeitadas

- **Chamar endpoint REST local:** cria acoplamento lateral e falhas artificiais de rede.
- **Importar caso de uso concreto de outro domínio:** ignora contrato e tradução do consumidor.
- **Serviço compartilhado com todas as operações:** amplia dependências por conveniência.
