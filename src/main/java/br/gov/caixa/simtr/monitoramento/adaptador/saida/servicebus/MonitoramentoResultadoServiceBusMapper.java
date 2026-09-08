package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto.ResultadoMonitoramentoDossieMtrV1;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.LogErroResultado.Tipo;

/**
 * Monta o JSON v1 e o envelope do resultado ja decidido pelo monitoramento.
 *
 * <p>Nao publica, calcula situacoes, persiste ou conclui entregas. Falhas reconhecidas
 * geram um log sanitizado e excecao propria com a mesma identidade.
 */
@ApplicationScoped
public class MonitoramentoResultadoServiceBusMapper {

    private final ObjectMapper json;
    private final Validator validator;
    private final String recurso;

    /**
     * @param json serializador configurado pelo Quarkus
     * @param validator validador do contrato proprio desta borda
     * @param recurso fila de saida configurada, usada no log
     */
    @Inject
    public MonitoramentoResultadoServiceBusMapper(ObjectMapper json, Validator validator,
            @ConfigProperty(name = "monitoramento.service-bus.output-queue") String recurso) {
        this.json = json;
        this.validator = validator;
        this.recurso = recurso;
    }

    /**
     * @param resultado resultado ja calculado, sem normalizacao dos parametros
     * @return mensagem com envelope deterministico, sem publicacao ou agendamento
     * @throws MapeamentoResultadoException se o contrato ou a serializacao falhar
     */
    public ServiceBusMessage paraMensagem(ResultadoMonitoramento resultado) {
        if (resultado == null) {
            throw falhaContrato();
        }
        var contrato = new ResultadoMonitoramentoDossieMtrV1(1, resultado.monitoramentoId(),
                resultado.orquestracaoId(), resultado.idDossiePreValidacao(), resultado.idDossieMtr(),
                resultado.resultadoMonitoramento(), resultado.situacaoMtr(), resultado.situacaoPreValidacao(),
                resultado.motivo(), resultado.tentativasRealizadas(), resultado.iniciadoEm(),
                resultado.concluidoEm(), resultado.inputSequenceNumber());
        if (!validator.validate(contrato).isEmpty()
                || contrato.concluidoEm().isBefore(contrato.iniciadoEm())) {
            throw falhaContrato();
        }

        String corpo;
        try {
            corpo = json.writeValueAsString(contrato);
        } catch (JsonProcessingException causa) {
            throw LogErroResultado.registrar(Tipo.SERIALIZACAO_FALHOU, "paraMensagem", recurso, causa);
        }
        return new ServiceBusMessage(corpo)
                .setMessageId(resultado.monitoramentoId() + ":resultado:v1")
                .setCorrelationId(resultado.orquestracaoId())
                .setSubject("RESULTADO_MONITORAMENTO_DOSSIE_MTR")
                .setContentType("application/json");
    }

    private MapeamentoResultadoException falhaContrato() {
        return LogErroResultado.registrar(Tipo.CONTRATO_INVALIDO, "paraMensagem", recurso, null);
    }
}
