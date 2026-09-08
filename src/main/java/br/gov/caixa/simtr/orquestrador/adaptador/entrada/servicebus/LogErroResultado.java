package br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade.CamposLogJson;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.dto.ErroResultadoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentelemetry.api.trace.Span;
import io.quarkus.jsonp.JsonProviderHolder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/** Registra a classificacao local uma vez e propaga a identidade sem a excecao original. */
final class LogErroResultado {

    private static final String EVENTO = "orquestrador.servicebus.resultado.falhou";
    private static final Class<?> COMPONENTE = MonitoramentoResultadoServiceBusMapper.class;
    private static final Logger LOG = Logger.getLogger(COMPONENTE.getName());

    enum Tipo {
        JSON_INVALIDO("JSON invalido na mensagem de resultado.", "Falha de desserializacao."),
        ENVELOPE_INVALIDO("Envelope invalido na mensagem de resultado.", "Propriedades AMQP invalidas."),
        CONTRATO_INVALIDO("Contrato invalido na mensagem de resultado.", "Contrato v1 nao atendido.");

        private final String mensagem;
        private final String detalhe;

        Tipo(String mensagem, String detalhe) {
            this.mensagem = mensagem;
            this.detalhe = detalhe;
        }

        String codigo() {
            return "ORQUESTRADOR_RESULTADO_" + name();
        }
    }

    private LogErroResultado() {
    }

    static MapeamentoResultadoException registrar(
            Tipo tipo, String operacao, String recurso, JsonProcessingException causa) {
        var erro = new ErroResultadoDto(recurso, UUID.randomUUID().toString(), tipo.codigo(),
                List.of(new ErroResultadoDto.Mensagem(tipo.mensagem)), tipo.detalhe, stackSeguro(causa));
        var json = JsonProviderHolder.jsonProvider();
        var mensagens = json.createArrayBuilder();
        erro.erros().forEach(mensagem -> mensagens.add(json.createObjectBuilder()
                .add("mensagem", mensagem.mensagem())));
        var campos = json.createObjectBuilder()
                .add("evento", EVENTO)
                .add("camada", "adaptador")
                .add("componente", COMPONENTE.getSimpleName())
                .add("operacao", operacao)
                .add("recurso", erro.recurso())
                .add("id_erro", erro.idErro())
                .add("codigo_erro", erro.codigoErro())
                .add("erros", mensagens)
                .add("detalhe", erro.detalhe());
        if (erro.stacktrace() != null) {
            campos.add("stacktrace", erro.stacktrace());
        }
        if (causa != null && causa.getLocation() != null) {
            var local = causa.getLocation();
            if (local.getLineNr() > 0) {
                campos.add("linha_json", local.getLineNr());
            }
            if (local.getColumnNr() > 0) {
                campos.add("coluna_json", local.getColumnNr());
            }
        }
        var contexto = Span.current().getSpanContext();
        if (contexto.isValid()) {
            campos.add("traceId", contexto.getTraceId()).add("spanId", contexto.getSpanId());
        }
        var registro = new ExtLogRecord(Level.ERROR, EVENTO, LogErroResultado.class.getName());
        registro.setLoggerName(COMPONENTE.getName());
        registro.setMarker(new CamposLogJson(campos.build()));
        LOG.log(registro);
        return new MapeamentoResultadoException(erro.idErro(), erro.codigoErro(), tipo.mensagem);
    }

    private static String stackSeguro(JsonProcessingException causa) {
        if (causa == null) {
            return null;
        }
        // Mensagem, causas, suppressed e processor podem conter o payload.
        return causa.getClass().getName() + Arrays.stream(causa.getStackTrace())
                .map(frame -> "\n\tat " + frame)
                .collect(Collectors.joining());
    }
}
