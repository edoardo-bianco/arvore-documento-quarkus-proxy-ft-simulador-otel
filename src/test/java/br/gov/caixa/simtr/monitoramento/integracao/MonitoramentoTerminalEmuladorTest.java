package br.gov.caixa.simtr.monitoramento.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.MonitoramentoEntradaListener;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoServiceBusMapper;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.publisher.Mono;

/** SDK/emulador e composicao CDI reais; somente a porta publica do Hub e controlada. */
@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(MonitoramentoTerminalEmuladorTest.TerminalProfile.class)
class MonitoramentoTerminalEmuladorTest {
    private static final Duration ESPERA = Duration.ofSeconds(30);
    private static final Duration INTERVALO = Duration.ofMillis(100);

    @Inject
    Instance<MonitoramentoEntradaListener> listeners;
    @Inject
    @FilaEntrada
    ServiceBusSenderAsyncClient senderEntrada;
    @Inject
    @FilaEntrada
    ServiceBusReceiverAsyncClient entrada;
    @Inject
    @FilaSaida
    ServiceBusReceiverAsyncClient saida;
    @Inject
    MonitoramentoResultadoServiceBusMapper consumidor;
    @Inject
    ObjectMapper json;
    @Inject
    ServiceBusClientBuilder builder;
    @InjectMock
    ConsultarDossieProduto hub;

    private Instance.Handle<MonitoramentoEntradaListener> listener;
    private ServiceBusReceiverAsyncClient observadorEntrada;
    private ServiceBusReceiverAsyncClient observadorSaida;

    @BeforeEach
    void prepararListenerSemAtivarConsumo() {
        observadorEntrada = criarObservador(entrada.getEntityPath());
        observadorSaida = criarObservador(saida.getEntityPath());
        listener = listeners.getHandle();
        listener.get();
        assertFilaVazia(observadorEntrada);
        assertFilaVazia(observadorSaida);
    }

    @AfterEach
    void encerrarAssinaturaEObservadoresDoTeste() {
        try (var _ = observadorEntrada; var _ = observadorSaida) {
            if (listener != null) {
                listener.destroy();
            }
        }
    }

    private ServiceBusReceiverAsyncClient criarObservador(String fila) {
        // Peeks nao compartilham o lifecycle da assinatura receive/cancel do consumidor.
        return builder.receiver().queueName(fila).receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete().prefetchCount(0).buildAsyncClient();
    }

    @ParameterizedTest
    @CsvSource({
            "FINALIZADO_CONFORME, CONFORME",
            "FINALIZADO_INCONFORME, INCONFORME",
            "PENDENTE_INFORMACA, INCONFORME"
    })
    void deveProcessarSolicitacaoRestPreservandoSituacaoMtr(String original, String calculada) throws Exception {
        when(hub.executar(any())).thenReturn(Uni.createFrom().item(respostaHub(original)));
        var resposta = given().contentType(ContentType.JSON)
                .body(Map.of("idDossiePreValidacao", "pre-em-analise", "idDossieMtr", "0007"))
                .when().post("/simtr-hub/v1/monitoramentos-dossie")
                .then().statusCode(202).extract().jsonPath();
        var enviada = observarEntrada(resposta.getString("monitoramentoId") + ":tentativa:1");
        verificarAntesDoInicio(enviada);
        assertEquals("v1", json.readTree(enviada.getBody().toString()).path("politicaMonitoramentoVersao").asText());

        iniciarListener();
        var resultado = receberResultado(enviada);

        assertEquals("CONCLUSIVO", resultado.resultadoMonitoramento());
        assertEquals(original, resultado.situacaoMtr());
        assertEquals(calculada, resultado.situacaoPreValidacao());
        assertEquals("SITUACAO_CONCLUSIVA_MTR", resultado.motivo());
        assertEquals(1, resultado.tentativasRealizadas());
        verify(hub).executar(new IdentificadorDossieProduto(7L));
        aguardarRemocao(observadorEntrada, enviada.getSequenceNumber());
        assertFilaVazia(observadorSaida);
    }

