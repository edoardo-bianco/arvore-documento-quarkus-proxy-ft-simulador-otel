package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.FormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoFormularioDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtualizarFormularioDossieProdutoCasoDeUsoTest {

    @Test
    void delegaComandoCompletoParaPortaDeSaidaSemCopiarListas() {
        List<FormularioDossieProduto> formularios = formulariosComElementoNulo();
        ComandoAtualizacaoFormularioDossieProduto comando =
                new ComandoAtualizacaoFormularioDossieProduto(123L, formularios);
        ResultadoAtualizacaoFormularioDossieProduto esperado =
                new ResultadoAtualizacaoFormularioDossieProduto(987L);
        FakePortaSaida portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        AtualizarFormularioDossieProdutoCasoDeUso casoDeUso =
                new AtualizarFormularioDossieProdutoCasoDeUso(portaSaida);

        ResultadoAtualizacaoFormularioDossieProduto resultado = casoDeUso.executar(comando)
                .await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertSame(formularios, portaSaida.comandoRecebido.formularios());
        assertNull(portaSaida.comandoRecebido.formularios().get(1));
        assertSame(esperado, resultado);
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        IllegalStateException falha = new IllegalStateException("falha interna");
        AtualizarFormularioDossieProdutoCasoDeUso casoDeUso =
                new AtualizarFormularioDossieProdutoCasoDeUso(
                        new FakePortaSaida(Uni.createFrom().failure(falha)));

        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar(
                        new ComandoAtualizacaoFormularioDossieProduto(123L, null))
                        .await().indefinitely()
        );

        assertEquals("falha interna", observada.getMessage());
    }

    private static List<FormularioDossieProduto> formulariosComElementoNulo() {
        List<ClienteAvalistaFormularioDossieProduto> avalistas = new ArrayList<>();
        avalistas.add(new ClienteAvalistaFormularioDossieProduto(null, "12345678000190"));
        avalistas.add(null);

        List<RespostaFormularioDossieProduto> respostas = new ArrayList<>();
        respostas.add(new RespostaFormularioDossieProduto(
                600L,
                "resposta",
                List.of("opcao-1"),
                true
        ));
        respostas.add(null);

        FormularioDossieProduto formulario = new FormularioDossieProduto(
                new VinculoFormularioDossieProduto(
                        10L,
                        new ClienteFormularioDossieProduto("12345678901", null, 1L),
                        new ProdutoFormularioDossieProduto(100, 200),
                        new GarantiaFormularioDossieProduto(300, 400, 500, avalistas),
                        respostas
                )
        );

        List<FormularioDossieProduto> formularios = new ArrayList<>();
        formularios.add(formulario);
        formularios.add(null);
        return formularios;
    }

    private static final class FakePortaSaida implements SolicitarAtualizacaoFormularioDossieProduto {

        private final Uni<ResultadoAtualizacaoFormularioDossieProduto> resultado;
        private ComandoAtualizacaoFormularioDossieProduto comandoRecebido;

        private FakePortaSaida(Uni<ResultadoAtualizacaoFormularioDossieProduto> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<ResultadoAtualizacaoFormularioDossieProduto> atualizar(
                ComandoAtualizacaoFormularioDossieProduto comando) {
            comandoRecebido = comando;
            return resultado;
        }
    }
}
