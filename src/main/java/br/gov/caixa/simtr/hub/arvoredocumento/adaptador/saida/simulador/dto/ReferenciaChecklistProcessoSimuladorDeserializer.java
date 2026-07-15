package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class ReferenciaChecklistProcessoSimuladorDeserializer
        extends JsonDeserializer<ProcessoParametrizadoSimuladorResponse.ReferenciaChecklist> {

    @Override
    public ProcessoParametrizadoSimuladorResponse.ReferenciaChecklist deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);

        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            if (node.isEmpty()) {
                return null;
            }
            node = node.get(0);
        }
        if (!node.isObject()) {
            return null;
        }

        return new ProcessoParametrizadoSimuladorResponse.ReferenciaChecklist(
                longValue(node.get("identificador_checklist")),
                integerValue(node.get("versao_checklist"))
        );
    }

    private static Long longValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isNumber() ? node.longValue() : Long.valueOf(node.asText());
    }

    private static Integer integerValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isNumber() ? node.intValue() : Integer.valueOf(node.asText());
    }
}
