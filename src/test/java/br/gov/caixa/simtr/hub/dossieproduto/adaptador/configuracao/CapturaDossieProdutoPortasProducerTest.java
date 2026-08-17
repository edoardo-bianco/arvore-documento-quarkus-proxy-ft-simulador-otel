package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@QuarkusTest
class CapturaDossieProdutoPortasProducerTest {

    private static final String PROPERTY_SIMULADOR =
            "simtr-hub.simulador.dossie-produto.habilitado";

    @Inject
    Instance<SolicitarCapturaDossieProduto> portaSelecionada;

    @Test
    void selecionaSimuladorSomenteQuandoLigadoEMtrQuandoDesligado() {
        SolicitarCapturaDossieProduto mtr = identificador -> Uni.createFrom().nullItem();
        SolicitarCapturaDossieProduto simulador = identificador ->
                Uni.createFrom().nullItem();
        var producer = new CapturaDossieProdutoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }

    @Test
    void declaraQualifiersEReutilizaPropertyExistenteNoPontoDeSelecao() throws Exception {
        var tipo = CapturaDossieProdutoPortasProducer.class;
        Method metodo = tipo.getDeclaredMethod(
                "portaSaida",
                SolicitarCapturaDossieProduto.class,
                SolicitarCapturaDossieProduto.class,
                boolean.class);

        assertNotNull(tipo.getAnnotation(ApplicationScoped.class));
        assertNotNull(metodo.getAnnotation(Produces.class));
        assertNotNull(metodo.getAnnotation(ApplicationScoped.class));
        assertNotNull(metodo.getParameters()[0].getAnnotation(CapturaMtr.class));
        assertNotNull(metodo.getParameters()[1].getAnnotation(CapturaSimulador.class));
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
