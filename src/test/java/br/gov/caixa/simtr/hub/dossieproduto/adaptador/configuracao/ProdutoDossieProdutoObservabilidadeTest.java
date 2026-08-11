package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProdutoDossieProdutoObservabilidadeTest {

    @Test
    void delegaMesmoComandoEPreservaResultadoVoid() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        var observabilidade = new ProdutoDossieProdutoObservabilidade(portaSaida, false);
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(
                123L,
                List.of(new ProdutoContratadoDossieProduto(100, 200, false)));

        Void resultado = observabilidade.executar(comando).await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertNull(resultado);
    }

    @Test
    void propagaMesmaFalhaDaPortaDeSaida() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException("falha produto");
        var observabilidade = new ProdutoDossieProdutoObservabilidade(portaSaida, true);
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(null, null);

        var espera = observabilidade.executar(comando).await();
        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                espera::indefinitely);

        assertSame(portaSaida.falha, falha);
        assertSame(comando, portaSaida.comandoRecebido);
    }

    @Test
    void declaraSpanDeAplicacaoAprovado() throws NoSuchMethodException {
        Method executar = ProdutoDossieProdutoObservabilidade.class.getDeclaredMethod(
                "executar", ComandoAlteracaoProdutosContratadosDossieProduto.class);
        WithSpan span = executar.getAnnotation(WithSpan.class);

        assertNotNull(span, "wrapper deve declarar span de aplicacao");
        assertEquals("simtr-hub.service.dossie-produto.produto.alterar", span.value());
    }

    private static final class FakePortaSaida
            implements SolicitarAlteracaoProdutosContratadosDossieProduto {

        private ComandoAlteracaoProdutosContratadosDossieProduto comandoRecebido;
        private RuntimeException falha;

        @Override
        public Uni<Void> alterar(ComandoAlteracaoProdutosContratadosDossieProduto comando) {
            comandoRecebido = comando;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().voidItem();
        }
    }
}
