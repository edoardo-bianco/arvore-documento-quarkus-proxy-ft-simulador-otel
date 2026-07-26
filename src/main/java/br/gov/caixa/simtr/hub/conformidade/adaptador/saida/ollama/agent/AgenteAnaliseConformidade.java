package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.agent;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.dto.ResultadoAnaliseAgente;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterAiService
public interface AgenteAnaliseConformidade {

    @SequenceAgent(
            description = "Aplica o checklist e revisa a cobertura da análise",
            outputKey = "resultadoFinal",
            subAgents = {AplicadorChecklistAgent.class, RevisorCoberturaAgent.class})
    ResultadoAnaliseAgente analisar(
            @MemoryId String identificadorMemoria,
            @V("entradaJson") String entradaJson);
}
