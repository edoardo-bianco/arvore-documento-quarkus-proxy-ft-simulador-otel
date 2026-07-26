package br.gov.caixa.simtr.hub.conformidade.spike.persistencia;

import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

final class CouchDbChangeEventMapper {

    private static final URI SOURCE = URI.create("urn:simtr:couchdb:conformidade");
    private static final String TYPE = "br.gov.caixa.simtr.conformidade.revisao-referenciada.v1";

    CloudEvent map(JsonNode change) {
        var changeObject = requireObject(change, "mudança");
        var document = requireObject(changeObject.path("doc"), "documento");
        var documentId = requireText(document, "_id");
        var revision = requireText(document, "_rev");

        return CloudEventBuilder.v1()
                .withId("couchdb:" + documentId + ":" + revision)
                .withSource(SOURCE)
                .withType(TYPE)
                .withSubject(documentId)
                .withExtension("referencia", requireText(document, "referencia"))
                .withExtension("hashdocumento", requireText(document, "hash"))
                .withExtension("couchdbseq", requireText(changeObject, "seq"))
                .withExtension("couchdbrev", revision)
                .build();
    }

    private static JsonNode requireObject(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(field + " deve ser um objeto JSON");
        }
        return node;
    }

    private static String requireText(JsonNode object, String field) {
        var value = object.path(field);
        if (!value.isValueNode() || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("campo obrigatório ausente: " + field);
        }
        return value.asText();
    }
}
