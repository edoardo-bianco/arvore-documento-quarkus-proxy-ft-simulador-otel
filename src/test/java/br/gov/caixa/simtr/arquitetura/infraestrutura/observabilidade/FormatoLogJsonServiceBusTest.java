package br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade;

import io.quarkus.logging.json.runtime.JsonFormatter;
import jakarta.json.Json;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.Executors;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FormatoLogJsonServiceBusTest {

    private static final String CONSUMIDOR =
            "br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.Mapper";
    private final JsonFormatter original = new JsonFormatter();
    private final FormatoLogJsonServiceBus formato = new FormatoLogJsonServiceBus(original);

    @Test
    void deveEmitirArrayObjetoENumeroPreservandoMetadados() {
        var registro = registro(CONSUMIDOR);
        registro.setMarker(campos("ocorrencia-1", 4));
        var antes = ler(original.format(registro));
        var depois = ler(formato.format(registro));

        assertEquals("mensagem controlada", depois.getJsonArray("erros")
                .getJsonObject(0).getString("mensagem"));
        assertEquals(4, depois.getInt("linha_json"));
        assertEquals("ocorrencia-1", depois.getString("id_erro"));
        antes.forEach((chave, valor) -> assertEquals(valor, depois.get(chave), chave));
        assertFalse(depois.containsKey("codigo_http"));
        assertEquals("evento.controlado", depois.getString("message"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "br.gov.caixa.simtr.hub",
            "br.gov.caixa.simtr.hub.arquitetura.observabilidade",
            "br.gov.caixa.simtr.monitoramento.dominio.modelo.Tentativa",
            "br.gov.caixa.simtr.orquestrador.aplicacao.Servico",
            "br.gov.caixa.simtr.monitoramento.adaptador.entrada.rest.Resource",
            "br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebusInvalido.Mapper",
            "br.gov.caixa.simtr.monitoramentoInvalido.adaptador.entrada.servicebus.Mapper"
    })
    void devePreservarByteAByteHubEOutrasCategoriasMesmoComMarcador(String categoria) {
        var registro = registro(categoria);
        registro.setMarker(campos("nao-aplicar", 1));
        registro.setMdc(Map.of("evento", "legado", "recurso", "existente"));

        assertEquals(original.format(registro), formato.format(registro));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.Mapper",
            "br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.Mapper",
            "br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.Mapper"
    })
    void deveLimitarExtensaoAosAdaptersServiceBusNovos(String categoria) {
        var registro = registro(categoria);
        registro.setMarker(campos("novo", 2));

        assertEquals("novo", ler(formato.format(registro)).getString("id_erro"));
    }

    @Test
    void devePreservarRegistroElegivelSemMarcadorEspecifico() {
        var registro = registro(CONSUMIDOR);
        assertEquals(original.format(registro), formato.format(registro));
        registro.setMarker("marcador de outra biblioteca");
        assertEquals(original.format(registro), formato.format(registro));
    }

    @Test
    void deveRejeitarSobrescritaDeMetadadosSemExporValor() {
        var registro = registro(CONSUMIDOR);
        registro.setMarker(new CamposLogJson(Json.createObjectBuilder().add("level", "segredo").build()));

        var erro = assertThrows(IllegalArgumentException.class, () -> formato.format(registro));

        assertFalse(erro.getMessage().contains("segredo"));
        assertNull(erro.getCause());
    }

    @Test
    void deveRejeitarThrowableOriginalEmRegistroMarcado() {
        var registro = registro(CONSUMIDOR);
        registro.setMarker(campos("erro", 1));
        registro.setThrown(new IllegalArgumentException("payload secreto"));

        var erro = assertThrows(IllegalArgumentException.class, () -> formato.format(registro));

        assertFalse(erro.getMessage().contains("payload secreto"));
        assertNull(erro.getCause());
    }

    @Test
    void devePreservarEscapingEDelimitadorConfigurado() {
        original.setRecordDelimiter("\n\n");
        var registro = registro(CONSUMIDOR);
        registro.setMarker(campos("aspas \" e quebra\n", 2));
        var saida = formato.format(registro);

        assertEquals("aspas \" e quebra\n", ler(saida).getString("id_erro"));
        assertTrue(saida.endsWith("\n\n"));
        assertEquals(original.isCallerCalculationRequired(), formato.isCallerCalculationRequired());
        assertEquals(original.getHead(null), formato.getHead(null));
        assertEquals(original.getTail(null), formato.getTail(null));
    }

    @Test
    void deveIsolarCamposDeOcorrenciasConcorrentes() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tarefas = java.util.stream.IntStream.range(0, 40).mapToObj(numero ->
                    executor.submit(() -> {
                        var registro = registro(CONSUMIDOR);
                        registro.setMarker(campos("erro-" + numero, numero));
                        var json = ler(formato.format(registro));
                        assertEquals("erro-" + numero, json.getString("id_erro"));
                        assertEquals(numero, json.getInt("linha_json"));
                    })).toList();
            for (var tarefa : tarefas) {
                tarefa.get();
            }
        }
    }

    static CamposLogJson campos(String id, int linha) {
        return new CamposLogJson(Json.createObjectBuilder()
                .add("id_erro", id)
                .add("erros", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("mensagem", "mensagem controlada")))
                .add("linha_json", linha).build());
    }

    static ExtLogRecord registro(String categoria) {
        var registro = new ExtLogRecord(Level.ERROR, "evento.controlado",
                FormatoLogJsonServiceBusTest.class.getName());
        registro.setLoggerName(categoria);
        registro.setMdc(Map.of());
        return registro;
    }

    static jakarta.json.JsonObject ler(String texto) {
        try (var leitor = Json.createReader(new StringReader(texto))) {
            return leitor.readObject();
        }
    }
}
