package br.gov.caixa.simtr.monitoramento.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoServiceBusMapper;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

/** Startup CDI por configuracao e REST reais; nao injeta nem inicia o listener pelo teste. */
@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(MonitoramentoAtivacaoEmuladorTest.AtivacaoProfile.class)
class MonitoramentoAtivacaoEmuladorTest {
    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Inject
    @FilaEntrada
    ServiceBusReceiverAsyncClient entrada;
    @Inject
    @FilaSaida
    ServiceBusReceiverAsyncClient saida;
    @Inject
    MonitoramentoResultadoServiceBusMapper consumidor;
    @InjectMock
    ConsultarDossieProduto hub;

    @BeforeEach
    void conferirFilasVaziasSemDisputarConsumoDaEntrada() {
        assertEquals(Boolean.FALSE, entrada.peekMessages(1, 0L).hasElements().block(ESPERA));
        assertEquals(Boolean.FALSE, saida.peekMessages(1, 0L).hasElements().block(ESPERA));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveProcessarPostAposStartupComReagendamentoQuandoNecessario(boolean reagendar) {
        var consultas = new CopyOnWriteArrayList<Instant>();
        when(hub.executar(any())).thenAnswer(_ -> {
            consultas.add(Instant.now());
            var situacao = reagendar && consultas.size() == 1 ? "Rascunho" : "FINALIZADO_CONFORME";
            return Uni.createFrom().item(respostaHub(situacao));
        });

        var solicitacao = given().contentType(ContentType.JSON)
                .body(Map.of("idDossiePreValidacao", "pre-em-analise", "idDossieMtr", "0007"))
                .when().post("/simtr-hub/v1/monitoramentos-dossie")
                .then().statusCode(202).extract().jsonPath();
        var recebida = saida.receiveMessages()
                .concatMap(entrega -> saida.complete(entrega).thenReturn(entrega), 0)
                .next().block(ESPERA);
        assertNotNull(recebida);
        var resultado = consumidor.paraResultado(recebida.getBody().toString(), recebida.getMessageId(),
                recebida.getCorrelationId(), recebida.getSubject(), recebida.getContentType());

        assertEquals(solicitacao.getString("monitoramentoId"), resultado.monitoramentoId());
        assertEquals(solicitacao.getString("orquestracaoId"), resultado.orquestracaoId());
        assertEquals("pre-em-analise", resultado.idDossiePreValidacao());
        assertEquals("0007", resultado.idDossieMtr());
        assertEquals("CONCLUSIVO", resultado.resultadoMonitoramento());
        assertEquals("FINALIZADO_CONFORME", resultado.situacaoMtr());
        assertEquals("CONFORME", resultado.situacaoPreValidacao());
        assertEquals("SITUACAO_CONCLUSIVA_MTR", resultado.motivo());
        assertEquals(reagendar ? 2 : 1, resultado.tentativasRealizadas());
        verify(hub, times(reagendar ? 2 : 1)).executar(new IdentificadorDossieProduto(7L));
        if (reagendar) {
            assertFalse(consultas.get(1).isBefore(consultas.getFirst().plusSeconds(1)));
        }

        var entradaVazia = Mono.defer(() -> entrada.peekMessages(1, 0L).hasElements())
                .repeatWhen(repeticoes -> repeticoes.delayElements(Duration.ofMillis(100)))
                .filter(existe -> !existe).next().block(ESPERA);
        assertEquals(Boolean.FALSE, entradaVazia);
        assertEquals(Boolean.FALSE, saida.peekMessages(1, 0L).hasElements().block(ESPERA));
    }

    private static DossieProdutoConsultado respostaHub(String situacao) {
        return new DossieProdutoConsultado(7L, null, null, null, null, null, null,
                List.of(), null, null, new DossieProdutoConsultado.Situacao(null, situacao, null, null),
                List.of(), List.of());
    }

    public static class AtivacaoProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return new ServiceBusEmuladorTestProfile().getConfigProfile();
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(new ServiceBusEmuladorTestProfile().getConfigOverrides());
            config.put("monitoramento.service-bus.entrada.consumo-habilitado", "true");
            config.put("monitoramento.simulador.prevalidacao.habilitado", "true");
            config.put("monitoramento.politicas.definicoes.padrao.intervalos", "PT1S");
            config.put("monitoramento.politicas.definicoes.padrao.duracao-maxima", "PT1M");
            config.put("monitoramento.politicas.definicoes.padrao.max-tentativas", "3");
            return Map.copyOf(config);
        }
    }
}
