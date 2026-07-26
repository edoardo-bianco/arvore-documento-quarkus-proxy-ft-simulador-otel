package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

record EventoFlowOutRecebido(
        String id,
        URI source,
        String tipo,
        OffsetDateTime ocorridoEm,
        String instanceId,
        String taskId,
        JsonNode dados) {

    EventoFlowOutRecebido {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm");
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(dados, "dados");
        dados = dados.deepCopy();
    }

    @Override
    public JsonNode dados() {
        return dados.deepCopy();
    }
}
