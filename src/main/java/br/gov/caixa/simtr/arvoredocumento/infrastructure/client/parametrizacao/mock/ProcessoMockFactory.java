package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.mock;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ApplicationScoped
public class ProcessoMockFactory {

    private static final long DEFAULT_IDENTIFICADOR_NEGOCIAL = 1000016487L;
    private static final String JSON_SECTION_TITLE = "## dados do mock corpo do retorno json";
    private static final String MOCK_RESOURCE_TEMPLATE =
            "mock/parametrizacao/%d-consulta-processo-parametrizacao-v2-identificador-negocial.md";
    private static final String DEFAULT_MOCK_RESOURCE = mockResourceName(DEFAULT_IDENTIFICADOR_NEGOCIAL);

    private final ObjectMapper objectMapper;

    @Inject
    public ProcessoMockFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ProcessoDto criarProcessoMock(Long identificador) {
        long identificadorMock = Objects.requireNonNullElse(identificador, DEFAULT_IDENTIFICADOR_NEGOCIAL);
        String resourceName = mockResourceName(identificadorMock);
        String markdown = readResource(resourceName);

        if (markdown == null && !DEFAULT_MOCK_RESOURCE.equals(resourceName)) {
            resourceName = DEFAULT_MOCK_RESOURCE;
            markdown = readResource(resourceName);
        }

        if (markdown == null) {
            throw new IllegalStateException("Arquivo de mock nao encontrado no classpath: " + resourceName);
        }

        return readProcessoFromMarkdown(markdown, resourceName);
    }

    private static String mockResourceName(long identificador) {
        return MOCK_RESOURCE_TEMPLATE.formatted(identificador);
    }

    private String readResource(String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao ler arquivo de mock: " + resourceName, e);
        }
    }

    private ProcessoDto readProcessoFromMarkdown(String markdown, String resourceName) {
        try {
            return objectMapper.readValue(extractJsonBody(markdown, resourceName), ProcessoDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao converter JSON do arquivo de mock: " + resourceName, e);
        }
    }

    private static String extractJsonBody(String markdown, String resourceName) {
        int jsonSectionStart = markdown.indexOf(JSON_SECTION_TITLE);
        int searchStart = jsonSectionStart >= 0
                ? jsonSectionStart + JSON_SECTION_TITLE.length()
                : 0;
        int start = markdown.indexOf('{', searchStart);
        int end = markdown.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new IllegalStateException("Arquivo de mock nao contem corpo JSON valido: " + resourceName);
        }

        return markdown.substring(start, end + 1);
    }
}
