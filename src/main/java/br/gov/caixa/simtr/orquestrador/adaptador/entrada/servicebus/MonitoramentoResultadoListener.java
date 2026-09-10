package br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade.CamposLogJson;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.ReceberResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import io.quarkus.jsonp.JsonProviderHolder;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Consome resultados serialmente e confirma depois da submissao do log pela porta.
 * Inicio explicito ou opt-in no startup; falha de settlement encerra sem segunda liquidacao.
 * Nao garante escrita do log, nao cria/fecha clientes nem altera tentativas por redelivery.
 */
@ApplicationScoped
public class MonitoramentoResultadoListener {
    private static final Logger LOG = Logger.getLogger(MonitoramentoResultadoListener.class.getName());
    private final Instance<ServiceBusReceiverAsyncClient> clientes;
    private final MonitoramentoResultadoServiceBusMapper mapper;
    private final ReceberResultadoMonitoramento recebimento;
    private final Disposable.Swap assinatura = Disposables.swap();
    private Estado estado = Estado.NOVO;

    @Inject
    public MonitoramentoResultadoListener(@FilaSaida Instance<ServiceBusReceiverAsyncClient> clientes,
            MonitoramentoResultadoServiceBusMapper mapper, ReceberResultadoMonitoramento recebimento) {
        this.clientes = clientes;
        this.mapper = mapper;
        this.recebimento = recebimento;
    }

    /** Ativacao da saida independente da flag do listener da entrada. */
    void iniciarNoStartup(@Observes StartupEvent evento,
            @ConfigProperty(name = "monitoramento.service-bus.saida.consumo-habilitado",
                    defaultValue = "false") boolean habilitado) {
        if (habilitado) {
            iniciar();
        }
    }

    /** Inicia uma vez; termino ou falha exigem nova instancia/runtime. */
    public synchronized void iniciar() {
        if (estado != Estado.NOVO) {
            throw new IllegalStateException("Consumo de resultados ja iniciado ou encerrado.");
        }
        estado = Estado.INICIADO;
        try {
            var receiver = clientes.get();
            var fluxo = Flux.defer(receiver::receiveMessages)
                    .concatMap(mensagem -> tratar(receiver, mensagem), 0)
                    .doFinally(_ -> fechar());
            assinatura.update(fluxo.subscribe(_ -> { }, _ -> registrarFalha("consumir")));
        } catch (RuntimeException _) {
            fechar();
            throw new IllegalStateException("Falha ao iniciar consumo de resultados.");
        }
    }

    private Mono<Void> tratar(ServiceBusReceiverAsyncClient receiver, ServiceBusReceivedMessage mensagem) {
        // Recuperacao termina antes do settlement: uma falha nele nao provoca outro settlement.
        return escolherAcao(mensagem).flatMap(acao -> Mono.defer(() -> switch (acao) {
            case COMPLETAR -> receiver.complete(mensagem);
            case ABANDONAR -> receiver.abandon(mensagem);
            case DEAD_LETTER -> receiver.deadLetter(mensagem, new DeadLetterOptions()
                    .setDeadLetterReason("MONITORAMENTO_SAIDA_INVALIDA")
                    .setDeadLetterErrorDescription("Mensagem nao atende ao contrato de resultado."));
        }));
    }

    private Mono<Acao> escolherAcao(ServiceBusReceivedMessage mensagem) {
        return Mono.defer(() -> {
            ResultadoMonitoramento resultado;
            try {
                var corpo = mensagem.getBody();
                resultado = mapper.paraResultado(corpo == null ? null : corpo.toString(),
                        mensagem.getMessageId(), mensagem.getCorrelationId(),
                        mensagem.getSubject(), mensagem.getContentType());
            } catch (MapeamentoResultadoException _) {
                return Mono.just(Acao.DEAD_LETTER);
            }
            return Mono.fromCompletionStage(() -> recebimento.executar(resultado).subscribeAsCompletionStage())
                    .thenReturn(Acao.COMPLETAR);
        }).onErrorResume(_ -> {
            registrarFalha("receber");
            return Mono.just(Acao.ABANDONAR);
        });
    }

    /** Cancela antes do fechamento dos clientes pela fabrica (PLATFORM_AFTER). */
    void encerrar(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) ShutdownEvent evento) {
        fechar();
    }

    @PreDestroy
    synchronized void fechar() {
        estado = Estado.ENCERRADO;
        assinatura.dispose();
    }

    private static void registrarFalha(String operacao) {
        String evento = "orquestrador.monitoramento-dossie.resultado.falhou";
        var campos = JsonProviderHolder.jsonProvider().createObjectBuilder()
                .add("evento", evento).add("camada", "adaptador")
                .add("componente", "MonitoramentoResultadoListener").add("operacao", operacao)
                .add("error_type", "FALHA_TECNICA").build();
        var registro = new ExtLogRecord(Level.ERROR, evento, MonitoramentoResultadoListener.class.getName());
        registro.copyMdc();
        registro.setMarker(new CamposLogJson(campos));
        LOG.log(registro);
    }

    private enum Estado { NOVO, INICIADO, ENCERRADO }

    private enum Acao { COMPLETAR, ABANDONAR, DEAD_LETTER }
}
