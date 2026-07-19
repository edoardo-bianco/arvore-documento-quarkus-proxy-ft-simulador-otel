# ADR-0001: Monólito modular e arquitetura hexagonal pragmática

- **Status:** Aceito
- **Decisão em uma frase:** organizar o Hub como monólito modular por domínio, usando portas e
  adapters para proteger responsabilidades sem impor pureza de framework.
- **Quando consultar:** mudanças de packages, camadas, dependências, modularização, APIs de
  framework no núcleo ou regras ArchUnit.

## Contexto

O Hub reúne capacidades relacionadas no mesmo processo e precisa evoluir sem acoplar regras de
negócio a REST, MTR, simulador ou futuros mecanismos de entrada. Separar cada capacidade em um
serviço agora adicionaria rede e operação sem necessidade comprovada. Proibir Quarkus no núcleo,
por outro lado, criaria abstrações artificiais sem melhorar a direção das dependências.

## Decisão

O sistema permanece um monólito modular com **package by domain**. Dentro de cada domínio, a
arquitetura hexagonal separa aplicação e domínio das bordas de entrada e saída.

Quarkus, Jakarta, MicroProfile, Mutiny, Jackson e OpenTelemetry podem ser usados onde cumpram uma
responsabilidade real. A restrição é estrutural: domínio não depende de adapters; aplicação não
depende de DTOs de borda, REST Clients ou outros domínios; adapters apontam para portas e tipos
internos.

Pastas, portas e abstrações só existem para capacidades e consumidores concretos. ArchUnit protege
papéis e direção de dependência, não uma blacklist de tecnologias.

## Consequências

- capacidades podem evoluir e ser extraídas com fronteiras explícitas;
- testes exercitam núcleo sem obrigar transporte real;
- o framework continua disponível sem atravessar papéis arquiteturais;
- uma extração para microsserviço exige decisão própria sobre contrato, consistência e operação.

## Alternativas rejeitadas

- **Organização global por tecnologia:** esconde ownership e acopla capacidades independentes.
- **Núcleo livre de qualquer framework:** custo e indireção sem benefício estrutural comprovado.
- **Microsserviço por domínio agora:** adiciona falhas distribuídas antes de necessidade concreta.
