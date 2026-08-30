# Guia para consumir a consulta de documentos do dossiê via porta CDI

## Objetivo

Este guia mostra como um componente no novo package
`br.gov.caixa.simtr.dossie`, dentro do mesmo artifact e runtime Quarkus do `simtr-hub`, deve
consultar os documentos de um dossiê de produto sem chamar o endpoint REST local.

O consumidor usa a porta de entrada existente
`ConsultarDocumentosDossieProduto`. Ele **não** injeta
`ConsultarDocumentosDossieProdutoCasoDeUso`, `DossieProdutoResource`, REST Client, porta de saída
ou adapter.

Este documento é um roteiro. Ele não implementa o novo package e não substitui o plano, o GO e os
checkpoints exigidos antes da alteração de produção.

## Decisão em uma frase

```text
br.gov.caixa.simtr.dossie
    -> ConsultarDocumentosDossieProduto                    porta de entrada
        -> ConsultaDocumentosDossieProdutoObservabilidade  bean CDI
            -> ConsultarDocumentosDossieProdutoCasoDeUso   implementação concreta
                -> ObterDocumentosDossieProduto             porta de saída
                    |-- MTR
                    `-- simulador
```

`ConsultarDocumentosDossieProdutoCasoDeUso` é a implementação da capacidade, não a API que o
novo package deve consumir. A API pública de aplicação é a interface
`ConsultarDocumentosDossieProduto` e os tipos semânticos presentes em sua assinatura.

## Quando este desenho é válido

O desenho é válido porque os dois packages são compilados no mesmo projeto e carregados no mesmo
container Quarkus:

```text
src/main/java/br/gov/caixa/simtr/
|-- dossie/       novo consumidor local
`-- hub/          capacidades existentes do Hub
```

Não há JAR, processo, container ou deploy separado entre eles. Se essa premissa mudar no futuro,
CDI não atravessará a fronteira de processo; será necessário definir um contrato remoto, como REST
ou mensageria.

## Componentes que já existem

| Papel | Tipo existente |
|---|---|
| Porta de entrada a injetar | `ConsultarDocumentosDossieProduto` |
| Critérios internos | `CriteriosConsultaDocumentosDossieProduto` |
| Identificador semântico | `IdentificadorDossieProduto` |
| Resultado interno | `List<DocumentoDossieProdutoConsultado>` |
| Falha interna | `FalhaConsultaDocumentosDossieProduto` |
| Bean CDI que implementa a porta | `ConsultaDocumentosDossieProdutoObservabilidade` |
| Caso de uso envolvido pelo bean | `ConsultarDocumentosDossieProdutoCasoDeUso` |
| Porta de saída selecionada pelo CDI | `ObterDocumentosDossieProduto` |

O bean `ConsultaDocumentosDossieProdutoObservabilidade` é `@ApplicationScoped` e implementa a
porta de entrada. Seu construtor recebe a porta de saída já selecionada pelo producer CDI e cria o
caso de uso concreto. Por isso o consumidor não precisa conhecer o caso de uso nem selecionar MTR
ou simulador.

## Passo 1 — Criar o package no nível correto

Quando a implementação for autorizada, crie o diretório:

```text
src/main/java/br/gov/caixa/simtr/dossie/
```

O arquivo Java começa com:

```java
package br.gov.caixa.simtr.dossie;
```

Não coloque esse consumidor dentro de `br.gov.caixa.simtr.hub.dossieproduto`, pois ele representa
outro módulo lógico consumindo a API pública do Hub.

## Passo 2 — Injetar a interface da porta

O componente real do package `dossie` deve receber a porta por constructor injection. Substitua o
nome genérico abaixo pela responsabilidade real do módulo; não crie uma classe que apenas repita a
porta sem necessidade de negócio.

```java
package br.gov.caixa.simtr.dossie;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada
        .ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo
        .CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo
        .DocumentoDossieProdutoConsultado;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ComponenteDoModuloDossie {

    private final ConsultarDocumentosDossieProduto consultarDocumentos;

    @Inject
    public ComponenteDoModuloDossie(
            ConsultarDocumentosDossieProduto consultarDocumentos
    ) {
        this.consultarDocumentos = consultarDocumentos;
    }

    public Uni<List<DocumentoDossieProdutoConsultado>> consultar(
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        return consultarDocumentos.executar(criterios);
    }
}
```

O projeto usa Quarkus Arc e já possui a dependência `quarkus-arc`. Como a classe está nas fontes da
própria aplicação e possui uma bean-defining annotation (`@ApplicationScoped`), ela participa da
descoberta CDI. O `@Inject` no único construtor é explícito e consistente com o padrão atual do
repositório.

## Passo 3 — Montar os critérios internos

O adapter REST atual converte path e query params para
`CriteriosConsultaDocumentosDossieProduto`. No caminho CDI não há mapper REST; o novo consumidor
deve construir os critérios internos diretamente.

