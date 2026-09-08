package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class MonitoramentoEntradaContratoTest {

    private static final String CORPO = """
            {"schemaVersion":1,"monitoramentoId":"MON-1","orquestracaoId":"ORQ-1",
             "idDossiePreValidacao":"pre-externa","idDossieMtr":"0009223372036854775807",
             "tentativaAtual":3,"iniciadoEm":"2026-09-04T12:00:00.123Z",
             "limiteEm":"2026-09-05T12:00:00.123Z","politicaMonitoramentoVersao":"politica-v7"}
            """;

    @Inject
    ObjectMapper json;

    @Inject
    MonitoramentoEntradaServiceBusMapper mapper;

    @Test
    void deveValidarContratoV1EMapearParaModeloProprioPreservandoValores() {
        assertEquals(new TentativaMonitoramento("MON-1", "ORQ-1", "pre-externa",
                "0009223372036854775807", 3, Instant.parse("2026-09-04T12:00:00.123Z"),
                Instant.parse("2026-09-05T12:00:00.123Z"), "politica-v7"), ler(CORPO));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "[]", "true", "{", "{\"segredo\":\"nao-expor\"}", "{} {}"})
    void deveClassificarCorpoInvalidoSemExporConteudoOuCausaDoParser(String corpo) {
        var erro = assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(corpo));

        assertEquals("Contrato de monitoramento invalido.", erro.getMessage());
        assertNull(erro.getCause());
    }

    @ParameterizedTest
    @MethodSource("camposObrigatorios")
    void deveRejeitarCadaCampoAusenteOuNulo(String campo, boolean nulo) throws Exception {
        var arvore = (ObjectNode) json.readTree(CORPO);
        if (nulo) {
            arvore.putNull(campo);
        } else {
            arvore.remove(campo);
        }
        var corpo = json.writeValueAsString(arvore);

        assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(corpo));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            schemaVersion | 0
            schemaVersion | 2
            schemaVersion | 1.5
            schemaVersion | "1"
            tentativaAtual | 0
            tentativaAtual | -1
            tentativaAtual | 1.5
            tentativaAtual | "3"
            tentativaAtual | 2147483648
            monitoramentoId | ""
            orquestracaoId | " "
            idDossiePreValidacao | ""
            idDossiePreValidacao | 123
            idDossieMtr | "0"
            idDossieMtr | "-1"
            idDossieMtr | "9223372036854775808"
            idDossieMtr | "abc"
            idDossieMtr | "١"
            idDossieMtr | " 1"
            idDossieMtr | 123
            iniciadoEm | "ontem"
            iniciadoEm | 123
            limiteEm | "2026-09-04T12:00:00.123Z"
            limiteEm | "2026-09-04T11:59:59Z"
            politicaMonitoramentoVersao | ""
            """)
    void deveRejeitarValoresOuTiposForaDoContrato(String campo, String valorJson) throws Exception {
        var arvore = (ObjectNode) json.readTree(CORPO);
        arvore.set(campo, json.readTree(valorJson));
        var corpo = json.writeValueAsString(arvore);

        assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(corpo));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void deveRejeitarCadaPropriedadeAmqpAusenteOuDivergente(int indice) {
        var propriedades = new String[] {"MON-1:tentativa:3", "ORQ-1", "MONITORAR_DOSSIE_MTR", "application/json"};
        propriedades[indice] = null;
        assertThrows(ContratoMonitoramentoInvalidoException.class, () -> lerComPropriedades(propriedades));
        propriedades[indice] = "divergente";
        assertThrows(ContratoMonitoramentoInvalidoException.class, () -> lerComPropriedades(propriedades));
    }

    @Test
    void deveIgnorarCamposExtrasSemUsarDeliveryCountComoTentativaOuTrocarPolitica() throws Exception {
        var arvore = (ObjectNode) json.readTree(CORPO);
        arvore.put("DeliveryCount", 99);
        arvore.put("SequenceNumber", 13527);
        arvore.put("campoFuturo", true);
        var corpo = json.writeValueAsString(arvore);

        assertEquals(ler(CORPO), ler(corpo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "true", "texto-invalido"})
    void deveRejeitarConteudoAposObjetoValido(String sufixo) {
        var corpo = CORPO + sufixo;

        assertThrows(ContratoMonitoramentoInvalidoException.class, () -> ler(corpo));
    }

    private TentativaMonitoramento ler(String corpo) {
        return mapper.paraTentativa(corpo, "MON-1:tentativa:3", "ORQ-1",
                "MONITORAR_DOSSIE_MTR", "application/json");
    }

    private TentativaMonitoramento lerComPropriedades(String[] propriedades) {
        return mapper.paraTentativa(CORPO, propriedades[0], propriedades[1], propriedades[2], propriedades[3]);
    }

    static Stream<Arguments> camposObrigatorios() {
        return Stream.of("schemaVersion", "monitoramentoId", "orquestracaoId", "idDossiePreValidacao",
                "idDossieMtr", "tentativaAtual", "iniciadoEm", "limiteEm", "politicaMonitoramentoVersao")
                .flatMap(campo -> Stream.of(Arguments.of(campo, false), Arguments.of(campo, true)));
    }
}
