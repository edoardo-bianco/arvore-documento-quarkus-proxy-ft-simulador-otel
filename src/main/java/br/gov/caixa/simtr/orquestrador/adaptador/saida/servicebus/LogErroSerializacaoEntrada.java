package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade.CamposLogJson;
import br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.dto.ErroSerializacaoEntradaDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentelemetry.api.trace.Span;
import io.quarkus.jsonp.JsonProviderHolder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

final class LogErroSerializacaoEntrada {

    private static final String EVENTO = "orquestrador.servicebus.entrada.falhou";
    private static final String CODIGO = "ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU";
    private static final Class<?> COMPONENTE = MonitoramentoEntradaServiceBusMapper.class;
    private static final Logger LOG = Logger.getLogger(COMPONENTE.getName());

    private LogErroSerializacaoEntrada() {
    }

    static SerializacaoEntradaException registrar(JsonProcessingException causa) {
        var recurso = ConfigProvider.getConfig().getValue("monitoramento.service-bus.input-queue", String.class);
        var erro = new ErroSerializacaoEntradaDto(recurso, UUID.randomUUID().toString(), CODIGO,
                List.of(new ErroSerializacaoEntradaDto.Mensagem("Falha ao serializar a mensagem de entrada.")),
                "Falha de serializacao.", stackSeguro(causa));
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
                .add("detalhe", erro.detalhe())
                .add("stacktrace", erro.stacktrace());
        var contexto = Span.current().getSpanContext();
        if (contexto.isValid()) {
            campos.add("traceId", contexto.getTraceId()).add("spanId", contexto.getSpanId());
        }
        var registro = new ExtLogRecord(Level.ERROR, EVENTO, LogErroSerializacaoEntrada.class.getName());
        registro.setLoggerName(COMPONENTE.getName());
        registro.setMarker(new CamposLogJson(campos.build()));
        LOG.log(registro);
        return new SerializacaoEntradaException(erro.idErro(), erro.codigoErro());
    }

    private static String stackSeguro(JsonProcessingException causa) {
        // Mensagem, causas, suppressed e processor podem carregar dados do contrato.
        return causa.getClass().getName() + Arrays.stream(causa.getStackTrace())
                .map(frame -> "\n\tat " + frame)
                .collect(Collectors.joining());
    }
}
