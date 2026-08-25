package br.gov.caixa.simtr.dossie;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultaDocumentosDossieProdutoTest {

    @Test
    void delegaUmaVezComOsMesmosCriteriosEPropagaALista() {
        var criterios = criteriosCompletos();
        var esperado = List.of(documentoConsultado());
        var porta = new FakePortaEntrada(Uni.createFrom().item(esperado));
        var consulta = new ConsultaDocumentosDossieProduto(porta);

        var resultado = consulta.consultar(criterios).await().indefinitely();

        assertEquals(1, porta.invocacoes);
        assertSame(criterios, porta.criteriosRecebidos);
        assertSame(esperado, resultado);
    }

    @Test
    void propagaAListaVaziaSemNovaDelegacao() {
        var criterios = criteriosCompletos();
        List<DocumentoDossieProdutoConsultado> esperado = List.of();
        var porta = new FakePortaEntrada(Uni.createFrom().item(esperado));
        var consulta = new ConsultaDocumentosDossieProduto(porta);

        var resultado = consulta.consultar(criterios).await().indefinitely();

        assertEquals(1, porta.invocacoes);
        assertSame(criterios, porta.criteriosRecebidos);
        assertSame(esperado, resultado);
    }

    @Test
    void propagaAMesmaFalhaSemNovaDelegacao() {
        var criterios = criteriosCompletos();
        var falha = new FalhaConsultaDocumentosDossieProduto(
                FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                503,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        var porta = new FakePortaEntrada(Uni.createFrom().failure(falha));
        var consulta = new ConsultaDocumentosDossieProduto(porta);

        var espera = consulta.consultar(criterios).await();
        var observada = assertThrows(
                FalhaConsultaDocumentosDossieProduto.class, espera::indefinitely);

        assertEquals(1, porta.invocacoes);
        assertSame(criterios, porta.criteriosRecebidos);
        assertSame(falha, observada);
    }

    private static CriteriosConsultaDocumentosDossieProduto criteriosCompletos() {
        return new CriteriosConsultaDocumentosDossieProduto(
                new IdentificadorDossieProduto(4081899L),
                "12345678000190",
                "12345678901",
                7L,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                "192.0.2.10",
                "CONTRATO");
    }

    private static DocumentoDossieProdutoConsultado documentoConsultado() {
        return new DocumentoDossieProdutoConsultado(
                101L,
                202L,
                "GED-SINTETICO-001",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static final class FakePortaEntrada implements ConsultarDocumentosDossieProduto {

        private final Uni<List<DocumentoDossieProdutoConsultado>> resultado;
        private CriteriosConsultaDocumentosDossieProduto criteriosRecebidos;
        private int invocacoes;

        private FakePortaEntrada(Uni<List<DocumentoDossieProdutoConsultado>> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<List<DocumentoDossieProdutoConsultado>> executar(
                CriteriosConsultaDocumentosDossieProduto criterios
        ) {
            invocacoes++;
            criteriosRecebidos = criterios;
            return resultado;
        }
    }
}