    @Test
    void deveConcluirNoOpSemConsultarHubOuPublicarSaida() throws Exception {
        var enviada = publicar("pre-conforme", "v1", 1, Instant.now(), Instant.now().plusSeconds(3600));
        verificarAntesDoInicio(enviada);

        iniciarListener();
        aguardarRemocao(observadorEntrada, enviada.getSequenceNumber());

        verifyNoInteractions(hub);
        assertFilaVazia(observadorSaida);
    }

    @Test
    void deveEncerrarPrazoOriginalMesmoComVersaoRemovida() throws Exception {
        var agora = Instant.now();
        var enviada = publicar("pre-em-analise", "versao-removida", 1,
                agora.minusSeconds(7200), agora.minusSeconds(3600));

        iniciarListener();
        var resultado = receberResultado(enviada);

        assertEquals("QUARENTENA", resultado.resultadoMonitoramento());
        assertEquals("QUARENTENA", resultado.situacaoPreValidacao());
        assertEquals("PRAZO_MAXIMO", resultado.motivo());
        assertEquals(0, resultado.tentativasRealizadas());
        assertNull(resultado.situacaoMtr());
        verifyNoInteractions(hub);
        aguardarRemocao(observadorEntrada, enviada.getSequenceNumber());
        assertFilaVazia(observadorSaida);
    }

    @Test
    void deveUsarPoliticaInativaComUmaTentativaAntesDeEncerrar() throws Exception {
        when(hub.executar(any())).thenReturn(Uni.createFrom().item(respostaHub("Rascunho")));
        var agora = Instant.now();
        var enviada = publicar("pre-em-analise", "v-limitada", 1, agora, agora.plusSeconds(3600));

        iniciarListener();
        var resultado = receberResultado(enviada);

        assertEquals("QUARENTENA", resultado.resultadoMonitoramento());
        assertEquals("MAXIMO_TENTATIVAS", resultado.motivo());
        assertEquals("Rascunho", resultado.situacaoMtr());
        assertEquals(1, resultado.tentativasRealizadas());
        verify(hub).executar(new IdentificadorDossieProduto(7L));
        aguardarRemocao(observadorEntrada, enviada.getSequenceNumber());
        assertFilaVazia(observadorSaida);
    }

    @Test
    void deveContinuarComPadraoV1QuandoVersaoRecebidaFoiRemovida() throws Exception {
        when(hub.executar(any())).thenReturn(Uni.createFrom().item(respostaHub("FINALIZADO_CONFORME")));
        var agora = Instant.now();
        var enviada = publicar("pre-em-analise", "versao-removida", 4,
                agora.minusSeconds(3600), agora.plusSeconds(3600));

        iniciarListener();
        var resultado = receberResultado(enviada);

        assertEquals("CONCLUSIVO", resultado.resultadoMonitoramento());
        assertEquals("CONFORME", resultado.situacaoPreValidacao());
        assertEquals(4, resultado.tentativasRealizadas());
        verify(hub).executar(new IdentificadorDossieProduto(7L));
        aguardarRemocao(observadorEntrada, enviada.getSequenceNumber());
        assertFilaVazia(observadorSaida);
    }

