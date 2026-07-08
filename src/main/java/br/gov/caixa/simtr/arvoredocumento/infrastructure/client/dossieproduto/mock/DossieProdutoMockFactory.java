package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto.mock;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.mock.MarkdownJsonMockReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DossieProdutoMockFactory {

    private static final String MOCK_RESOURCE =
            "mock/dossieproduto/criacao-basica-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public DossieProdutoMockFactory(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    public DossieProdutoCriadoDto criarDossieProdutoMock(DossieProdutoCriacaoDto requisicao) {
        DossieProdutoCriadoDto resposta = mockReader.readFirstJsonObject(MOCK_RESOURCE, DossieProdutoCriadoDto.class);

        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + MOCK_RESOURCE);
        }

        return resposta;
    }
}
