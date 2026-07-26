package br.gov.caixa.simtr.hub.conformidade.spike.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class CouchDbChangeEventMapperTest {

    private static final String CHANGE = """
            {
              "seq": "3-g1AAA",
              "id": "revisao:analise-123",
              "changes": [{"rev": "2-acde"}],
              "doc": {
                "_id": "revisao:analise-123",
                "_rev": "2-acde",
                "tipo": "revisao-humana",
                "referencia": "analise-123",
                "hash": "sha256:abc123",
                "parecer": "conteudo-que-nao-deve-ser-publicado"
              }
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CouchDbChangeEventMapper mapper = new CouchDbChangeEventMapper();

    @Test
    void converteMudancaRepetidaNoMesmoCloudEventReferencial() throws Exception {
        var mudanca = objectMapper.readTree(CHANGE);

        var primeiro = mapper.map(mudanca);
        var repetido = mapper.map(mudanca);

        assertEquals(primeiro.getId(), repetido.getId());
        assertEquals(URI.create("urn:simtr:couchdb:conformidade"), primeiro.getSource());
        assertEquals("br.gov.caixa.simtr.conformidade.revisao-referenciada.v1", primeiro.getType());
        assertEquals("analise-123", primeiro.getExtension("referencia"));
        assertEquals("sha256:abc123", primeiro.getExtension("hashdocumento"));
        assertEquals("3-g1AAA", primeiro.getExtension("couchdbseq"));
        assertEquals("2-acde", primeiro.getExtension("couchdbrev"));
        assertNull(primeiro.getData(), "o evento deve carregar apenas referência, hash e metadados");
    }
}