    @Test
    void deveAbandonarFalhaTecnicaERedeliverSemIncrementarTentativaFuncional() throws Exception {
        var chamadas = new AtomicInteger();
        var segundaConsulta = new CountDownLatch(1);
        var respostaPendente = new CompletableFuture<DossieProdutoConsultado>();
        when(hub.executar(any())).thenAnswer(_ -> {
            if (chamadas.incrementAndGet() == 1) {
                return Uni.createFrom().failure(new IllegalStateException("falha-transitoria-sintetica"));
            }
            segundaConsulta.countDown();
            return Uni.createFrom().completionStage(respostaPendente);
        });
        var agora = Instant.now();
        var enviada = publicar("pre-em-analise", "v1", 3, agora, agora.plusSeconds(3600));

        iniciarListener();
        try {
            assertTrue(segundaConsulta.await(ESPERA.toSeconds(), TimeUnit.SECONDS));
            var emProcessamento = observadorEntrada.peekMessage(enviada.getSequenceNumber()).block(ESPERA);
            assertNotNull(emProcessamento);
            assertEquals(enviada.getSequenceNumber(), emProcessamento.getSequenceNumber());
            assertEquals(enviada.getBody().toString(), emProcessamento.getBody().toString());
            assertTrue(emProcessamento.getDeliveryCount() > enviada.getDeliveryCount());
            assertFilaVazia(observadorSaida);
        } finally {
            respostaPendente.complete(respostaHub("FINALIZADO_INCONFORME"));
        }

        var resultado = receberResultado(enviada);
        assertEquals("CONCLUSIVO", resultado.resultadoMonitoramento());
        assertEquals("FINALIZADO_INCONFORME", resultado.situacaoMtr());
        assertEquals("INCONFORME", resultado.situacaoPreValidacao());
        assertEquals(3, resultado.tentativasRealizadas());
        verify(hub, times(2)).executar(new IdentificadorDossieProduto(7L));
        aguardarRemocao(observadorEntrada, enviada.getSequenceNumber());
        assertFilaVazia(observadorSaida);
    }

    @Test
    void deveMoverContratoInvalidoParaDlqComDiagnosticoFixo() {
        var mensagem = new ServiceBusMessage("{").setMessageId(UUID.randomUUID().toString());
        senderEntrada.sendMessage(mensagem).block(ESPERA);
        var enviada = observarEntrada(mensagem.getMessageId());

        // Cliente adicional pertence apenas a esta prova; clientes CDI continuam na fabrica.
        try (var dlq = builder.receiver().queueName(entrada.getEntityPath()).subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete().prefetchCount(0)
                .buildAsyncClient();
                var observadorDlq = builder.receiver().queueName(entrada.getEntityPath())
                        .subQueue(SubQueue.DEAD_LETTER_QUEUE).receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                        .disableAutoComplete().prefetchCount(0).buildAsyncClient()) {
            assertFilaVazia(observadorDlq);
            iniciarListener();
            var recebida = receberEConcluir(dlq);

            assertEquals(enviada.getMessageId(), recebida.getMessageId());
            assertEquals("MONITORAMENTO_ENTRADA_INVALIDA", recebida.getDeadLetterReason());
            assertEquals("Mensagem nao atende ao contrato de entrada.", recebida.getDeadLetterErrorDescription());
            verifyNoInteractions(hub);
            aguardarRemocao(observadorEntrada, enviada.getSequenceNumber());
            assertFilaVazia(observadorDlq);
            assertFilaVazia(observadorSaida);
        }
    }

