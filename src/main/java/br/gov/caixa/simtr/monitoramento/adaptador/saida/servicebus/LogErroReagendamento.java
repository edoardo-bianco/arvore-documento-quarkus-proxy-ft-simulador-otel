package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade.CamposLogJson;
import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto.ErroReagendamentoDto;
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

/** Classifica e registra uma falha reconhecida sem enviar a excecao original ao formatter. */
final class LogErroReagendamento {

    private static final String EVENTO = "monitoramento.servicebus.reagendamento.falhou";
    private static final Class<?> COMPONENTE = MonitoramentoReagendamentoServiceBusMapper.class;
    private static final Logger LOG = Logger.getLogger(COMPONENTE.getName());

    enum Tipo {
        CONTRATO_INVALIDO("Contrato invalido na mensagem de reagendamento.", "Contrato v1 nao atendido."),
        SERIALIZACAO_FALHOU("Falha ao serializar a mensagem de reagendamento.", "Falha de serializacao.");

        private final String mensagem;
        private final String detalhe;

        Tipo(String mensagem, String detalhe) {
            this.mensagem = mensagem;
            this.detalhe = detalhe;
        }

        String codigo() {
            return "MONITORAMENTO_REAGENDAMENTO_" + name();
        }
    }

    private LogErroReagendamento() {
    }

    static MapeamentoReagendamentoException registrar(Tipo tipo, String recurso, JsonProcessingException causa) {
        var erro = new ErroReagendamentoDto(recurso, UUID.randomUUID().toString(), tipo.codigo(),
                List.of(new ErroReagendamentoDto.Mensagem(tipo.mensagem)), tipo.detalhe, stackSeguro(causa));
        var json = JsonProviderHolder.jsonProvider();
        var mensagens = json.createArrayBuilder();
        erro.erros().forEach(mensagem -> mensagens.add(json.createObjectBuilder()
                .add("mensagem", mensagem.mensagem())));
        var campos = json.createObjectBuilder()
                .add("evento", EVENTO)
                .add("camada", "adaptador")
                .add("componente", COMPONENTE.getSimpleName())
                .add("operacao", "paraMensagem")
                .add("recurso", erro.recurso())
                .add("id_erro", erro.idErro())
                .add("codigo_erro", erro.codigoErro())
                .add("erros", mensagens)
                .add("detalhe", erro.detalhe());
        if (erro.stacktrace() != null) {
            campos.add("stacktrace", erro.stacktrace());
        }
        var contexto = Span.current().getSpanContext();
        if (contexto.isValid()) {
            campos.add("traceId", contexto.getTraceId()).add("spanId", contexto.getSpanId());
        }
        var registro = new ExtLogRecord(Level.ERROR, EVENTO, LogErroReagendamento.class.getName());
        registro.setLoggerName(COMPONENTE.getName());
        registro.setMarker(new CamposLogJson(campos.build()));
        LOG.log(registro);
        return new MapeamentoReagendamentoException(erro.idErro(), erro.codigoErro(), tipo.mensagem);
    }

    private static String stackSeguro(JsonProcessingException causa) {
        if (causa == null) {
            return null;
        }
        // Mensagem, causas, suppressed e processor podem conter o payload; usar apenas tipo/frames.
        return causa.getClass().getName() + Arrays.stream(causa.getStackTrace())
                .map(frame -> "\n\tat " + frame)
                .collect(Collectors.joining());
    }
}
