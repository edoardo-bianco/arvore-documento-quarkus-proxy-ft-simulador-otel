package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CriteriosConsultaDocumentosDossieProdutoTest {

    @Test
    void preservaIdentificadorETodosOsFiltrosInformados() {
        var identificador = new IdentificadorDossieProduto(4081899L);

        var criterios = new CriteriosConsultaDocumentosDossieProduto(
                identificador,
                "00000000000000",
                "00000000000",
                1000016488L,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                "192.0.2.10",
                "0001000100069003");

        assertSame(identificador, criterios.identificador());
        assertEquals("00000000000000", criterios.cnpj());
        assertEquals("00000000000", criterios.cpf());
        assertEquals(1000016488L, criterios.fase());
        assertTrue(criterios.incluiArmazenamento());
        assertFalse(criterios.incluiAssinaturas());
        assertTrue(criterios.incluiAtributos());
        assertFalse(criterios.incluiConformidade());
        assertTrue(criterios.incluiOutsourcing());
        assertFalse(criterios.incluiPropriedades());
        assertTrue(criterios.incluiUrl());
        assertEquals("192.0.2.10", criterios.ipUsuario());
        assertEquals("0001000100069003", criterios.tipologia());
    }

    @Test
    void preservaFiltrosAusentesSemAplicarDefaults() {
        var criterios = new CriteriosConsultaDocumentosDossieProduto(
                new IdentificadorDossieProduto(4081900L),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertNull(criterios.cnpj());
        assertNull(criterios.cpf());
        assertNull(criterios.fase());
        assertNull(criterios.incluiArmazenamento());
        assertNull(criterios.incluiAssinaturas());
        assertNull(criterios.incluiAtributos());
        assertNull(criterios.incluiConformidade());
        assertNull(criterios.incluiOutsourcing());
        assertNull(criterios.incluiPropriedades());
        assertNull(criterios.incluiUrl());
        assertNull(criterios.ipUsuario());
        assertNull(criterios.tipologia());
    }
}
