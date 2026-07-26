package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.configuracao;

import dev.langchain4j.model.ollama.OllamaChatModel.OllamaChatModelBuilder;
import io.quarkiverse.langchain4j.ModelBuilderCustomizer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OllamaChatModelCustomizer
        implements ModelBuilderCustomizer<OllamaChatModelBuilder> {

    @Override
    public void customize(OllamaChatModelBuilder builder) {
        builder.maxRetries(0);
    }
}
