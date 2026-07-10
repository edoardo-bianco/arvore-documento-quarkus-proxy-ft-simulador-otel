package br.gov.caixa.simtr.arvoredocumento.infrastructure.client;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class RequestHeaderFactoryTest {

    @Test
    void adicionaApikeyConfiguradaNoHeaderDeSaida() {
        RequestHeaderFactory factory = new RequestHeaderFactory();
        factory.apikey = "apikey-teste";

        var headers = factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

        assertEquals("apikey-teste", headers.getFirst("apikey"));
        assertFalse(headers.containsKey("authorization"));
    }
}
