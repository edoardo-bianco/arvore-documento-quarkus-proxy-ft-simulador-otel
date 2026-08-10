# Checklist: revisar especificação da PoC de conformidade

## Estado

- **Branch:** `docs/revisar-especificacao-poc`
- **Escopo:** documental
- **Próximo item:** CF Revisão humana e encerramento da feature documental

## Checklist

- [x] 0.1 Confirmar intenção e critérios de sucesso;
- [x] 0.2 Ler arquitetura, índice e ADRs aplicáveis;
- [x] 0.3 Inspecionar código, contratos e testes relacionados;
- [x] 0.4 Registrar plano, riscos e checkpoints;
- [x] C0 Registrar autorização documental explícita do usuário;
- [x] 1.1 Reescrever e revisar integralmente a especificação;
- [x] C1 Confrontar C4, schema e localização dos dados com o estado implementado;
- [ ] CF Validar e encerrar a revisão documental.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 | APROVADO | 2026-08-10 | Solicitação explícita de revisão total do documento, incluindo decisões, C4 e schema/localização dos documentos | Usuário |

## Evidências

- nenhuma mudança de código, tooling ou formato derivado faz parte do escopo;
- ADR-0010 deve permanecer `Proposto` até decisão humana explícita e gate real do Cosmos;
- a especificação passou de plano pré-implementação para referência do estado atual, com 18
  decisões arquiteturais identificadas por status e origem;
- o diagrama C4 Component foi conferido contra Resources, casos de uso, Flow, portas, adapters,
  EventPublisher, EventConsumer e dependências externas atuais;
- o schema registra os oito tipos documentais, IDs determinísticos, envelope comum, projeção
  mutável, fatos imutáveis, cursor CouchDB e leases Cosmos;
- a localização lógica e física foi conferida contra `application.properties`,
  `compose-devservices.yml`, `compose-poc.yml` e `k8s/poc`;
- o path da página foi corrigido para
  `src/main/resources/META-INF/resources/poc-conformidade/index.html`; não há uso de
  LocalStorage, SessionStorage ou IndexedDB;
- 14 referências locais do documento resolvem para arquivos existentes e os 52 delimitadores de
  blocos Markdown estão pareados;
- as formulações obsoletas de planejamento não foram encontradas na versão revisada;
- `git diff --check` terminou sem erro, além do aviso esperado de conversão LF/CRLF no Windows;
- Maven, SonarQube e formatos derivados não foram executados ou alterados por se tratar de escopo
  exclusivamente documental.
