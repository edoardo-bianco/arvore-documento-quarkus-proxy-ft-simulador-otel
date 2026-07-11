# Planejamento - Ajuste Mockito Java Agent nos testes

Data: 2026-07-10

Status: revisado, aprovado pelo usuario e implementado em 2026-07-10.

## Objetivo

Resolver os warnings emitidos durante `mvn -q test`:

```text
Mockito is currently self-attaching to enable the inline-mock-maker.
WARNING: A Java agent has been loaded dynamically (...byte-buddy-agent...)
WARNING: Dynamic loading of agents will be disallowed by default in a future release
```

O objetivo e evitar o auto-anexo dinamico do Mockito/Byte Buddy durante os testes e preparar o build para JDKs futuros, preservando Quarkus, `@InjectMock`, `quarkus-junit-mockito`, `quarkus-jacoco` e a suite atual.

## Fontes consultadas

- `pom.xml`
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/planejamento-dossie-produto-workflow-v1.md`
- Saida recente de `mvn -q test`
- Documentacao oficial do Mockito indicada pelo proprio warning: `https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3`
- Repositorio Maven local em `C:\Users\edoar\.m2\repository\org\mockito` e `C:\Users\edoar\.m2\repository\net\bytebuddy`

## Analise

O projeto usa:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit-mockito</artifactId>
    <scope>test</scope>
</dependency>
```

E ha testes com:

```java
@InjectMock
```

Tambem ha testes com uso direto de Mockito:

```java
mock(...)
when(...)
verify(...)
```

Com Java 25, o Mockito inline precisa instrumentar classes em runtime. Hoje ele consegue fazer isso por auto-anexo dinamico do agente Byte Buddy. O JDK ainda permite, mas avisa que o carregamento dinamico de agentes sera bloqueado por padrao em versao futura.

O warning nao indica falha funcional da suite. Ele indica que o build de teste ainda depende de um comportamento transitorio do JDK.

O espaco colaborativo registra uma regra importante:

```text
Nao adicionar `argLine` no Surefire sem decisao explicita.
```

Portanto, a correcao deve ser planejada e aprovada antes de alterar `pom.xml`.

## Causa provavel

Durante os testes, `quarkus-junit-mockito`/Mockito usa o inline mock maker. Sem `-javaagent` preconfigurado na JVM de teste, Mockito tenta anexar o agente dinamicamente. O JDK emite warnings porque esse modo sera restringido.

O caminho mostrado no warning:

```text
C:\Users\edoar\.m2\repository\net\bytebuddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar
```

confirma que Byte Buddy esta sendo carregado dinamicamente.

## Alternativas avaliadas

### Alternativa A - Adicionar `-XX:+EnableDynamicAgentLoading`

Exemplo:

```xml
<argLine>-XX:+EnableDynamicAgentLoading</argLine>
```

Vantagem:

- Reduz parte do ruido do JDK.

Problemas:

- Nao resolve a causa.
- Mantem o auto-anexo dinamico.
- Nao prepara o build para o futuro bloqueio do carregamento dinamico.

Decisao proposta:

- Nao usar.

### Alternativa B - Remover Mockito dos testes

Substituir `@InjectMock`, `mock(...)`, `when(...)` e `verify(...)` por fakes, stubs manuais, beans alternativos de teste ou fixtures.

Vantagem:

- Elimina dependencia de Mockito inline.

Problemas:

- Mudanca maior e mais invasiva.
- Pode reduzir clareza nos testes que hoje validam comportamento de resources e filtros.
- Nao e necessaria para resolver o warning.

Decisao proposta:

- Nao fazer neste ciclo. Pode ser avaliado depois como refatoracao de testes.

### Alternativa C - Configurar Mockito como Java agent no Surefire

Configurar a JVM de teste para iniciar com Mockito como agente estatico:

```text
-javaagent:<caminho-para-mockito-core.jar>
```

Vantagens:

- Alinha o build ao recomendado pelo warning do Mockito.
- Evita auto-anexo dinamico em runtime.
- Preserva `quarkus-junit-mockito` e `@InjectMock`.
- Mantem os testes atuais.

Riscos:

