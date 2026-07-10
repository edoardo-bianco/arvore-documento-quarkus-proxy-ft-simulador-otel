package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto.mock;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.mock.MarkdownJsonMockReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class DossieProdutoMockFactory {

    private static final String CRIACAO_BASICA_MOCK_RESOURCE =
            "mock/dossieproduto/criacao-basica-dossie-produto.md";
    private static final String FORMULARIO_MOCK_RESOURCE =
            "mock/dossieproduto/formulario-dossie-produto.md";
    private static final String DOCUMENTO_MOCK_RESOURCE =
            "mock/dossieproduto/documento-dossie-produto.md";
    private static final String VALIDACAO_NEGOCIAL_MOCK_RESOURCE =
            "mock/dossieproduto/validacao-negocial-dossie-produto.md";
    private static final String WORKFLOW_MOCK_RESOURCE =
            "mock/dossieproduto/workflow-dossie-produto.md";

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public DossieProdutoMockFactory(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    public DossieProdutoCriadoDto criarDossieProdutoMock(DossieProdutoCriacaoDto requisicao) {
        DossieProdutoCriadoDto resposta = mockReader.readFirstJsonObject(
                CRIACAO_BASICA_MOCK_RESOURCE,
                DossieProdutoCriadoDto.class
        );

        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + CRIACAO_BASICA_MOCK_RESOURCE);
        }

        return resposta;
    }

    public DossieProdutoCriadoDto atualizarFormularioDossieProdutoMock(
            Long id,
            List<DossieProdutoFormularioDto> requisicao
    ) {
        DossieProdutoCriadoDto resposta = mockReader.readFirstJsonObject(
                FORMULARIO_MOCK_RESOURCE,
                DossieProdutoCriadoDto.class
        );

        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + FORMULARIO_MOCK_RESOURCE);
        }

        if (id != null) {
            return new DossieProdutoCriadoDto(id);
        }

        return resposta;
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

    public DossieProdutoCriadoDto iniciarOuAvancarWorkflowDossieProdutoMock(Long id) {
        DossieProdutoCriadoDto resposta = mockReader.readFirstJsonObject(
                WORKFLOW_MOCK_RESOURCE,
                DossieProdutoCriadoDto.class
        );

        if (resposta == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + WORKFLOW_MOCK_RESOURCE);
        }

        if (id != null) {
            return new DossieProdutoCriadoDto(id);
        }

        return resposta;
    }
}
