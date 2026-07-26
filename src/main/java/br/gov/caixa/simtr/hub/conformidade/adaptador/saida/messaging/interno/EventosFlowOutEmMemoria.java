package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Projeta os eventos produzidos pelo Flow no estado consultado por polling e
 * preserva um histórico técnico mínimo para os testes dos canais internos.
 */
@ApplicationScoped
public class EventosFlowOutEmMemoria implements RegistrarEventoFlowOut {

    private final ArmazenarEstadoAnaliseConformidade estados;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CopyOnWriteArrayList<EventoFlowOutRecebido>> eventos =
            new ConcurrentHashMap<>();

    @Inject
    public EventosFlowOutEmMemoria(
            ArmazenarEstadoAnaliseConformidade estados,
            ObjectMapper objectMapper) {
        this.estados = estados;
        this.objectMapper = objectMapper;
    }

    @Override
    public Uni<Void> registrar(EventoFlowOutRecebido evento) {
        return Uni.createFrom().deferred(() -> {
            ResultadoAnaliseConformidade resultado = lerResultado(evento);
            Uni<Void> projecao = switch (evento.tipo()) {
                case CloudEventMapper.EVENTO_REVISAO_SOLICITADA ->
                    estados.aguardarRevisao(evento.instanceId(), resultado);
                case CloudEventMapper.EVENTO_ANALISE_CONCLUIDA ->
                    estados.concluir(evento.instanceId(), resultado);
                default -> throw new CloudEventInvalidoException(
                        "Tipo de CloudEvent inválido para projeção");
            };
            return projecao.invoke(() ->
                    eventos.computeIfAbsent(
                                    evento.instanceId(),
                                    ignored -> new CopyOnWriteArrayList<>())
                            .add(evento));
        });
    }

    private ResultadoAnaliseConformidade lerResultado(EventoFlowOutRecebido evento) {
        try {
            return objectMapper.treeToValue(
                    evento.dados(),
                    ResultadoAnaliseConformidade.class);
        } catch (JsonProcessingException falha) {
            throw new CloudEventInvalidoException(
                    "Data do CloudEvent não contém resultado de análise válido",
                    falha);
        }
    }

    List<EventoFlowOutRecebido> eventos(String instanceId) {
        var registrados = eventos.get(instanceId);
        return registrados == null ? List.of() : List.copyOf(registrados);
    }

    void limpar() {
        eventos.clear();
    }
}
