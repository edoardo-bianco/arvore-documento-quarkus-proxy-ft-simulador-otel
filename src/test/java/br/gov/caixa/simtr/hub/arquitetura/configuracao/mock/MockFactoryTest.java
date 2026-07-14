package br.gov.caixa.simtr.hub.arquitetura.configuracao.mock;

import br.gov.caixa.simtr.hub.gestaodocumento.integracao.mock.GestaoDocumentoMockFactory;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class MockFactoryTest {

    private final MarkdownJsonMockReader reader = new MarkdownJsonMockReader(new ObjectMapper());

    @Test
    void deveLerMockDeCredencialContainerGestaoDocumento() {
        GestaoDocumentoMockFactory factory = new GestaoDocumentoMockFactory(reader);

        GestaoDocumentoCredencialContainerDto resposta = factory.gerarCredencialContainerMock();

        assertNotNull(resposta.sas());
        assertEquals("10/07/2026 18:00:00", resposta.validade());
        assertEquals("https://dossiedigitaldes.blob.core.windows.net", resposta.urlStorage());
        assertEquals("pre-validacao", resposta.nomeContainer());
    }
}
