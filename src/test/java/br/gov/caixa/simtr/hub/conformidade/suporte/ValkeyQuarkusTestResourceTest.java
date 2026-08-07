package br.gov.caixa.simtr.hub.conformidade.suporte;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValkeyQuarkusTestResourceTest {

    @Test
    void preservaValkeyExternoSomenteDuranteProvaDeRestart() {
        assertFalse(ValkeyQuarkusTestResource.deveGerenciarValkey("true"));
        assertTrue(ValkeyQuarkusTestResource.deveGerenciarValkey("false"));
        assertTrue(ValkeyQuarkusTestResource.deveGerenciarValkey(null));
    }
}
