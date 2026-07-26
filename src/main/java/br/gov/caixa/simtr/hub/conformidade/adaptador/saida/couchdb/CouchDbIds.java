package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class CouchDbIds {

    private CouchDbIds() {
    }

    static String entrada(String correlationId) {
        return "entrada-" + sha256(correlationId);
    }

    static String projecao(String instanceId) {
        return "projecao-" + sha256(instanceId);
    }

    static String checklist(String correlationId) {
        return "checklist-" + sha256(correlationId);
    }

    static String resultadoPreliminar(String correlationId) {
        return "resultado-preliminar-" + sha256(correlationId);
    }

    static String revisao(String correlationId) {
        return "revisao-" + sha256(correlationId);
    }

    static String resultadoFinal(String correlationId) {
        return "resultado-final-" + sha256(correlationId);
    }

    static String falha(String correlationId) {
        return "falha-" + sha256(correlationId);
    }

    private static String sha256(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Identificador obrigatório");
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível", impossible);
        }
    }
}
