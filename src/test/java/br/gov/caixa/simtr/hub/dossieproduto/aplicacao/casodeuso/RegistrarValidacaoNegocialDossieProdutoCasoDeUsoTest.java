package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ParecerApontamentoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VerificacaoValidacaoNegocialDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarValidacaoNegocialDossieProdutoCasoDeUsoTest {

    @Test
    void delegaComandoCompletoParaPortaDeSaidaSemCopiarListas() {
        List<ClienteAvalistaValidacaoNegocialDossieProduto> avalistas = new ArrayList<>();
        avalistas.add(new ClienteAvalistaValidacaoNegocialDossieProduto(
                "12345678901", null));
        avalistas.add(null);

        List<ParecerApontamentoValidacaoNegocialDossieProduto> pareceres = new ArrayList<>();
        pareceres.add(new ParecerApontamentoValidacaoNegocialDossieProduto(
                1000012877L,
                "APROVADO",
                "apontamento aprovado",
                false,
                1.0));
        pareceres.add(null);

        List<String> opcoesSelecionadas = new ArrayList<>();
        opcoesSelecionadas.add("2");
        opcoesSelecionadas.add(null);

        List<VerificacaoValidacaoNegocialDossieProduto> verificacoes = new ArrayList<>();
        verificacoes.add(new VerificacaoValidacaoNegocialDossieProduto(
                1122928L,
                6592L,
                2,
                true,
                pareceres,
                new GarantiaValidacaoNegocialDossieProduto(300, avalistas),
                new ProdutoValidacaoNegocialDossieProduto(100, 200),
                null));
        verificacoes.add(null);

        List<RespostaFormularioValidacaoNegocialDossieProduto> respostas = new ArrayList<>();
        respostas.add(new RespostaFormularioValidacaoNegocialDossieProduto(
                1000011699L, null, opcoesSelecionadas));
        respostas.add(null);

        ComandoRegistroValidacaoNegocialDossieProduto comando =
                new ComandoRegistroValidacaoNegocialDossieProduto(
                        123L, verificacoes, respostas);
        FakePortaSaida portaSaida = new FakePortaSaida(Uni.createFrom().voidItem());
        RegistrarValidacaoNegocialDossieProdutoCasoDeUso casoDeUso =
                new RegistrarValidacaoNegocialDossieProdutoCasoDeUso(portaSaida);

        Void resultado = casoDeUso.executar(comando).await().indefinitely();

        assertNull(resultado);
        assertSame(comando, portaSaida.comandoRecebido);
        assertSame(verificacoes, portaSaida.comandoRecebido.verificacoes());
        assertNull(portaSaida.comandoRecebido.verificacoes().get(1));
        assertSame(pareceres, portaSaida.comandoRecebido.verificacoes().getFirst()
                .parecerApontamentos());
        assertNull(portaSaida.comandoRecebido.verificacoes().getFirst()
                .parecerApontamentos().get(1));
        assertSame(avalistas, portaSaida.comandoRecebido.verificacoes().getFirst()
                .garantia().clientesAvalistas());
        assertNull(portaSaida.comandoRecebido.verificacoes().getFirst()
                .garantia().clientesAvalistas().get(1));
        assertSame(respostas, portaSaida.comandoRecebido.respostasFormulario());
        assertNull(portaSaida.comandoRecebido.respostasFormulario().get(1));
        assertSame(opcoesSelecionadas, portaSaida.comandoRecebido.respostasFormulario()
                .getFirst().opcoesSelecionadas());
        assertNull(portaSaida.comandoRecebido.respostasFormulario()
                .getFirst().opcoesSelecionadas().get(1));
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        IllegalStateException falha = new IllegalStateException("falha interna");
        RegistrarValidacaoNegocialDossieProdutoCasoDeUso casoDeUso =
                new RegistrarValidacaoNegocialDossieProdutoCasoDeUso(
                        new FakePortaSaida(Uni.createFrom().failure(falha)));

        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar(comandoMinimo()).await().indefinitely()
        );

        assertSame(falha, observada);
    }

    private static ComandoRegistroValidacaoNegocialDossieProduto comandoMinimo() {
        return new ComandoRegistroValidacaoNegocialDossieProduto(123L, null, null);
    }

    private static final class FakePortaSaida
            implements SolicitarRegistroValidacaoNegocialDossieProduto {

        private final Uni<Void> resultado;
        private ComandoRegistroValidacaoNegocialDossieProduto comandoRecebido;

        private FakePortaSaida(Uni<Void> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<Void> registrar(ComandoRegistroValidacaoNegocialDossieProduto comando) {
            comandoRecebido = comando;
            return resultado;
        }
    }
}
