package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.dto.MonitorarDossieMtrV1;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class MonitoramentoEntradaServiceBusMapper {

    private MonitoramentoEntradaServiceBusMapper() {
    }

    static ServiceBusMessage paraMensagem(TentativaMonitoramento tentativa, ObjectMapper json) {
        var contrato = new MonitorarDossieMtrV1(1, tentativa.monitoramentoId(), tentativa.orquestracaoId(),
                tentativa.idDossiePreValidacao(), tentativa.idDossieMtr(), tentativa.tentativaAtual(),
                tentativa.iniciadoEm(), tentativa.limiteEm(), tentativa.politicaMonitoramentoVersao());

        String corpo;
        try {
            corpo = json.writeValueAsString(contrato);
        } catch (JsonProcessingException causa) {
            throw LogErroSerializacaoEntrada.registrar(causa);
        }

        return new ServiceBusMessage(corpo)
                .setMessageId(tentativa.monitoramentoId() + ":tentativa:" + tentativa.tentativaAtual())
                .setCorrelationId(tentativa.orquestracaoId())
                .setSubject("MONITORAR_DOSSIE_MTR")
                .setContentType("application/json");
    }
}
