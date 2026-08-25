package br.gov.caixa.simtr.dossie;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.ConsultaDocumentosDossieProdutoObservabilidade;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ConsultaDocumentosDossieProdutoQuarkusTest {

    @Inject
    Instance<ConsultaDocumentosDossieProduto> consumidorSelecionado;

    @Inject
    Instance<ConsultarDocumentosDossieProduto> portaSelecionada;

    @Test
    void resolveConsumidorComPortaObservavelSemAmbiguidade() {
        assertFalse(consumidorSelecionado.isUnsatisfied());
        assertFalse(consumidorSelecionado.isAmbiguous());
        assertNotNull(consumidorSelecionado.get());

        assertFalse(portaSelecionada.isUnsatisfied());
        assertFalse(portaSelecionada.isAmbiguous());
        assertInstanceOf(
                ConsultaDocumentosDossieProdutoObservabilidade.class,
                portaSelecionada.get());
    }
}
