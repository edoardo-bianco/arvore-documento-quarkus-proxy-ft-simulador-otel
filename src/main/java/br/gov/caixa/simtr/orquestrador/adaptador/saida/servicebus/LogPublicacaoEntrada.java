package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade.CamposLogJson;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import io.opentelemetry.api.trace.SpanContext;
import io.quarkus.jsonp.JsonProviderHolder;
import java.util.Map;
import java.util.UUID;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/** Campos tecnicos explicitos: nao copia contexto diagnostico nem recebe Throwable. */
final class LogPublicacaoEntrada {
    private static final Class<?> COMPONENTE = MonitoramentoEntradaPublisher.class;
    private static final Logger LOG = Logger.getLogger(COMPONENTE.getName());

    void registrar(TentativaMonitoramento tentativa, SpanContext contexto, String tipoFalha) {
        var evento = "orquestrador.monitoramento-dossie.publicacao."
                + (tipoFalha == null ? "confirmada" : "falhou");
        var campos = JsonProviderHolder.jsonProvider().createObjectBuilder()
                .add("evento", evento).add("camada", "adaptador")
                .add("componente", COMPONENTE.getSimpleName()).add("operacao", "publicar");
        if (tentativa != null) {
            boolean monitoramentoValido = uuidCanonico(tentativa.monitoramentoId());
            boolean orquestracaoValida = uuidCanonico(tentativa.orquestracaoId());
            if (monitoramentoValido) {
                campos.add("monitoramento_id", tentativa.monitoramentoId());
            }
            if (orquestracaoValida) {
                campos.add("orquestracao_id", tentativa.orquestracaoId());
            }
            if (tentativa.tentativaAtual() > 0) {
                campos.add("tentativa_atual", tentativa.tentativaAtual());
                if (monitoramentoValido && orquestracaoValida) {
                    campos.add("message_id", tentativa.monitoramentoId() + ":tentativa:" + tentativa.tentativaAtual());
                }
            }
        }
        if (tipoFalha != null) {
            campos.add("error_type", tipoFalha);
        }
        if (contexto.isValid()) {
            campos.add("traceId", contexto.getTraceId()).add("spanId", contexto.getSpanId());
        }
        var registro = new ExtLogRecord(tipoFalha == null ? Level.INFO : Level.ERROR, evento,
                LogPublicacaoEntrada.class.getName());
        registro.setLoggerName(COMPONENTE.getName());
        registro.setMdc(Map.of());
        registro.setNdc("");
        registro.setMarker(new CamposLogJson(campos.build()));
        LOG.log(registro);
    }

    private static boolean uuidCanonico(String valor) {
        if (valor == null) {
            return false;
        }
        try {
            return UUID.fromString(valor).toString().equalsIgnoreCase(valor);
        } catch (IllegalArgumentException _) {
            return false;
        }
    }
}