    @ParameterizedTest
    @CsvSource({"Rascunho,QUARENTENA,QUARENTENA,MAXIMO_TENTATIVAS",
            "FINALIZADO_CONFORME,CONCLUSIVO,CONFORME,SITUACAO_CONCLUSIVA_MTR"})
    void deveReagendarProgressivamenteEEncerrarNaUltimaConsulta(
            String ultimaSituacao, String tipo, String situacaoPre, String motivo) throws Exception {
        var consultas = new java.util.concurrent.CopyOnWriteArrayList<Instant>();
        when(hub.executar(any())).thenAnswer(_ -> {
            consultas.add(Instant.now());
            return Uni.createFrom().item(respostaHub(consultas.size() == 5 ? ultimaSituacao : "Rascunho"));
        });
        var inicio = Instant.now();
        var enviada = publicar("pre-em-analise", "v-curta", 1, inicio, inicio.plusSeconds(3600));

        iniciarListener();
        var resultado = receberResultado(enviada, true);

        assertEquals(tipo, resultado.resultadoMonitoramento());
        assertEquals(situacaoPre, resultado.situacaoPreValidacao());
        assertEquals(motivo, resultado.motivo());
        assertEquals(ultimaSituacao, resultado.situacaoMtr());
        assertEquals(5, resultado.tentativasRealizadas());
        verify(hub, times(5)).executar(new IdentificadorDossieProduto(7L));
        var intervalos = List.of(1L, 2L, 3L, 3L);
        for (int i = 1; i < consultas.size(); i++) {
            assertFalse(consultas.get(i).isBefore(consultas.get(i - 1).plusSeconds(intervalos.get(i - 1))),
                    "A proxima consulta deve aguardar o intervalo progressivo.");
        }
        aguardarRemocao(observadorEntrada, resultado.inputSequenceNumber());
        assertFilaVazia(observadorEntrada);
        assertFilaVazia(observadorSaida);
    }

    @Test
    void deveAgendarNoPrazoOriginalQuandoPadraoDeTrintaMinutosNaoCabeNaJanela() throws Exception {
        when(hub.executar(any())).thenReturn(Uni.createFrom().item(respostaHub("Rascunho")));
        var agora = Instant.now();
        var limite = agora.plusSeconds(10);
        var enviada = publicar("pre-em-analise", "versao-removida", 1, agora.minusSeconds(3600), limite);

        iniciarListener();
        var resultado = receberResultado(enviada, true);

        assertEquals("QUARENTENA", resultado.resultadoMonitoramento());
        assertEquals("PRAZO_MAXIMO", resultado.motivo());
        assertEquals(1, resultado.tentativasRealizadas());
        assertFalse(resultado.concluidoEm().isBefore(limite));
        assertNull(resultado.situacaoMtr());
        verify(hub).executar(new IdentificadorDossieProduto(7L));
        aguardarRemocao(observadorEntrada, resultado.inputSequenceNumber());
        assertFilaVazia(observadorEntrada);
        assertFilaVazia(observadorSaida);
    }

    private void iniciarListener() {
        listener.get().iniciar();
    }

    private ServiceBusReceivedMessage publicar(String pre, String versao, int tentativa,
            Instant inicio, Instant limite) throws Exception {
        String monitoramentoId = UUID.randomUUID().toString();
        String orquestracaoId = UUID.randomUUID().toString();
        var corpo = json.writeValueAsString(Map.of(
                "schemaVersion", 1, "monitoramentoId", monitoramentoId, "orquestracaoId", orquestracaoId,
                "idDossiePreValidacao", pre, "idDossieMtr", "0007", "tentativaAtual", tentativa,
                "iniciadoEm", inicio.toString(), "limiteEm", limite.toString(), "politicaMonitoramentoVersao", versao));
        var mensagem = new ServiceBusMessage(corpo).setMessageId(monitoramentoId + ":tentativa:" + tentativa)
                .setCorrelationId(orquestracaoId).setSubject("MONITORAR_DOSSIE_MTR").setContentType("application/json");
        senderEntrada.sendMessage(mensagem).block(ESPERA);
        return observarEntrada(mensagem.getMessageId());
    }

    private ServiceBusReceivedMessage observarEntrada(String messageId) {
        var mensagem = observadorEntrada.peekMessage(0L).block(ESPERA);
        assertNotNull(mensagem);
        assertEquals(messageId, mensagem.getMessageId());
        return mensagem;
    }

    private void verificarAntesDoInicio(ServiceBusReceivedMessage mensagem) {
        verifyNoInteractions(hub);
        assertFilaVazia(observadorSaida);
        assertEquals(mensagem.getMessageId(), observadorEntrada.peekMessage(mensagem.getSequenceNumber()).block(ESPERA).getMessageId());
    }

