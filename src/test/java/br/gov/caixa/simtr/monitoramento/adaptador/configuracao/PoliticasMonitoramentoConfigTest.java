package br.gov.caixa.simtr.monitoramento.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
