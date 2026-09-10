package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto.MonitorarDossieMtrV1;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.LogErroReagendamento.Tipo;

/**
 * Valida e serializa a tentativa decidida pelo monitoramento no contrato v1 da fila de entrada.
 *
 * <p>Preserva identificadores, contador, inicio, limite e versao recebidos. Monta somente o
 * JSON e o envelope AMQP; o modelo calcula a proxima tentativa e o adapter executa a
 * transacao de reagendamento. Falhas reconhecidas geram um log JSON sanitizado e uma
 * excecao local com a mesma identidade, conforme o item 4.1.
 */
@ApplicationScoped
public class MonitoramentoReagendamentoServiceBusMapper {

    private final ObjectMapper json;
    private final Validator validator;
    private final String recurso;

    /**
     * @param json serializador configurado pelo Quarkus
     * @param validator validador do contrato proprio desta borda
     * @param recurso nome da fila de entrada, usado na localizacao do erro
     */
    @Inject
    public MonitoramentoReagendamentoServiceBusMapper(ObjectMapper json, Validator validator,
            @ConfigProperty(name = "monitoramento.service-bus.input-queue") String recurso) {
        this.json = json;
        this.validator = validator;
        this.recurso = recurso;
    }

    /**
     * @param tentativa tentativa ja decidida pelo dominio, sem incremento neste mapper
     * @return mensagem v1 com envelope deterministico e sem horario de agendamento
     * @throws MapeamentoReagendamentoException se o contrato for invalido ou a serializacao falhar
     */
    public ServiceBusMessage paraMensagem(TentativaMonitoramento tentativa) {
        if (tentativa == null) {
            throw LogErroReagendamento.registrar(Tipo.CONTRATO_INVALIDO, recurso, null);
        }
        var contrato = new MonitorarDossieMtrV1(1, tentativa.monitoramentoId(), tentativa.orquestracaoId(),
                tentativa.idDossiePreValidacao(), tentativa.idDossieMtr(), tentativa.tentativaAtual(),
                tentativa.iniciadoEm(), tentativa.limiteEm(), tentativa.politicaMonitoramentoVersao());
        if (!validator.validate(contrato).isEmpty()
                || !contrato.limiteEm().isAfter(contrato.iniciadoEm())) {
            throw LogErroReagendamento.registrar(Tipo.CONTRATO_INVALIDO, recurso, null);
        }

        String corpo;
        try {
            corpo = json.writeValueAsString(contrato);
        } catch (JsonProcessingException causa) {
            throw LogErroReagendamento.registrar(Tipo.SERIALIZACAO_FALHOU, recurso, causa);
        }

        return new ServiceBusMessage(corpo)
                .setMessageId(tentativa.monitoramentoId() + ":tentativa:" + tentativa.tentativaAtual())
                .setCorrelationId(tentativa.orquestracaoId())
                .setSubject("MONITORAR_DOSSIE_MTR")
                .setContentType("application/json");
    }
}
