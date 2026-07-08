package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class MarkdownJsonMockReader {

    private static final String JSON_SECTION_TITLE = "## dados do mock corpo do retorno json";

    private final ObjectMapper objectMapper;

    @Inject
    public MarkdownJsonMockReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T readFirstJsonObject(String resourceName, Class<T> type) {
        String markdown = readResource(resourceName);
        if (markdown == null) {
            return null;
        }

        try {
            return objectMapper.readValue(extractJsonBody(markdown, resourceName), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao converter JSON do arquivo de mock: " + resourceName, e);
        }
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