Exemplo apenas com o identificador e sem filtros opcionais:

```java
var criterios = new CriteriosConsultaDocumentosDossieProduto(
        new IdentificadorDossieProduto(4_081_899L),
        null, // cnpj
        null, // cpf
        null, // fase
        null, // incluiArmazenamento
        null, // incluiAssinaturas
        null, // incluiAtributos
        null, // incluiConformidade
        null, // incluiOutsourcing
        null, // incluiPropriedades
        null, // incluiUrl
        null, // ipUsuario
        null  // tipologia
);

return componenteDoModuloDossie.consultar(criterios);
```

Exemplo com filtros:

```java
var criterios = new CriteriosConsultaDocumentosDossieProduto(
        new IdentificadorDossieProduto(4_081_899L),
        null,
        null,
        7L,
        true,
        true,
        true,
        true,
        true,
        true,
        false,
        null,
        "CONTRATO"
);
```

Mantenha `null` quando um filtro não tiver sido informado. No modo MTR, o adapter encaminha apenas
os filtros presentes. O simulador possui cenário determinístico para o identificador `4081899` e
não reproduz a filtragem/projeção do MTR.

Se o package `dossie` receber dados brutos de uma nova borda, essa borda deve validar os dados
antes de criar os tipos internos. A Jakarta Validation declarada no Resource REST não é executada
em uma chamada CDI local.

## Passo 4 — Compor o `Uni` sem bloquear

A porta devolve:

```java
Uni<List<DocumentoDossieProdutoConsultado>>
```

O consumidor deve continuar o fluxo de forma reativa:

```java
return consultarDocumentos.executar(criterios)
        .onItem().transform(documentos -> aplicarRegraDoModulo(documentos));
```

Também é possível encadear outra operação assíncrona:

```java
return consultarDocumentos.executar(criterios)
        .onItem().transformToUni(documentos -> proximaEtapa(documentos));
```

Não use `await().indefinitely()`, `subscribe().with(...)`, `Thread`, `Future#get()` ou outra forma
de bloqueio no código de produção. `await()` permanece aceitável em teste quando for necessário
observar o resultado do `Uni`.

## Passo 5 — Tratar resultado e falha sem semântica HTTP

Na chamada direta não existem status `200` ou `204` nem
`ConsultaDocumentosDossieProdutoResponse`:

- uma consulta com documentos entrega a lista interna;
- uma consulta sem documentos entrega uma lista vazia;
- uma falha é propagada no `Uni`, normalmente como
  `FalhaConsultaDocumentosDossieProduto`;
- somente uma borda HTTP deve converter resultado/falha em status e DTO REST.

O consumidor pode compor ou registrar a falha conforme a responsabilidade real do módulo, sem
expor payload ou dados sensíveis:

```java
return consultarDocumentos.executar(criterios)
        .onFailure(FalhaConsultaDocumentosDossieProduto.class)
        .invoke(falha -> registrarTipoTecnico(falha.getClass().getSimpleName()));
```

Não registre a lista retornada, CPF, CNPJ, nome, matrícula, URL de documento, path de storage,
argumentos dos filtros ou mensagem externa completa.

## O que o CDI resolve em runtime

Ao encontrar o ponto de injeção:

```java
ConsultarDocumentosDossieProduto consultarDocumentos
```

o container resolve o bean não qualificado
`ConsultaDocumentosDossieProdutoObservabilidade`. Esse bean:

1. inicia o span INTERNAL `simtr-hub.service.dossie-produto.documentos.consultar`;
2. registra somente os atributos e eventos já aprovados;
3. chama `ConsultarDocumentosDossieProdutoCasoDeUso`;
4. o caso de uso chama `ObterDocumentosDossieProduto`;
5. o producer CDI seleciona adapter MTR ou simulador pela property
   `simtr-hub.simulador.dossie-produto.habilitado`.

O novo consumidor não deve usar os qualifiers da porta de saída. A seleção permanece encapsulada
no Hub.

Como não há entrada HTTP local nesse caminho, não será criado o span SERVER
`simtr-hub.api.dossie-produto.documentos.consultar`. O span INTERNAL continuará filho do contexto
que estiver ativo no componente chamador; no modo MTR haverá também o span CLIENT existente.

## Código que não deve ser criado

### Não injetar o caso de uso concreto

```java
// Incorreto: acopla o consumidor à implementação e pula o wrapper observável.
@Inject
ConsultarDocumentosDossieProdutoCasoDeUso casoDeUso;
```

### Não instanciar o caso de uso

```java
// Incorreto: obriga o consumidor a conhecer e selecionar a porta de saída.
new ConsultarDocumentosDossieProdutoCasoDeUso(portaSaida);
```

### Não chamar o Resource ou DTO REST

```java
// Incorreto: Resource e DTO são contratos exclusivos da borda HTTP.
DossieProdutoResource resource;
ConsultaDocumentosDossieProdutoResponse response;
```