    private ResultadoMonitoramento receberResultado(ServiceBusReceivedMessage enviada) throws Exception {
        return receberResultado(enviada, false);
    }

    private ResultadoMonitoramento receberResultado(ServiceBusReceivedMessage enviada, boolean reagendada) throws Exception {
        var recebida = receberEConcluir(saida);
        var resultado = consumidor.paraResultado(recebida.getBody().toString(), recebida.getMessageId(),
                recebida.getCorrelationId(), recebida.getSubject(), recebida.getContentType());
        var original = json.readTree(enviada.getBody().toString());
        assertEquals(original.path("monitoramentoId").asText(), resultado.monitoramentoId());
        assertEquals(original.path("orquestracaoId").asText(), resultado.orquestracaoId());
        assertEquals(original.path("idDossiePreValidacao").asText(), resultado.idDossiePreValidacao());
        assertEquals("0007", resultado.idDossieMtr());
        assertEquals(Instant.parse(original.path("iniciadoEm").asText()), resultado.iniciadoEm());
        assertFalse(resultado.concluidoEm().isBefore(resultado.iniciadoEm()));
        if (reagendada) assertTrue(resultado.inputSequenceNumber() > enviada.getSequenceNumber());
        else assertEquals(enviada.getSequenceNumber(), resultado.inputSequenceNumber());
        return resultado;
    }

    private static ServiceBusReceivedMessage receberEConcluir(ServiceBusReceiverAsyncClient receiver) {
        var mensagem = receiver.receiveMessages()
                .concatMap(entrega -> receiver.complete(entrega).thenReturn(entrega), 0).next().block(ESPERA);
        assertNotNull(mensagem);
        return mensagem;
    }

    private static void aguardarRemocao(ServiceBusReceiverAsyncClient receiver, long sequencia) {
        var removida = Mono.defer(() -> receiver.peekMessages(1, sequencia)
                        .all(mensagem -> mensagem.getSequenceNumber() != sequencia))
                .repeatWhen(repeticoes -> repeticoes.delayElements(INTERVALO))
                .filter(Boolean::booleanValue).next().block(ESPERA);
        assertEquals(Boolean.TRUE, removida);
    }

    private static void assertFilaVazia(ServiceBusReceiverAsyncClient receiver) {
        assertEquals(Boolean.FALSE, receiver.peekMessages(1, 0L).hasElements().block(ESPERA));
    }

    private static DossieProdutoConsultado respostaHub(String nome) {
        return new DossieProdutoConsultado(7L, null, null, null, null, null, null,
                List.of(), null, null, new DossieProdutoConsultado.Situacao(null, nome, null, null),
                List.of(), List.of());
    }

    public static class TerminalProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return new ServiceBusEmuladorTestProfile().getConfigProfile();
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(new ServiceBusEmuladorTestProfile().getConfigOverrides());
            config.put("monitoramento.simulador.prevalidacao.habilitado", "true");
            config.put("monitoramento.politicas.definicoes.limitada.versao", "v-limitada");
            config.put("monitoramento.politicas.definicoes.limitada.tipo", "progressiva");
            config.put("monitoramento.politicas.definicoes.limitada.intervalos", "PT30M");
            config.put("monitoramento.politicas.definicoes.limitada.duracao-maxima", "PT24H");
            config.put("monitoramento.politicas.definicoes.limitada.max-tentativas", "1");
            config.put("monitoramento.politicas.definicoes.curta.versao", "v-curta");
            config.put("monitoramento.politicas.definicoes.curta.tipo", "progressiva");
            config.put("monitoramento.politicas.definicoes.curta.intervalos", "PT1S,PT2S,PT3S");
            config.put("monitoramento.politicas.definicoes.curta.duracao-maxima", "PT1H");
            config.put("monitoramento.politicas.definicoes.curta.max-tentativas", "5");
            return Map.copyOf(config);
        }
    }
}
