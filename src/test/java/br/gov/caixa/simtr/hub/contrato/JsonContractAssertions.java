package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonContractAssertions {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CAMPO_ID_ERRO = "id_erro";
    private static final String UUID_NORMALIZADO = "<uuid>";

    private JsonContractAssertions() {
    }

    static void assertJsonExato(String esperado, JsonNode atual) {
        try {
            assertEquals(JSON.readTree(esperado), atual);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON esperado invalido no teste", e);
        }
    }

    static void assertErroValidacaoExato(JsonNode atual, String... mensagens) {
        assertTrue(uuidValido(atual.path(CAMPO_ID_ERRO).asText()), "id_erro deve ser um UUID");

        ObjectNode normalizado = ((ObjectNode) atual.deepCopy());
        normalizado.put(CAMPO_ID_ERRO, UUID_NORMALIZADO);
        ordenarErros(normalizado);

        ObjectNode esperado = JsonNodeFactory.instance.objectNode();
        esperado.put("codigo_http", 400);
        esperado.put("recurso", "simtr-hub");
        esperado.put(CAMPO_ID_ERRO, UUID_NORMALIZADO);
        esperado.put("codigo_erro", "ARVDOCP0001");
        ArrayNode erros = esperado.putArray("erros");
        for (String mensagem : mensagens) {
            erros.addObject().put("mensagem", mensagem);
        }
        ordenarErros(esperado);

        assertEquals(esperado, normalizado);
    }

    static void assertFingerprint(String esperado, JsonNode atual) {
        assertEquals(esperado, fingerprint(atual), "contrato JSON publico alterado");
    }

    private static String fingerprint(JsonNode node) {
        try {
            byte[] jsonCanonico = JSON.writeValueAsString(canonicalizar(node))
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jsonCanonico));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Nao foi possivel calcular o fingerprint JSON", e);
        }
    }

    private static JsonNode canonicalizar(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objeto = JsonNodeFactory.instance.objectNode();
            List<String> campos = new ArrayList<>();
            node.fieldNames().forEachRemaining(campos::add);
            campos.stream().sorted().forEach(campo -> objeto.set(campo, canonicalizar(node.get(campo))));
            return objeto;
        }

        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            node.forEach(elemento -> array.add(canonicalizar(elemento)));
            return array;
        }

        return node;
    }

    private static void ordenarErros(ObjectNode erro) {
        ArrayNode array = (ArrayNode) erro.path("erros");
        List<JsonNode> mensagens = new ArrayList<>();
        array.forEach(mensagens::add);
        mensagens.sort(Comparator.comparing(JsonNode::toString));
        array.removeAll();
        mensagens.forEach(array::add);
    }

    private static boolean uuidValido(String valor) {
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
