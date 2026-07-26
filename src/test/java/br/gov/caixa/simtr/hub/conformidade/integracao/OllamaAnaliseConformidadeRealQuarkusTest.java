package br.gov.caixa.simtr.hub.conformidade.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.AnalisarTextoComChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EntradaAnaliseAgente;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@QuarkusTest
@Tag("ollama")
@EnabledIfSystemProperty(named = "ollama.integration", matches = "true")
class OllamaAnaliseConformidadeRealQuarkusTest {

    @Inject
    AnalisarTextoComChecklist analisarTexto;

    @Test
    void executaSequenciaAgenticNoModeloLocalComSaidaEstruturada() {
        var resultado = analisarTexto.analisar(
                "teste-ollama-local",
                new EntradaAnaliseAgente(
                        "O documento identifica o cliente João da Silva pelo CPF 123.456.789-00.",
                        checklist()));

        assertEquals(OrigemResultado.AGENTE, resultado.origem());
        assertEquals(1000012583L, resultado.identificadorChecklist());
        assertEquals(10L, resultado.apontamentos().get(0).identificadorApontamento());
    }

    private static Checklist checklist() {
        return new Checklist(
                "Identificação do cliente",
                1000012583L,
                1,
                null,
                null,
                false,
                "Avaliar somente o conteúdo fornecido",
                List.of(new ApontamentoChecklist(
                        10L,
                        "Cliente identificado",
                        "Verificar se nome e CPF do cliente estão presentes",
                        "Considere conforme quando nome e CPF aparecem no texto",
                        false,
                        1)));
    }
}
