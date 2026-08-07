package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb;

import com.fasterxml.jackson.databind.JsonNode;

record CouchDbMudanca(String sequencia, JsonNode documento) {

    CouchDbMudanca {
        if (sequencia == null || sequencia.isBlank()) {
            throw new IllegalArgumentException("Sequência CouchDB inválida");
        }
    }
}
