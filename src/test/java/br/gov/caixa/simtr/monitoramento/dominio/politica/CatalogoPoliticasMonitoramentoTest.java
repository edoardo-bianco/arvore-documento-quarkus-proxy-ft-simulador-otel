package br.gov.caixa.simtr.monitoramento.dominio.politica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento.Decisao;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento.MotivoEncerramento;

class CatalogoPoliticasMonitoramentoTest {

    private static final Instant INICIO = Instant.parse("2026-09-09T12:00:00Z");

    @Test
    void priorizaDefinicaoConfiguradaMesmoQuandoSuaVersaoEhV1() {
        var personalizada = new PoliticaMonitoramentoProgressiva(
                "v1", List.of(Duration.ofMinutes(7)), OptionalInt.of(3), Duration.ofHours(2));
        var catalogo = new CatalogoPoliticasMonitoramento(List.of(personalizada));

        var resolucao = catalogo.resolver("v1");

        assertEquals("v1", resolucao.versaoSolicitada());
        assertSame(personalizada, resolucao.politica());
        assertFalse(resolucao.padraoAplicado());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 100})
    void recuperaV1RemovidaComIntervaloPadraoDeTrintaMinutosSemTetoOpcional(int tentativa) {
        var catalogo = new CatalogoPoliticasMonitoramento(List.of());
        var resolucao = catalogo.resolver("v1");
        var politica = resolucao.politica();

        assertTrue(resolucao.padraoAplicado());
        assertEquals("v1", resolucao.versaoSolicitada());
        assertEquals("v1", politica.versao());
        assertEquals(INICIO.plus(Duration.ofHours(24)), politica.calcularLimite(INICIO));
        assertEquals(new Decisao.Reagendar(tentativa + 1, Duration.ofMinutes(30)),
                politica.avaliarTentativaNaoConclusiva(tentativa, INICIO, politica.calcularLimite(INICIO)));
    }

    @Test
    void versaoAusenteUsaPadroesFixosMesmoComV1PersonalizadaConfigurada() {
        var personalizada = new PoliticaMonitoramentoProgressiva(
                "v1", List.of(Duration.ofMinutes(7)), OptionalInt.of(1), Duration.ofHours(2));
        var catalogo = new CatalogoPoliticasMonitoramento(List.of(personalizada));
        var resolucao = catalogo.resolver("v9");

        assertEquals("v9", resolucao.versaoSolicitada());
        assertEquals("v1", resolucao.politica().versao());
        assertTrue(resolucao.padraoAplicado());
        assertEquals(new Decisao.Reagendar(2, Duration.ofMinutes(30)),
                resolucao.politica().avaliarTentativaNaoConclusiva(1, INICIO, INICIO.plusSeconds(3600)));
    }

    @Test
    void recuperacaoRespeitaPrazoRecebidoSemReiniciarMonitoramento() {
        var tentativa = new TentativaMonitoramento("mon-1", "orq-1", "pre-1", "0007",
                4, INICIO, INICIO.plusSeconds(3600), "v9");
        var catalogo = new CatalogoPoliticasMonitoramento(List.of());
        var resolucao = catalogo.resolver(tentativa.politicaMonitoramentoVersao());

        assertEquals(new Decisao.Encerrar(MotivoEncerramento.PRAZO_MAXIMO),
                resolucao.politica().avaliarTentativaNaoConclusiva(
                        tentativa.tentativaAtual(), tentativa.limiteEm(), tentativa.limiteEm()));
        assertEquals("v9", tentativa.politicaMonitoramentoVersao());
        assertEquals(INICIO, tentativa.iniciadoEm());
        assertEquals(INICIO.plusSeconds(3600), tentativa.limiteEm());
        assertEquals(4, tentativa.tentativaAtual());
    }

    @Test
    void configuracaoCopiadaNaoMudaAposConstrucao() {
        var politica = new PoliticaMonitoramentoProgressiva(
                "v2", List.of(Duration.ofMinutes(7)), OptionalInt.empty(), Duration.ofHours(2));
        var definicoes = new ArrayList<PoliticaMonitoramento>(List.of(politica));
        var catalogo = new CatalogoPoliticasMonitoramento(definicoes);

        definicoes.clear();

        assertSame(politica, catalogo.resolver("v2").politica());
        assertFalse(catalogo.resolver("v2").padraoAplicado());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void versaoInvalidaNaoEhTratadaComoDefinicaoRemovida(String versao) {
        var catalogo = new CatalogoPoliticasMonitoramento(List.of());

        assertThrows(IllegalArgumentException.class, () -> catalogo.resolver(versao));
    }
}
