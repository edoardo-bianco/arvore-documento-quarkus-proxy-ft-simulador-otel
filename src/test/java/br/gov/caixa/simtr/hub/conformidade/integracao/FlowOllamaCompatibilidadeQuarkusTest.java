package br.gov.caixa.simtr.hub.conformidade.integracao;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.agent;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consumed;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emitJson;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.quarkiverse.flow.Flow;
import io.quarkiverse.flow.messaging.FlowMessagingConsumer;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FlowOllamaCompatibilidadeQuarkusTest {

    @Test
    void iniciaSemServicoExternoComApisEConfiguracaoAprovadas() {
        Config config = ConfigProvider.getConfig();
        ApiCompativelFlow flow = new ApiCompativelFlow();

        assertAll(
                () -> assertNotNull(flow.descriptor()),
                () -> assertNotNull(Flow.class),
                () -> assertNotNull(FlowMessagingConsumer.class),
                () -> assertNotNull(OllamaChatModel.class),
                () -> assertTrue(RegisterAiService.class.isAnnotation()),
                () -> assertTrue(SequenceAgent.class.isAnnotation()),
                () -> assertTrue(Agent.class.isAnnotation()),
                () -> assertEquals(
                        "http://localhost:1/",
                        config.getValue("quarkus.langchain4j.ollama.base-url", String.class)),
                () -> assertEquals(
                        "llama3.2:3b",
                        config.getValue("quarkus.langchain4j.ollama.chat-model.model-id", String.class)),
                () -> assertEquals(
                        0.2d,
                        config.getValue("quarkus.langchain4j.ollama.chat-model.temperature", Double.class)),
                () -> assertEquals(
                        2048,
                        config.getValue(
                                "quarkus.langchain4j.ollama.chat-model.model-options.num-ctx",
                                Integer.class)),
                () -> assertEquals(
                        Duration.ofSeconds(60),
                        config.getValue("quarkus.langchain4j.ollama.timeout", Duration.class)),
                () -> assertFalse(
                        config.getValue("quarkus.langchain4j.ollama.devservices.enabled", Boolean.class)),
                () -> assertFalse(
                        config.getValue("quarkus.langchain4j.ollama.enable-integration", Boolean.class)),
                () -> assertFalse(
                        config.getValue("quarkus.flow.tracing.enabled", Boolean.class)),
                () -> assertFalse(
                        config.getOptionalValue(
                                        "quarkus.langchain4j.log-requests",
                                        Boolean.class)
                                .orElse(false)),
                () -> assertFalse(
                        config.getOptionalValue(
                                        "quarkus.langchain4j.log-responses",
                                        Boolean.class)
                                .orElse(false)),
                () -> assertEquals(
                        "ponte-assincrona",
                        flow.comoCompletionStage(Uni.createFrom().item("ponte-assincrona"))
                                .toCompletableFuture()
                                .join()));
    }

    private static final class ApiCompativelFlow {

        private final AgenteCompativel agente = new AgenteCompativel();

        Workflow descriptor() {
            return FuncWorkflowBuilder.workflow("compatibilidade-flow-agentic-ollama")
                    .tasks(
                            agent("analisar", agente::analisar, Entrada.class),
                            emitJson(
                                    "solicitarRevisao",
                                    "br.gov.caixa.simtr.hub.conformidade.revisao.solicitada",
                                    Resultado.class),
                            listen(
                                    "aguardarRevisao",
                                    toOne(consumed("br.gov.caixa.simtr.hub.conformidade.revisao.concluida")
                                            .extensionByInstanceId("flowinstanceid"))))
                    .build();
        }

        CompletionStage<String> comoCompletionStage(Uni<String> resultado) {
            return resultado.subscribeAsCompletionStage();
        }
    }

    private static final class AgenteCompativel {

        Resultado analisar(String idInstancia, Entrada entrada) {
            return new Resultado(entrada.texto(), 1.0d);
        }
    }

    private record Entrada(String texto) {
    }

    private record Resultado(String parecer, double confianca) {
    }
}
