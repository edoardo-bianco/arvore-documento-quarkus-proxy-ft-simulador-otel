package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class RepositorioDocumentalContratoArquiteturalTest {

    @Test
    void separaVersaoOtimistaDoConteudoDoDocumento() {
        assertDoesNotThrow(() -> RepositorioDocumental.class.getMethod(
                "substituir",
                String.class,
                String.class,
                ObjectNode.class));
    }
}
