package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.gestaodocumento.mock;

import br.gov.caixa.simtr.arvoredocumento.api.dto.gestaodocumento.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.mock.MarkdownJsonMockReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GestaoDocumentoMockFactory {

    private static final String CREDENCIAL_CONTAINER_MOCK_RESOURCE =
            "mock/gestaodocumento/credencial-container.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public GestaoDocumentoMockFactory(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    public GestaoDocumentoCredencialContainerDto gerarCredencialContainerMock() {
        GestaoDocumentoCredencialContainerDto resposta = mockReader.readFirstJsonObject(
                CREDENCIAL_CONTAINER_MOCK_RESOURCE,
                GestaoDocumentoCredencialContainerDto.class
        );

        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + CREDENCIAL_CONTAINER_MOCK_RESOURCE);
        }

        return resposta;
    }
}
