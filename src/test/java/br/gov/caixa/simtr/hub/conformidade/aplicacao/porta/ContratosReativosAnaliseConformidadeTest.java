package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.AnaliseConformidadeResource;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise.IniciarAnaliseConformidadeRequest;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
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

    private static void assertRetornaUni(Method metodo) {
        assertEquals(
                Uni.class,
                metodo.getReturnType(),
                () -> metodo + " deve propagar Uni");
    }
}
