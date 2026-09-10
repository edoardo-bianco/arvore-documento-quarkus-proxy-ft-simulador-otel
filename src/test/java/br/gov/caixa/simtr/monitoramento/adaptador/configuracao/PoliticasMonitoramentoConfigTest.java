package br.gov.caixa.simtr.monitoramento.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento.Decisao;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento.MotivoEncerramento;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfigBuilder;

class PoliticasMonitoramentoConfigTest {

    private static final String PREFIXO = "monitoramento.politicas.";
    private static final String DEFINICAO = PREFIXO + "definicoes.fixa.";
    private static final Instant INICIO = Instant.parse("2026-09-06T12:00:00Z");

    @Test
    void selecionaDefinicaoNomeadaSemLimiteDeTentativas() {
        var propriedades = propriedadesValidas();
        var config = mapear(propriedades);
        assertEquals("fixa", config.ativa());
        assertTrue(config.definicoes().get("fixa").maxTentativas().isEmpty());

        var politica = produzir(config);
        assertEquals("fixa-v2", politica.versao());
        assertEquals(INICIO.plus(Duration.ofHours(12)), politica.calcularLimite(INICIO));
        assertEquals(new Decisao.Reagendar(101, Duration.ofMinutes(30)),
                politica.avaliarTentativaNaoConclusiva(100, INICIO, politica.calcularLimite(INICIO)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "PT30M|1|PT30M", "PT30M|100|PT30M",
            "PT3H,PT4H,PT6H|1|PT3H", "PT3H,PT4H,PT6H|2|PT4H",
            "PT3H,PT4H,PT6H|3|PT6H", "PT3H,PT4H,PT6H|4|PT6H", "PT3H,PT4H,PT6H|100|PT6H"})
    void aplicaListaConfiguradaSemTetoDeTentativasAteOPrazo(String intervalos, int tentativa, String esperado) {
        var propriedades = propriedadesValidas();
        propriedades.put(DEFINICAO + "intervalos", intervalos);
        var politica = produzir(mapear(propriedades));
        var limite = politica.calcularLimite(INICIO);

        assertEquals(new Decisao.Reagendar(tentativa + 1, Duration.parse(esperado)),
                politica.avaliarTentativaNaoConclusiva(tentativa, INICIO, limite));
        assertEquals(new Decisao.Encerrar(MotivoEncerramento.PRAZO_MAXIMO),
                politica.avaliarTentativaNaoConclusiva(tentativa, limite, limite));
    }

    @Test
    void selecionaOutraDefinicaoComProgressaoEMaximoOpcional() {
        var propriedades = propriedadesValidas();
        propriedades.put(PREFIXO + "ativa", "progressiva");
        propriedades.put(PREFIXO + "definicoes.progressiva.max-tentativas", "5");
        var politica = produzir(mapear(propriedades));
        var limite = politica.calcularLimite(INICIO);

        assertEquals("progressiva-v3", politica.versao());
        assertEquals(INICIO.plus(Duration.ofHours(24)), limite);
        assertEquals(new Decisao.Reagendar(2, Duration.ofMinutes(10)),
                politica.avaliarTentativaNaoConclusiva(1, INICIO, limite));
        assertEquals(new Decisao.Reagendar(5, Duration.ofHours(2)),
                politica.avaliarTentativaNaoConclusiva(4, INICIO, limite));
        assertEquals(new Decisao.Encerrar(MotivoEncerramento.MAXIMO_TENTATIVAS),
                politica.avaliarTentativaNaoConclusiva(5, INICIO, limite));
    }

    @ParameterizedTest
    @ValueSource(strings = {"inexistente", "   "})
    void rejeitaSelecaoInvalida(String ativa) {
        var propriedades = propriedadesValidas();
        propriedades.put(PREFIXO + "ativa", ativa);
        var config = mapear(propriedades);
        assertThrows(IllegalArgumentException.class, () -> produzir(config));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ativa", "definicoes.fixa.versao", "definicoes.fixa.tipo",
            "definicoes.fixa.intervalos", "definicoes.fixa.duracao-maxima"})
    void exigePropriedadesObrigatorias(String propriedade) {
        var propriedades = propriedadesValidas();
        propriedades.remove(PREFIXO + propriedade);
        assertThrows(ConfigValidationException.class, () -> mapear(propriedades));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "tipo|desconhecida", "intervalos|invalido", "duracao-maxima|invalida", "max-tentativas|abc"})
    void rejeitaValoresQueNaoCorrespondemAoTipo(String propriedade, String valor) {
        var propriedades = propriedadesValidas();
        propriedades.put(DEFINICAO + propriedade, valor);
        assertThrows(ConfigValidationException.class, () -> mapear(propriedades));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "intervalos|PT0S", "intervalos|-PT1M", "intervalos|PT30M,PT0S",
            "duracao-maxima|PT0S", "duracao-maxima|-PT1H", "max-tentativas|0", "max-tentativas|-1"})
    void rejeitaValoresSemanticamenteInvalidos(String propriedade, String valor) {
        var propriedades = propriedadesValidas();
        propriedades.put(DEFINICAO + propriedade, valor);
        var config = mapear(propriedades);
        assertThrows(IllegalArgumentException.class, () -> produzir(config));
    }

    @Test
    void validaTambemDefinicoesInativas() {
        var propriedades = propriedadesValidas();
        propriedades.put(PREFIXO + "definicoes.progressiva.intervalos", "PT0S");
        var config = mapear(propriedades);
        assertThrows(IllegalArgumentException.class, () -> produzir(config));
    }

    @Test
    void rejeitaConfiguracaoNaConstrucaoAntesDeProduzirPolitica() {
        var propriedades = propriedadesValidas();
        propriedades.put(DEFINICAO + "intervalos", "PT0S");
        var config = mapear(propriedades);

        assertThrows(IllegalArgumentException.class, () -> new PoliticaMonitoramentoProducer(config));
    }

    @Test
    void resolveVersaoInativaSemTrocarPoliticaDeNovosMonitoramentos() {
        var producer = new PoliticaMonitoramentoProducer(mapear(propriedadesValidas()));

        var resolucao = producer.catalogo().resolver("progressiva-v3");

        assertEquals("fixa-v2", producer.politica().versao());
        assertEquals("progressiva-v3", resolucao.politica().versao());
        assertFalse(resolucao.padraoAplicado());
        assertEquals(new Decisao.Reagendar(2, Duration.ofMinutes(10)),
                resolucao.politica().avaliarTentativaNaoConclusiva(1, INICIO, INICIO.plusSeconds(3600)));
    }

    @Test
    void ausenciaDaV1RecuperaPadroesSemUsarAConfiguracaoAtiva() {
        var propriedades = propriedadesValidas();
        propriedades.put(DEFINICAO + "intervalos", "PT7M");
        propriedades.put(DEFINICAO + "max-tentativas", "1");
        var producer = new PoliticaMonitoramentoProducer(mapear(propriedades));

        var resolucao = producer.catalogo().resolver("v1");

        assertTrue(resolucao.padraoAplicado());
        assertEquals("v1", resolucao.politica().versao());
        assertEquals(new Decisao.Reagendar(2, Duration.ofMinutes(30)),
                resolucao.politica().avaliarTentativaNaoConclusiva(1, INICIO, INICIO.plusSeconds(3600)));
        assertEquals("fixa-v2", producer.politica().versao());
    }

    @Test
    void rejeitaVersoesDuplicadasEntreDefinicoesNomeadas() {
        var propriedades = propriedadesValidas();
        propriedades.put(PREFIXO + "definicoes.progressiva.versao", "fixa-v2");
        var config = mapear(propriedades);

        assertThrows(IllegalArgumentException.class, () -> new PoliticaMonitoramentoProducer(config));
    }

    private static Map<String, String> propriedadesValidas() {
        var propriedades = new HashMap<String, String>();
        propriedades.put(PREFIXO + "ativa", "fixa");
        propriedades.put(DEFINICAO + "versao", "fixa-v2");
        propriedades.put(DEFINICAO + "tipo", "progressiva");
        propriedades.put(DEFINICAO + "intervalos", "PT30M");
        propriedades.put(DEFINICAO + "duracao-maxima", "PT12H");
        propriedades.put(PREFIXO + "definicoes.progressiva.versao", "progressiva-v3");
        propriedades.put(PREFIXO + "definicoes.progressiva.tipo", "progressiva");
        propriedades.put(PREFIXO + "definicoes.progressiva.intervalos", "PT10M,PT2H");
        propriedades.put(PREFIXO + "definicoes.progressiva.duracao-maxima", "PT24H");
        return propriedades;
    }

    private static PoliticasMonitoramentoConfig mapear(Map<String, String> propriedades) {
        return new SmallRyeConfigBuilder()
                .withMapping(PoliticasMonitoramentoConfig.class)
                .withConverter(Duration.class, 100, new DurationConverter())
                .withDefaultValues(propriedades)
                .build().getConfigMapping(PoliticasMonitoramentoConfig.class);
    }

    private static PoliticaMonitoramento produzir(PoliticasMonitoramentoConfig config) {
        return new PoliticaMonitoramentoProducer(config).politica();
    }
}