- Exige `argLine` no `maven-surefire-plugin`, o que precisa de decisao explicita.
- Pode interagir com instrumentacao de cobertura. O projeto usa `quarkus-jacoco`, nao `jacoco-maven-plugin`, mas a suite precisa confirmar que o relatorio continua sendo gerado.
- Caminho direto para o jar no repositorio Maven local pode ficar fragil se a versao do Mockito mudar.

Decisao proposta:

- Usar esta alternativa, com uma forma robusta de localizar o jar.

## Solucao tecnica proposta

Adicionar `maven-dependency-plugin` para copiar o `mockito-core` resolvido pelo Maven para um caminho estavel em `target` antes da fase de teste:

```text
target/test-agents/mockito-core.jar
```

Em seguida, configurar o `maven-surefire-plugin` com:

```text
-javaagent:${project.build.directory}/test-agents/mockito-core.jar
```

Motivo para copiar o jar:

- evita hardcode de versao como `5.21.0`;
- respeita a versao gerenciada pelo BOM do Quarkus;
- evita depender diretamente de `C:\Users\...\repository\org\mockito\mockito-core\...`.

## Arquivos previstos

Alterar:

- `pom.xml`
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`, se a decisao for consolidada.

Nao alterar:

- Codigo de producao.
- Testes, salvo se algum ajuste pontual for exigido pela configuracao.
- `doc/planejamento-dossie-produto-workflow-v1.md`, porque o endpoint workflow ja esta implementado e este ajuste e transversal de build/testes.

## Plano de implementacao apos aprovacao

1. Adicionar propriedade vazia para preservar compatibilidade com qualquer outro produtor de `argLine`:

```xml
<argLine></argLine>
```

2. Adicionar `maven-dependency-plugin` vinculado a uma fase anterior ao teste, copiando `org.mockito:mockito-core` para:

```text
${project.build.directory}/test-agents/mockito-core.jar
```

3. Atualizar `maven-surefire-plugin`:

```xml
<argLine>@{argLine} -javaagent:${project.build.directory}/test-agents/mockito-core.jar</argLine>
```

4. Preservar:

```xml
<systemPropertyVariables>
    <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
</systemPropertyVariables>
```

5. Executar:

```powershell
mvn -q test
```

6. Verificar:

- suite passa;
- warning `Mockito is currently self-attaching...` desaparece;
- warning `A Java agent has been loaded dynamically` desaparece;
- `target/jacoco-report/index.html` continua sendo gerado;
- nenhum endpoint/teste existente regressa.

7. Atualizar `doc/espaco-colaborativo-de-desenvolvimento.md` com decisao, arquivos alterados e resultado.

8. Atualizar `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` para registrar a configuracao de testes se a decisao for aprovada e validada.

## Criterios de aceite

- `mvn -q test` passa.
- Mockito deixa de fazer self-attach dinamico.
- Warnings de carregamento dinamico do Byte Buddy deixam de aparecer.
- Relatorio do `quarkus-jacoco` continua disponivel em `target/jacoco-report/index.html`.
- A regra "nao adicionar `argLine` no Surefire sem decisao explicita" fica satisfeita por aprovacao do usuario registrada neste planejamento.

## Resultado da implementacao

Implementado em `pom.xml`:

- propriedade vazia `<argLine></argLine>` para preservar compatibilidade com outros produtores de `argLine`;
- `maven-dependency-plugin` copiando `org.mockito:mockito-core` para `target/test-agents/mockito-core.jar`;
- `maven-surefire-plugin` iniciando a JVM de testes com `@{argLine} -javaagent:${project.build.directory}/test-agents/mockito-core.jar`;
- `systemPropertyVariables` do Surefire preservado com `java.util.logging.manager=org.jboss.logmanager.LogManager`.

Validacao executada:

```powershell
mvn -q test
```

Resultado:

- suite passou;
- `target/test-agents/mockito-core.jar` foi gerado;
- `target/jacoco-report/index.html` foi gerado;
- os warnings `Mockito is currently self-attaching...`, `A Java agent has been loaded dynamically` e `Dynamic loading of agents will be disallowed...` nao apareceram na execucao validada;
- permaneceu o aviso `OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended`, que e diferente do auto-anexo dinamico e pode aparecer quando ha agente Java carregado no startup.

## Ponto de aprovacao

Planejamento revisado e aprovado pelo usuario. Implementacao concluida e validada com `mvn -q test`.
