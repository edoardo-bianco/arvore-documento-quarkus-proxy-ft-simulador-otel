package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.ProcessarTentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.MonitoramentoReagendamentoAdapter;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Processa entregas serialmente e executa um settlement depois dos efeitos confirmados.
 * Inicio explicito; reagendamento e Complete usam a mesma transacao da fila de entrada.
 * Nao cria/fecha clientes nem renova prazos.
 */
@ApplicationScoped
public class MonitoramentoEntradaListener {
    private final Instance<ServiceBusReceiverAsyncClient> clientes;
    private final MonitoramentoEntradaServiceBusMapper mapper;
    private final ProcessarTentativaMonitoramento processamento;
    private final MonitoramentoReagendamentoAdapter reagendamentos;
    private final Disposable.Swap assinatura = Disposables.swap();
    private Estado estado = Estado.NOVO;

    @Inject
    public MonitoramentoEntradaListener(@FilaEntrada Instance<ServiceBusReceiverAsyncClient> clientes,
            MonitoramentoEntradaServiceBusMapper mapper, ProcessarTentativaMonitoramento processamento,
            MonitoramentoReagendamentoAdapter reagendamentos) {
        this.clientes = clientes;
        this.mapper = mapper;
        this.processamento = processamento;
        this.reagendamentos = reagendamentos;
    }

    /** Uma inicialização por instância; nenhum observer de startup ativa este recorte. */
    public synchronized void iniciar() {
        if (estado != Estado.NOVO) {
            throw new IllegalStateException("Consumo de monitoramento ja iniciado ou encerrado.");
        }
        estado = Estado.INICIADO;
        try {
            var receiver = clientes.get();
            var fluxo = Flux.defer(receiver::receiveMessages)
                    .concatMap(mensagem -> tratar(receiver, mensagem), 0)
                    .doFinally(_ -> fechar());
            assinatura.update(fluxo.subscribe(_ -> { },
                    _ -> LogListenerMonitoramento.falha("consumir")));
        } catch (RuntimeException _) {
            fechar();
            throw new IllegalStateException("Falha ao iniciar consumo de monitoramento.");
        }
    }

    private Mono<Void> tratar(ServiceBusReceiverAsyncClient receiver, ServiceBusReceivedMessage mensagem) {
        // Recuperação pertence à escolha: falha ao liquidar nunca tenta um segundo settlement.
        return escolherAcao(mensagem).flatMap(acao -> Mono.defer(() -> liquidar(receiver, mensagem, acao)));
    }

    private Mono<Acao> escolherAcao(ServiceBusReceivedMessage mensagem) {
        return Mono.defer(() -> {
            TentativaMonitoramento tentativa;
            try {
                var corpo = mensagem.getBody();
                tentativa = mapper.paraTentativa(corpo == null ? null : corpo.toString(),
                        mensagem.getMessageId(), mensagem.getCorrelationId(),
                        mensagem.getSubject(), mensagem.getContentType());
            } catch (ContratoMonitoramentoInvalidoException _) {
                return Mono.just(Liquidacao.DEAD_LETTER);
            }
            return Mono.fromCompletionStage(() ->
                            processamento.executar(tentativa, mensagem.getSequenceNumber()).subscribeAsCompletionStage())
                    .switchIfEmpty(Mono.error(new IllegalStateException("Decisao de processamento obrigatoria.")))
                    .<Acao>map(decisao -> switch (decisao) {
                        case DecisaoProcessamento.Ignorar _ ->
                            Liquidacao.IGNORAR;
                        case DecisaoProcessamento.ResultadoPublicado _ ->
                            Liquidacao.COMPLETAR;
                        case DecisaoProcessamento.ReagendamentoPendente pendente ->
                            new Agendar(ReagendamentoMonitoramento.aPartirDe(pendente));
                    });
        }).onErrorResume(_ -> {
            LogListenerMonitoramento.falha("processar");
            return Mono.just(Liquidacao.ABANDONAR);
        });
    }

    private Mono<Void> liquidar(ServiceBusReceiverAsyncClient receiver,
            ServiceBusReceivedMessage mensagem, Acao acao) {
        Mono<Void> liquidacao = switch (acao) {
            case Liquidacao.IGNORAR -> {
                LogListenerMonitoramento.decisao("IGNORAR");
                yield receiver.complete(mensagem);
            }
            case Liquidacao.COMPLETAR -> receiver.complete(mensagem);
            case Liquidacao.ABANDONAR -> receiver.abandon(mensagem);
            case Liquidacao.DEAD_LETTER -> receiver.deadLetter(mensagem, new DeadLetterOptions()
                    .setDeadLetterReason("MONITORAMENTO_ENTRADA_INVALIDA")
                    .setDeadLetterErrorDescription("Mensagem nao atende ao contrato de entrada."));
            case Agendar(var pedido) -> {
                LogListenerMonitoramento.decisao("REAGENDAR");
                yield Mono.fromCompletionStage(() -> reagendamentos.associar(receiver, mensagem)
                        .executar(pedido).subscribeAsCompletionStage());
            }
        };
        return liquidacao.doOnSuccess(_ -> LogListenerMonitoramento.settlement(acao.settlement()));
    }

    /** Cancela antes do fechamento dos clientes pela fábrica (PLATFORM_AFTER). */
    void encerrar(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) ShutdownEvent evento) {
        fechar();
    }

    @PreDestroy
    synchronized void fechar() {
        estado = Estado.ENCERRADO;
        assinatura.dispose();
    }

    private enum Estado { NOVO, INICIADO, ENCERRADO }

    private sealed interface Acao {
        String settlement();
    }

    private enum Liquidacao implements Acao {
        IGNORAR("complete"), COMPLETAR("complete"), ABANDONAR("abandon"), DEAD_LETTER("dead_letter");

        private final String settlement;
        Liquidacao(String settlement) {
            this.settlement = settlement;
        }
        @Override
        public String settlement() {
            return settlement;
        }
    }

    private record Agendar(ReagendamentoMonitoramento pedido) implements Acao {
        @Override
        public String settlement() {
            return "complete_transacional";
        }
    }
}
