package br.gov.caixa.simtr.hub.parametrizacao.integracao.mock;

import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;

@ApplicationScoped
public class ProcessoMockFactory {

    private static final long DEFAULT_IDENTIFICADOR_NEGOCIAL = 1000016487L;
    private static final String MOCK_RESOURCE_TEMPLATE =
            "mock/parametrizacao/%d-consulta-processo-parametrizacao-v2-identificador-negocial.md";
    private static final String DEFAULT_MOCK_RESOURCE = mockResourceName(DEFAULT_IDENTIFICADOR_NEGOCIAL);

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public ProcessoMockFactory(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    public ProcessoDto criarProcessoMock(Long identificador) {
        long identificadorMock = Objects.requireNonNullElse(identificador, DEFAULT_IDENTIFICADOR_NEGOCIAL);
        String resourceName = mockResourceName(identificadorMock);
        ProcessoDto processo = mockReader.readFirstJsonObject(resourceName, ProcessoDto.class);

        if (processo == null && !DEFAULT_MOCK_RESOURCE.equals(resourceName)) {
            resourceName = DEFAULT_MOCK_RESOURCE;
            processo = mockReader.readFirstJsonObject(resourceName, ProcessoDto.class);
        }

        if (processo == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + resourceName);
        }

        return processo;
    }

    private static String mockResourceName(long identificador) {
        return MOCK_RESOURCE_TEMPLATE.formatted(identificador);
    }
}
