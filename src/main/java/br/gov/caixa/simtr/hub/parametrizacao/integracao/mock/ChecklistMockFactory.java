package br.gov.caixa.simtr.hub.parametrizacao.integracao.mock;

import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;

@ApplicationScoped
public class ChecklistMockFactory {

    private static final long DEFAULT_IDENTIFICADOR_NEGOCIAL = 1000012583L;
    private static final int DEFAULT_VERSAO = 1;
    private static final int VERSAO_API = 1;
    private static final String MOCK_RESOURCE_TEMPLATE =
            "mock/parametrizacao/%d-v%d-checklist-parametrizacao-versao-%d.md";
    private static final String DEFAULT_MOCK_RESOURCE = mockResourceName(DEFAULT_IDENTIFICADOR_NEGOCIAL, DEFAULT_VERSAO);

    private final MarkdownJsonMockReader mockReader;

    @Inject
    public ChecklistMockFactory(MarkdownJsonMockReader mockReader) {
        this.mockReader = mockReader;
    }

    public ChecklistDto criarChecklistMock(Long identificador, Integer versao) {
        long identificadorMock = Objects.requireNonNullElse(identificador, DEFAULT_IDENTIFICADOR_NEGOCIAL);
        int versaoMock = Objects.requireNonNullElse(versao, DEFAULT_VERSAO);
        String resourceName = mockResourceName(identificadorMock, versaoMock);
        ChecklistDto checklist = mockReader.readFirstJsonObject(resourceName, ChecklistDto.class);

        if (checklist == null && !DEFAULT_MOCK_RESOURCE.equals(resourceName)) {
            resourceName = DEFAULT_MOCK_RESOURCE;
            checklist = mockReader.readFirstJsonObject(resourceName, ChecklistDto.class);
        }

        if (checklist == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + resourceName);
        }

        return checklist;
    }

    private static String mockResourceName(long identificador, int versao) {
        return MOCK_RESOURCE_TEMPLATE.formatted(identificador, VERSAO_API, versao);
    }
}
