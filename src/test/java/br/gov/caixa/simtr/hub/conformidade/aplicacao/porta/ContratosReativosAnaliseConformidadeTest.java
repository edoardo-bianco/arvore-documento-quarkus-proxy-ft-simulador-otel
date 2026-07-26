package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.AnaliseConformidadeResource;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.IniciarAnaliseConformidadeRequest;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.AnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.ConsultarChecklistEtapa;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.ContextoAnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class ContratosReativosAnaliseConformidadeTest {

    @Test
    void inicioEConsultaPropagamUniAteOBordaRest() throws NoSuchMethodException {
        assertRetornaUni(
                IniciarAnaliseConformidade.class.getMethod(
                        "executar",
                        SolicitacaoAnaliseConformidade.class));
        assertRetornaUni(
                ConsultarAnaliseConformidade.class.getMethod(
                        "executar",
                        String.class));
        assertRetornaUni(
                AnaliseConformidadeResource.class.getMethod(
                        "iniciar",
                        IniciarAnaliseConformidadeRequest.class));
        assertRetornaUni(
                AnaliseConformidadeResource.class.getMethod(
                        "consultar",
                        String.class));
    }

    @Test
    void persistenciaDocumentalPropagaUniEmTodasAsOperacoes() {
        Method[] operacoes = ArmazenarEstadoAnaliseConformidade.class.getDeclaredMethods();

        assertEquals(7, operacoes.length);
        for (Method operacao : operacoes) {
            assertRetornaUni(operacao);
        }
    }

    @Test
    void callbacksDaAplicacaoPropagamUniDiretamenteAoFlow()
            throws NoSuchMethodException {
        assertRetornaUni(ConsultarChecklistEtapa.class.getMethod(
                "executar",
                SolicitacaoAnaliseConformidade.class));
        assertRetornaUni(AnaliseConformidadeFlow.class.getDeclaredMethod(
                "analisar",
                String.class,
                ContextoAnaliseConformidadeFlow.class));
        assertRetornaUni(AnaliseConformidadeFlow.class.getDeclaredMethod(
                "consolidarRevisao",
                String.class,
                RevisaoHumanaConformidade[].class));
    }

    private static void assertRetornaUni(Method metodo) {
        assertEquals(
                Uni.class,
                metodo.getReturnType(),
                () -> metodo + " deve propagar Uni");
    }
}
