package br.gov.caixa.simtr.monitoramento.adaptador.saida.acl.simtrhub;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Executa os cenarios no classloader instrumentado pelo Quarkus/JaCoCo do projeto. */
@QuarkusTest
class SituacaoDossieHubAclTest {

    private static final Duration ESPERA = Duration.ofSeconds(2);

    @ParameterizedTest
    @CsvSource({
            "1, 1", "0007, 7", "4324680, 4324680",
            "9223372036854775807, 9223372036854775807",
            "000000000000000000000000000000000000000000001, 1"
    })
    void converteSomenteIdentificadorParaPortaPublica(String textual, long esperado) {
        var recebido = new AtomicReference<Long>();
        var acl = new SituacaoDossieHubAcl(id -> {
            recebido.set(id.valor());
            return Uni.createFrom().item(resposta(1, "Rascunho"));
        });

        var resultado = acl.executar(textual).await().atMost(ESPERA);

        assertEquals(esperado, recebido.get());
        assertEquals(new SituacaoDossieConsultada(1, "Rascunho"), resultado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "0", "000", "-1", "+1", "1.0", "1e3",
            " 1", "1 ", "abc", "١", "１２", "9223372036854775808", "0009223372036854775808"})
    void entradaInvalidaFalhaSemChamarHubOuExporValor(String id) {
        var chamadas = new AtomicInteger();
        var acl = new SituacaoDossieHubAcl(ignorado -> {
            chamadas.incrementAndGet();
            return Uni.createFrom().item(resposta(1, "Rascunho"));
        });
        var consulta = acl.executar(id);

        var aguardando = consulta.await();
        var falha = assertThrows(IllegalArgumentException.class, () -> aguardando.atMost(ESPERA));

        assertEquals(0, chamadas.get());
        assertEquals("Identificador MTR invalido para consulta.", falha.getMessage());
        assertNull(falha.getCause());
    }

    @ParameterizedTest
    @CsvSource({"1, Rascunho", "27, Não Conforme", "88, PENDENTE_INFORMACAO", "-5, DESCONHECIDA", "0, Em analise"})
    void preservaIdENomeSemNormalizarOuClassificar(Integer id, String nome) {
        var acl = new SituacaoDossieHubAcl(ignorado -> Uni.createFrom().item(resposta(id, nome)));

        var resultado = acl.executar("7").await().atMost(ESPERA);

        assertEquals(new SituacaoDossieConsultada(id, nome), resultado);
    }

    @Test
    void preservaIdNuloEEspacosNoNomeOriginal() {
        var acl = new SituacaoDossieHubAcl(ignorado -> Uni.createFrom().item(resposta(null, " Rascunho ")));

        var resultado = acl.executar("7").await().atMost(ESPERA);

        assertNull(resultado.id());
        assertEquals(" Rascunho ", resultado.nome());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void nomeAusenteFalhaSemInventarSituacao(String nome) {
        var acl = new SituacaoDossieHubAcl(ignorado -> Uni.createFrom().item(resposta(1, nome)));

        var aguardando = acl.executar("7").await();
        var falha = assertThrows(IllegalArgumentException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Nome da situacao do dossie obrigatorio.", falha.getMessage());
    }

    @ParameterizedTest
    @MethodSource("respostasAusentes")
    void respostaOuSituacaoAusenteNaoSeTornaSucesso(DossieProdutoConsultado resposta) {
        var acl = new SituacaoDossieHubAcl(ignorado -> Uni.createFrom().item(resposta));

        var aguardando = acl.executar("7").await();
        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Resposta do Hub sem situacao do dossie.", falha.getMessage());
    }

    @Test
    void chamaHubSomenteNaAssinaturaSemRetryAdicional() {
        var chamadas = new AtomicInteger();
        var falha = new IllegalStateException("falha-sintetica-do-fornecedor");
        ConsultarDossieProduto hub = ignorado -> {
            chamadas.incrementAndGet();
            return Uni.createFrom().failure(falha);
        };
        var consulta = new SituacaoDossieHubAcl(hub).executar("7");
        assertEquals(0, chamadas.get());

        var aguardando = consulta.await();
        var recebida = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));

        assertSame(falha, recebida);
        assertEquals(1, chamadas.get());
    }

    @Test
    void falhaSincronaDaPortaTambemEEmitidaNoUni() {
        var falha = new IllegalArgumentException("falha-sintetica-sincrona");
        var acl = new SituacaoDossieHubAcl(ignorado -> {
            throw falha;
        });
        var consulta = acl.executar("7");

        var aguardando = consulta.await();
        var recebida = assertThrows(IllegalArgumentException.class, () -> aguardando.atMost(ESPERA));

        assertSame(falha, recebida);
    }

    static Stream<DossieProdutoConsultado> respostasAusentes() {
        return Stream.of(null, new DossieProdutoConsultado(7L, null, null, null,
                null, null, null, List.of(), null, null, null, List.of(), List.of()));
    }

    private static DossieProdutoConsultado resposta(Integer id, String nome) {
        return new DossieProdutoConsultado(7L, null, null, null, null, null, null,
                List.of(), null, null,
                new DossieProdutoConsultado.Situacao(id, nome, "data-nao-exportada", "matricula-nao-exportada"),
                List.of(), List.of());
    }
}
