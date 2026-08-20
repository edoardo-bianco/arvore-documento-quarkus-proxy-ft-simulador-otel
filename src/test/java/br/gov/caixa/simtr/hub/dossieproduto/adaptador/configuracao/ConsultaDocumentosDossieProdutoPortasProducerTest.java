package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
class ConsultaDocumentosDossieProdutoPortasProducerTest {

    private static final String PROPERTY_SIMULADOR =
            "simtr-hub.simulador.dossie-produto.habilitado";

    @Inject
    Instance<ObterDocumentosDossieProduto> portaSelecionada;

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        ObterDocumentosDossieProduto mtr = criterios -> Uni.createFrom().item(List.of());
        ObterDocumentosDossieProduto simulador = criterios -> Uni.createFrom().item(List.of());
        var producer = new ConsultaDocumentosDossieProdutoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }

    @Test
    void declaraQualifiersEPropertyExistenteNoPontoDeSelecao() throws Exception {
        var tipo = ConsultaDocumentosDossieProdutoPortasProducer.class;
        Method metodo = tipo.getDeclaredMethod(
                "portaSaida",
                ObterDocumentosDossieProduto.class,
                ObterDocumentosDossieProduto.class,
                boolean.class
        );

        assertNotNull(tipo.getAnnotation(ApplicationScoped.class));
        assertNotNull(metodo.getAnnotation(Produces.class));
        assertNotNull(metodo.getAnnotation(ApplicationScoped.class));
        assertNotNull(metodo.getParameters()[0].getAnnotation(
                ConsultaDocumentosDossieProdutoMtr.class));
        assertNotNull(metodo.getParameters()[1].getAnnotation(
                ConsultaDocumentosDossieProdutoSimulador.class));
        assertEquals(PROPERTY_SIMULADOR, metodo.getParameters()[2]
                .getAnnotation(ConfigProperty.class).name());
    }

    @Test
    void resolveUmaUnicaPortaNoBootstrapQuarkus() {
        assertFalse(portaSelecionada.isUnsatisfied());
        assertFalse(portaSelecionada.isAmbiguous());
        assertNotNull(portaSelecionada.get());
    }
}
