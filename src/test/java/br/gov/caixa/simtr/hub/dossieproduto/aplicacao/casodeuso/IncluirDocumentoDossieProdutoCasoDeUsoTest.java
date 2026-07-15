package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.AtributoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.PropriedadeDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoDocumentoDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncluirDocumentoDossieProdutoCasoDeUsoTest {

    @Test
    void delegaComandoCompletoParaPortaDeSaidaSemCopiarListas() {
        List<ClienteDocumentoDossieProduto> avalistas = new ArrayList<>();
        avalistas.add(new ClienteDocumentoDossieProduto(null, "12345678000190", null));
        avalistas.add(null);

        List<AtributoDocumentoDossieProduto> atributos = new ArrayList<>();
        atributos.add(new AtributoDocumentoDossieProduto(
                "numero", "12345", "documento", List.of("opcao-1")));
        atributos.add(null);

        List<PropriedadeDocumentoDossieProduto> propriedades = new ArrayList<>();
        propriedades.add(new PropriedadeDocumentoDossieProduto(
                "origem", "pre-validacao", "documento"));
        propriedades.add(null);

        ComandoInclusaoDocumentoDossieProduto comando =
                new ComandoInclusaoDocumentoDossieProduto(
                        123L,
                        321L,
                        "container/documento.pdf",
                        "GED123",
                        "OBJECT_STORE",
                        "RG",
                        new VinculoDocumentoDossieProduto(
                                new ClienteDocumentoDossieProduto("12345678901", null, 1L),
                                700L,
                                new GarantiaDocumentoDossieProduto(300, 400, 500, avalistas)
                        ),
                        atributos,
                        propriedades
                );
        ResultadoInclusaoDocumentoDossieProduto esperado =
                new ResultadoInclusaoDocumentoDossieProduto(456L, 789L);
        FakePortaSaida portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        IncluirDocumentoDossieProdutoCasoDeUso casoDeUso =
                new IncluirDocumentoDossieProdutoCasoDeUso(portaSaida);

        ResultadoInclusaoDocumentoDossieProduto resultado = casoDeUso.executar(comando)
                .await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertSame(atributos, portaSaida.comandoRecebido.atributos());
        assertNull(portaSaida.comandoRecebido.atributos().get(1));
        assertSame(propriedades, portaSaida.comandoRecebido.propriedades());
        assertNull(portaSaida.comandoRecebido.propriedades().get(1));
        assertSame(avalistas, portaSaida.comandoRecebido.vinculoDossie()
                .garantia().clientesAvalistas());
        assertNull(portaSaida.comandoRecebido.vinculoDossie()
                .garantia().clientesAvalistas().get(1));
        assertSame(esperado, resultado);
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        IllegalStateException falha = new IllegalStateException("falha interna");
        IncluirDocumentoDossieProdutoCasoDeUso casoDeUso =
                new IncluirDocumentoDossieProdutoCasoDeUso(
                        new FakePortaSaida(Uni.createFrom().failure(falha)));

        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar(comandoMinimo()).await().indefinitely()
        );

        assertEquals("falha interna", observada.getMessage());
    }

    private static ComandoInclusaoDocumentoDossieProduto comandoMinimo() {
        return new ComandoInclusaoDocumentoDossieProduto(
                123L, null, null, null, null, null, null, null, null);
    }

    private static final class FakePortaSaida
            implements SolicitarInclusaoDocumentoDossieProduto {

        private final Uni<ResultadoInclusaoDocumentoDossieProduto> resultado;
        private ComandoInclusaoDocumentoDossieProduto comandoRecebido;

        private FakePortaSaida(Uni<ResultadoInclusaoDocumentoDossieProduto> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<ResultadoInclusaoDocumentoDossieProduto> incluir(
                ComandoInclusaoDocumentoDossieProduto comando) {
            comandoRecebido = comando;
            return resultado;
        }
    }
}
