package br.gov.caixa.simtr.hub.dossieproduto.integracao.mock;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DossieProdutoMockFactory {

    private static final String DOCUMENTO_MOCK_RESOURCE =
            "mock/dossieproduto/documento-dossie-produto.md";
    private static final String VALIDACAO_NEGOCIAL_MOCK_RESOURCE =
            "mock/dossieproduto/validacao-negocial-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public DossieProdutoMockFactory(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    public DossieProdutoDocumentoCriadoDto incluirDocumentoDossieProdutoMock(
            Long id,
            DossieProdutoDocumentoInclusaoDto requisicao
    ) {
        DossieProdutoDocumentoCriadoDto resposta = mockReader.readFirstJsonObject(
                DOCUMENTO_MOCK_RESOURCE,
                DossieProdutoDocumentoCriadoDto.class
        );

        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + DOCUMENTO_MOCK_RESOURCE);
        }

        return resposta;
    }

    public void registrarValidacaoNegocialDossieProdutoMock(
            Long id,
            DossieProdutoValidacaoNegocialDto requisicao
    ) {
        Object resposta = mockReader.readFirstJsonObject(
                VALIDACAO_NEGOCIAL_MOCK_RESOURCE,
                Object.class
        );

        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + VALIDACAO_NEGOCIAL_MOCK_RESOURCE);
        }
    }
}
