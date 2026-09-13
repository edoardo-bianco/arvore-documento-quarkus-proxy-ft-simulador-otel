package br.gov.caixa.simtr.orquestrador.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.orquestrador.adaptador.saida.log.ResultadoMonitoramentoLogAdapter;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.publisher.Mono;

/** Dois listeners iniciados pelo startup; pipeline real ate o log e settlement no emulador. */
@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(MonitoramentoResultadoEmuladorTest.ResultadoProfile.class)
class MonitoramentoResultadoEmuladorTest {
    private static final Duration ESPERA = Duration.ofSeconds(30);
    private static final Duration INTERVALO = Duration.ofMillis(100);
    private static final String EVENTO = "orquestrador.monitoramento-dossie.resultado.registrado";
    private static final Path ARQUIVO = Path.of("target/logs/resultado-emulador-test.json");

    @Inject @FilaEntrada ServiceBusReceiverAsyncClient entrada;
    @Inject @FilaSaida ServiceBusReceiverAsyncClient saida;
    @Inject @FilaSaida ServiceBusSenderAsyncClient senderSaida;
    @Inject ServiceBusClientBuilder builder;
    @Inject ObjectMapper json;
    @InjectMock ConsultarDossieProduto hub;
    private int primeiraLinha;

    @BeforeEach
    void conferirFilasVaziasEIniciarCaptura() throws IOException {
        assertFilaVazia(entrada);
        assertFilaVazia(saida);
        primeiraLinha = linhasCompletas().size();
    }

    @Test
    void deveConcluirResultadoTerminalAposStartupEPost() throws Exception {
        verificarFluxo(1, "FINALIZADO_CONFORME");
    }

    @ParameterizedTest
    @CsvSource({"2,FINALIZADO_CONFORME", "3,Rascunho"})
    void deveReagendarAteConclusaoOuQuarentenaERegistrar(int consultas, String ultimaSituacao) throws Exception {
        verificarFluxo(consultas, ultimaSituacao);
    }

    private void verificarFluxo(int quantidade, String ultimaSituacao) throws Exception {
        var consultas = new LinkedBlockingQueue<ConsultaPendente>();
        var respostas = new CopyOnWriteArrayList<CompletableFuture<DossieProdutoConsultado>>();
        when(hub.executar(any())).thenAnswer(_ -> {
            var resposta = new CompletableFuture<DossieProdutoConsultado>();
            respostas.add(resposta);
            consultas.add(new ConsultaPendente(Instant.now(), resposta));
            return Uni.createFrom().completionStage(resposta);
        });
        try {
            var solicitacao = given().contentType(ContentType.JSON)
                    .body(Map.of("idDossiePreValidacao", "pre-em-analise", "idDossieMtr", "0007"))
                    .when().post("/simtr-hub/v1/monitoramentos-dossie")
                    .then().statusCode(202).extract().jsonPath();
            String monitoramentoId = solicitacao.getString("monitoramentoId");
            String orquestracaoId = solicitacao.getString("orquestracaoId");
            var sequencias = new ArrayList<Long>();
            Instant consultaAnterior = null;
            long proximaSequencia = 0L;
            for (int tentativa = 1; tentativa <= quantidade; tentativa++) {
                var consulta = consultas.poll(ESPERA.toSeconds(), TimeUnit.SECONDS);
                assertNotNull(consulta, "A consulta deve ser acionada pelo listener iniciado no startup.");
                var mensagem = entrada.peekMessage(proximaSequencia).block(ESPERA);
                assertNotNull(mensagem);
                assertEquals(monitoramentoId + ":tentativa:" + tentativa, mensagem.getMessageId());
                assertEquals(orquestracaoId, mensagem.getCorrelationId());
                assertEquals(tentativa, json.readTree(mensagem.getBody().toString()).path("tentativaAtual").asInt());
                sequencias.add(mensagem.getSequenceNumber());
                proximaSequencia = mensagem.getSequenceNumber() + 1;
                if (consultaAnterior != null) {
                    assertFalse(consulta.iniciadaEm().isBefore(consultaAnterior.plusSeconds(1)));
                }
                consultaAnterior = consulta.iniciadaEm();
                assertTrue(registros().isEmpty(), "Nao pode haver log final enquanto a consulta esta pendente.");
                consulta.resposta().complete(respostaHub(tentativa == quantidade ? ultimaSituacao : "Rascunho"));
            }
            var registros = Mono.fromCallable(this::registros)
                    .repeatWhen(repeticoes -> repeticoes.delayElements(INTERVALO))
                    .filter(lista -> !lista.isEmpty()).next().block(ESPERA);
            assertNotNull(registros);
            assertEquals(1, registros.size());
            verificarLog(registros.getFirst(), monitoramentoId, orquestracaoId);
            for (long sequencia : sequencias) {
                aguardarRemocaoEntrada(sequencia);
            }
            aguardarSaidaVazia();
            assertFilaVazia(entrada);
            assertEquals(1, registros().size());
            verify(hub, times(quantidade)).executar(new IdentificadorDossieProduto(7L));
        } finally {
            respostas.forEach(resposta -> resposta.complete(respostaHub("FINALIZADO_CONFORME")));
        }
    }

