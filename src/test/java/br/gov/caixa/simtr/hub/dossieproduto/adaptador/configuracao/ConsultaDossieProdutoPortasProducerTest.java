package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.lang.reflect.Method;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConsultaDossieProdutoPortasProducerTest {

    private static final String PROPERTY_SIMULADOR =
            "simtr-hub.simulador.dossie-produto.habilitado";

    @Inject
    Instance<ObterDossieProduto> portaSelecionada;

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        ObterDossieProduto mtr = identificador -> Uni.createFrom().nullItem();
        ObterDossieProduto simulador = identificador -> Uni.createFrom().nullItem();
        var producer = new ConsultaDossieProdutoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }

    @Test
    void declaraQualifiersEPropertyExistenteNoPontoDeSelecao() throws Exception {
        var tipo = ConsultaDossieProdutoPortasProducer.class;
        Method metodo = tipo.getDeclaredMethod(
                "portaSaida",
                ObterDossieProduto.class,
                ObterDossieProduto.class,
                boolean.class
        );

        assertNotNull(tipo.getAnnotation(ApplicationScoped.class));
        assertNotNull(metodo.getAnnotation(Produces.class));
        assertNotNull(metodo.getAnnotation(ApplicationScoped.class));
        assertNotNull(metodo.getParameters()[0].getAnnotation(
                ConsultaDossieProdutoMtr.class));
        assertNotNull(metodo.getParameters()[1].getAnnotation(
                ConsultaDossieProdutoSimulador.class));
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