### Não criar REST Client para o próprio Hub

```java
// Incorreto dentro do mesmo processo: adiciona HTTP, serialização e falhas artificiais.
@RegisterRestClient
interface SimtrHubClient {
    // GET /simtr-hub/v1/dossie-produto/{id}/documentos
}
```

## Testes recomendados antes do GREEN

### Teste unitário do consumidor

Use uma porta falsa ou mock de `ConsultarDocumentosDossieProduto`. O teste deve provar que os
mesmos critérios chegam à porta e que a mesma lista/falha é propagada ou composta pela regra real
do novo módulo.

```java
var porta = mock(ConsultarDocumentosDossieProduto.class);
var componente = new ComponenteDoModuloDossie(porta);
var esperado = List.of(documentoConsultado());

when(porta.executar(criterios)).thenReturn(Uni.createFrom().item(esperado));

var observado = componente.consultar(criterios).await().indefinitely();

assertSame(esperado, observado);
verify(porta).executar(criterios);
```

### Teste de wiring CDI

Um `@QuarkusTest` deve injetar o componente do package `br.gov.caixa.simtr.dossie`. Pode usar
`@InjectMock ConsultarDocumentosDossieProduto` para provar que o consumidor resolve a porta pelo
container sem iniciar HTTP local.

Também mantenha verdes os testes existentes de:

- `ConsultarDocumentosDossieProdutoCasoDeUsoTest`;
- `ConsultaDocumentosDossieProdutoObservabilidadeTest`;
- `ConsultaDocumentosDossieProdutoSelecaoSimuladorQuarkusTest`;
- `ConsultaDocumentosDossieProdutoMtrContractTest`;
- `ArchUnitProgressivoTest`.

### Guardrail arquitetural

Atualmente `ArchUnitProgressivoTest` importa somente `br.gov.caixa.simtr.hub`. Ao implementar o
novo package irmão, amplie a importação de produção para `br.gov.caixa.simtr` e adicione regra que
permita ao package `br.gov.caixa.simtr.dossie` acessar somente:

- portas de entrada públicas necessárias;
- tipos semânticos referenciados por essas portas;
- tipos técnicos genéricos realmente necessários, como `Uni` e CDI.

A regra deve proibir dependência em:

- `..aplicacao.casodeuso..`;
- `..aplicacao.porta.saida..`;
- `..adaptador..`, inclusive REST, MTR e simulador;
- `..recurso..` e REST Clients.

Inclua uma prova negativa que falhe quando uma fixture do package consumidor importar o caso de
uso concreto ou o Resource REST.

## Ordem segura para a futura implementação

1. registrar plano e checkpoints de arquitetura, segurança e comportamento observável;
2. obter GO humano;
3. inicializar o baseline SonarQube conforme `AGENTS.md`;
4. escrever o teste RED do consumidor;
5. criar o componente CDI mínimo no package `br.gov.caixa.simtr.dossie`;
6. escrever o teste de wiring CDI;
7. ampliar o guardrail ArchUnit e sua prova negativa;
8. executar testes focados, suíte completa e checkpoint Sonar do incremento;
9. atualizar a arquitetura consolidada somente depois do código estar implementado;
10. apresentar a evidência e aguardar encerramento humano.

## Checklist rápido

- [ ] O componente está em `br.gov.caixa.simtr.dossie`.
- [ ] Ele é um bean CDI com responsabilidade real.
- [ ] Ele injeta `ConsultarDocumentosDossieProduto`.
- [ ] Ele não importa `ConsultarDocumentosDossieProdutoCasoDeUso`.
- [ ] Ele constrói ou recebe `CriteriosConsultaDocumentosDossieProduto`.
- [ ] Ele preserva o fluxo `Uni` sem bloquear.
- [ ] Lista vazia e falha são tratadas sem inventar status HTTP.
- [ ] Payload e dados sensíveis não são registrados.
- [ ] CDI, MTR/simulador e observabilidade continuam encapsulados no Hub.
- [ ] ArchUnit inclui o novo package irmão e proíbe atalhos arquiteturais.
- [ ] O endpoint REST existente permanece inalterado.

## Referências

- [ADR-0001 — Monólito modular e arquitetura hexagonal pragmática](../adr/0001-monolito-modular-e-hexagonal.md);
- [ADR-0003 — Orquestração e colaboração por portas](../adr/0003-orquestracao-e-colaboracao-por-portas.md);
- [ADR-0004 — Contratos independentes por borda](../adr/0004-contratos-independentes-por-borda.md);
- [arquitetura consolidada](../arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md);
- [Quarkus 3.33 — uso de injeção CDI](https://quarkus.io/version/3.33/guides/getting-started#using-injection);
- [Quarkus — referência CDI e descoberta de beans](https://quarkus.io/guides/cdi-reference#bean_discovery).
