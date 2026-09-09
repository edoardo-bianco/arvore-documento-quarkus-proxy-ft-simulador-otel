package br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Regressao da protecao anterior ao bootstrap; nao inicia Quarkus nem broker. */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ServiceBusEmuladorTestProfileTest {

    private static final String CONEXAO = "quarkus.azure.servicebus.connection-string";
    private static final String NAMESPACE = "quarkus.azure.servicebus.namespace";
    private static final String ERRO_EXTERNO =
            "O teste de emulador exige ausencia de configuracao externa do Azure Service Bus.";
    private static final Map<String, String> EMULADOR = Map.of(
            "quarkus.devservices.enabled", "true",
            "quarkus.azure.servicebus.enabled", "true",
            "quarkus.azure.servicebus.devservices.enabled", "true");

    @TempDir
    Path diretorio;

    @ParameterizedTest
    @ValueSource(strings = {CONEXAO, NAMESPACE, "%test." + CONEXAO, "%test." + NAMESPACE})
    void deveRecusarConfiguracaoExternaCarregadaDeArquivo(String propriedade) {
        var falha = assertThrows(IllegalStateException.class,
                () -> executarComArquivo(propriedade + "=c2-valor-sintetico-restrito\n"));
        assertEquals(ERRO_EXTERNO, falha.getMessage());
        assertNull(falha.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {CONEXAO, NAMESPACE})
    void deveRecusarExpressaoSemResolverOuDivulgarSeuValor(String propriedade) {
        var falha = assertThrows(IllegalStateException.class,
                () -> executarComArquivo(propriedade + "=${C2_SEGREDO_AUSENTE}\n"));
        assertEquals(ERRO_EXTERNO, falha.getMessage());
        assertNull(falha.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {CONEXAO, NAMESPACE})
    void deveContinuarRecusandoPropriedadesDaJvm(String propriedade) {
        var falha = assertThrows(IllegalStateException.class,
                () -> executarComPropriedade(propriedade, "c2-valor-sintetico-restrito"));
        assertEquals(ERRO_EXTERNO, falha.getMessage());
        assertNull(falha.getCause());
    }

    @Test
    void devePermitirEmuladorSemConexaoExternaNoProfileEfetivo() throws IOException {
        assertEquals(EMULADOR, executarComArquivo("%prod." + CONEXAO + "=c2-inativo\n"));
        assertEquals("test", new ServiceBusEmuladorTestProfile().getConfigProfile());
    }

    @Test
    void deveRelerAsFontesSemReaproveitarConfiguracaoAnterior() throws IOException {
        assertThrows(IllegalStateException.class, () -> executarComArquivo(CONEXAO + "=c2-externo\n"));
        assertEquals(EMULADOR, executarComArquivo(""));
    }

    @Test
    void deveFalharSemCausaExternaQuandoNaoConsegueVerificarConfiguracao() {
        var falha = assertThrows(IllegalStateException.class,
                () -> executarComArquivo("sentinela=c2-restrito\\uZZZZ\n"));
        assertEquals("Nao foi possivel verificar a configuracao do teste de emulador.", falha.getMessage());
        assertNull(falha.getCause());
    }

    private Map<String, String> executarComArquivo(String propriedades) throws IOException {
        var arquivo = diretorio.resolve("servicebus-externo.properties");
        // A fonte sintetica prevalece sobre a configuracao local do desenvolvedor.
        var conexaoAusente = propriedades.startsWith(CONEXAO + "=") ? "" : CONEXAO + "=\n";
        var namespaceAusente = propriedades.startsWith(NAMESPACE + "=") ? "" : NAMESPACE + "=\n";
        Files.writeString(arquivo, "config_ordinal=1000\n" + conexaoAusente + namespaceAusente + propriedades);
        return executarComPropriedade("quarkus.config.locations", arquivo.toUri().toString());
    }

    private static Map<String, String> executarComPropriedade(String propriedade, String valor) {
        var anterior = System.getProperty(propriedade);
        try {
            System.setProperty(propriedade, valor);
            return new ServiceBusEmuladorTestProfile().getConfigOverrides();
        } finally {
            if (anterior == null) {
                System.clearProperty(propriedade);
            } else {
                System.setProperty(propriedade, anterior);
            }
        }
    }
}