    @Test
    void deveMoverContratoInvalidoDaSaidaParaDlqSemLogFinal() throws Exception {
        // Cliente exclusivo da prova: nunca disputa a fila principal com o listener.
        try (var dlq = builder.receiver().queueName(saida.getEntityPath()).subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete().prefetchCount(0)
                .buildAsyncClient();
                var observadorDlq = builder.receiver().queueName(saida.getEntityPath())
                        .subQueue(SubQueue.DEAD_LETTER_QUEUE).receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                        .disableAutoComplete().prefetchCount(0).buildAsyncClient()) {
            assertFilaVazia(observadorDlq);
            var mensagem = new ServiceBusMessage("{").setMessageId(UUID.randomUUID().toString());
            senderSaida.sendMessage(mensagem).block(ESPERA);
            var movida = Mono.defer(() -> observadorDlq.peekMessage(0L))
                    .repeatWhen(repeticoes -> repeticoes.delayElements(INTERVALO)).next().block(ESPERA);
            assertNotNull(movida);
            assertEquals(mensagem.getMessageId(), movida.getMessageId());
            aguardarSaidaVazia();
            var recebida = dlq.receiveMessages()
                    .concatMap(entrega -> {
                        assertEquals(mensagem.getMessageId(), entrega.getMessageId());
                        return dlq.complete(entrega).thenReturn(entrega);
                    }, 0).next().block(ESPERA);
            assertNotNull(recebida);
            assertEquals("{", recebida.getBody().toString());
            assertEquals("MONITORAMENTO_SAIDA_INVALIDA", recebida.getDeadLetterReason());
            assertEquals("Mensagem nao atende ao contrato de resultado.", recebida.getDeadLetterErrorDescription());
            aguardarSaidaVazia();
            assertFilaVazia(observadorDlq);
            assertFilaVazia(entrada);
            assertTrue(registros().isEmpty());
            verifyNoInteractions(hub);
        }
        verificarFluxo(1, "FINALIZADO_CONFORME");
    }

    private static void verificarLog(JsonNode registroJson, String monitoramentoId, String orquestracaoId) {
        assertEquals("INFO", registroJson.path("level").asText());
        assertEquals(EVENTO, registroJson.path("message").asText());
        assertEquals(ResultadoMonitoramentoLogAdapter.class.getName(), registroJson.path("loggerName").asText());
        var campos = registroJson.path("mdc");
        assertEquals(monitoramentoId, campos.path("monitoramento_id").asText());
        assertEquals(orquestracaoId, campos.path("orquestracao_id").asText());
        assertEquals("adaptador", campos.path("camada").asText());
        assertEquals("ResultadoMonitoramentoLogAdapter", campos.path("componente").asText());
        assertEquals("registrar", campos.path("operacao").asText());
        assertTrue(campos.path("monitoramento_id").isTextual());
        assertTrue(campos.path("orquestracao_id").isTextual());
        assertFalse(registroJson.has("exception"));
        assertFalse(registroJson.has("stacktrace"));
        assertFalse(registroJson.toString().contains("pre-em-analise"));
        assertFalse(registroJson.toString().contains("idDossieMtr"));
        assertFalse(registroJson.toString().contains("situacaoMtr"));
    }

    private List<String> linhasCompletas() throws IOException {
        var conteudo = Files.readString(ARQUIVO);
        // Nao tentar parsear a ultima linha se o handler ainda estiver escrevendo.
        return conteudo.substring(0, conteudo.lastIndexOf('\n') + 1).lines().toList();
    }

    private List<JsonNode> registros() throws IOException {
        var linhas = linhasCompletas();
        var encontrados = new ArrayList<JsonNode>();
        for (String linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registroJson = json.readTree(linha);
            if (EVENTO.equals(registroJson.path("mdc").path("evento").asText())) {
                encontrados.add(registroJson);
            }
        }
        return encontrados;
    }

    private void aguardarRemocaoEntrada(long sequencia) {
        var removida = Mono.defer(() -> entrada.peekMessages(1, sequencia)
                        .all(mensagem -> mensagem.getSequenceNumber() != sequencia))
                .repeatWhen(repeticoes -> repeticoes.delayElements(INTERVALO))
                .filter(Boolean::booleanValue).next().block(ESPERA);
        assertEquals(Boolean.TRUE, removida);
    }

    private void aguardarSaidaVazia() {
        var vazia = Mono.defer(() -> saida.peekMessages(1, 0L).hasElements())
                .repeatWhen(repeticoes -> repeticoes.delayElements(INTERVALO))
                .filter(existe -> !existe).next().block(ESPERA);
        assertEquals(Boolean.FALSE, vazia);
    }

    private static void assertFilaVazia(ServiceBusReceiverAsyncClient receiver) {
        assertEquals(Boolean.FALSE, receiver.peekMessages(1, 0L).hasElements().block(ESPERA));
    }

    private static DossieProdutoConsultado respostaHub(String situacao) {
        return new DossieProdutoConsultado(7L, null, null, null, null, null, null,
                List.of(), null, null, new DossieProdutoConsultado.Situacao(null, situacao, null, null),
                List.of(), List.of());
    }

    private record ConsultaPendente(Instant iniciadaEm, CompletableFuture<DossieProdutoConsultado> resposta) { }

    public static class ResultadoProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return new ServiceBusEmuladorTestProfile().getConfigProfile();
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(new ServiceBusEmuladorTestProfile().getConfigOverrides());
            config.put("monitoramento.service-bus.entrada.consumo-habilitado", "true");
            config.put("monitoramento.service-bus.saida.consumo-habilitado", "true");
            config.put("monitoramento.simulador.prevalidacao.habilitado", "true");
            config.put("monitoramento.politicas.definicoes.padrao.intervalos", "PT1S");
            config.put("monitoramento.politicas.definicoes.padrao.duracao-maxima", "PT1M");
            config.put("monitoramento.politicas.definicoes.padrao.max-tentativas", "3");
            config.put("quarkus.log.file.enabled", "true");
            config.put("quarkus.log.file.path", ARQUIVO.toString());
            config.put("quarkus.log.file.json.enabled", "true");
            config.put("quarkus.log.console.json.enabled", "true");
            return Map.copyOf(config);
        }
    }
}
