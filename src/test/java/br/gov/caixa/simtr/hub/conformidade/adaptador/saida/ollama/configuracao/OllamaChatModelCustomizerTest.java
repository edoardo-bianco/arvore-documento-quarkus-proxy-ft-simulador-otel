package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.configuracao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.langchain4j.model.ollama.OllamaChatModel.OllamaChatModelBuilder;
import org.junit.jupiter.api.Test;

class OllamaChatModelCustomizerTest {

    @Test
    void desabilitaRetryInternoParaManterFtComoPoliticaUnica() {
        OllamaChatModelBuilder builder = mock(OllamaChatModelBuilder.class);

        new OllamaChatModelCustomizer().customize(builder);

        verify(builder).maxRetries(0);
    }
}
