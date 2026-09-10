package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ResultadoMonitoramentoContratoTest {

    @Inject
    ObjectMapper json;
    @Inject
    MonitoramentoResultadoServiceBusMapper produtor;
    @Inject
    br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoServiceBusMapper consumidor;

    @Test
    void deveEmitirExatamenteOJsonV1Aprovado() throws Exception {
        var mensagem = produtor.paraMensagem(ResultadoFixture.resultado());

        assertEquals(json.readTree(ResultadoFixture.JSON), json.readTree(mensagem.getBody().toString()));
    }

    @Test
    void deveMontarEnvelopeDeterministicoSemPublicarOuAgendar() {
        var resultado = ResultadoFixture.resultado();

        var mensagem = produtor.paraMensagem(resultado);
        var repetida = produtor.paraMensagem(resultado);

        assertEquals("MON-1:resultado:v1", mensagem.getMessageId());
        assertEquals("ORQ-1", mensagem.getCorrelationId());
        assertEquals("RESULTADO_MONITORAMENTO_DOSSIE_MTR", mensagem.getSubject());
        assertEquals("application/json", mensagem.getContentType());
        assertNull(mensagem.getScheduledEnqueueTime());
        assertTrue(mensagem.getApplicationProperties().isEmpty());
        assertEquals(mensagem.getMessageId(), repetida.getMessageId());
        assertEquals(mensagem.getBody().toString(), repetida.getBody().toString());
    }

    @ParameterizedTest
    @CsvSource({"CONFORME,CONFORME", "NAO_CONFORME,NAO_CONFORME", "PENDENTE_INFORMACAO,NAO_CONFORME",
            "FINALIZADO_CONFORME,CONFORME", "FINALIZADO_INCONFORME,INCONFORME",
            "PENDENTE_INFORMACA,INCONFORME"})
    void devePreservarSeparadamenteSituacaoOriginalECalculadaEntreModelosIndependentes(
            String situacaoMtr, String situacaoPre) {
        var origem = ResultadoFixture.resultado();
        var resultado = new ResultadoMonitoramento(origem.monitoramentoId(), origem.orquestracaoId(),
                origem.idDossiePreValidacao(), origem.idDossieMtr(), origem.resultadoMonitoramento(),
                situacaoMtr, situacaoPre, origem.motivo(), 1,
                origem.iniciadoEm(), origem.concluidoEm(), origem.inputSequenceNumber());
        var mensagem = produtor.paraMensagem(resultado);

        var recebido = consumidor.paraResultado(mensagem.getBody().toString(), mensagem.getMessageId(),
                mensagem.getCorrelationId(), mensagem.getSubject(), mensagem.getContentType());

        assertEquals(json.valueToTree(resultado), json.valueToTree(recebido));
        assertNotEquals(resultado.getClass(), recebido.getClass());
    }

    @ParameterizedTest
    @CsvSource({"1,1,0", "0001,2147483647,9223372036854775807", "9223372036854775807,3,-9223372036854775808"})
    void devePreservarLimitesNumericosSemUsarSequenciaComoContador(String id, int tentativas, long sequencia) {
        var origem = ResultadoFixture.resultado();
        var resultado = new ResultadoMonitoramento(origem.monitoramentoId(), origem.orquestracaoId(),
                origem.idDossiePreValidacao(), id, origem.resultadoMonitoramento(), origem.situacaoMtr(),
                origem.situacaoPreValidacao(), origem.motivo(), tentativas,
                origem.iniciadoEm(), origem.concluidoEm(), sequencia);

        var recebido = ler(produtor.paraMensagem(resultado).getBody().toString());

        assertEquals(json.valueToTree(resultado), json.valueToTree(recebido));
    }


    @ParameterizedTest
    @ValueSource(ints = {0, 3, Integer.MAX_VALUE})
    void devePreservarQuarentenaSemSituacaoMtrDisponivel(int tentativas) throws Exception {
        var resultado = ResultadoFixture.quarentena(tentativas);
        var esperado = (ObjectNode) json.readTree(ResultadoFixture.JSON);
        esperado.put("resultadoMonitoramento", "QUARENTENA");
        esperado.putNull("situacaoMtr");
        esperado.put("situacaoPreValidacao", "QUARENTENA");
        esperado.put("motivo", "PRAZO_MAXIMO");
        esperado.put("tentativasRealizadas", tentativas);

        var mensagem = produtor.paraMensagem(resultado);
        var recebido = ler(mensagem.getBody().toString());

        assertEquals(esperado, json.readTree(mensagem.getBody().toString()));
        assertEquals(json.valueToTree(resultado), json.valueToTree(recebido));
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void quarentenaDeveRejeitarContadorNegativo(boolean produtorAtivo) {
        var resultado = ResultadoFixture.quarentena(-1);
        if (produtorAtivo) {
            assertThrows(MapeamentoResultadoException.class, () -> produtor.paraMensagem(resultado));
        } else {
            ObjectNode arvore = json.valueToTree(resultado);
            arvore.put("schemaVersion", 1);
            String corpo = arvore.toString();
            assertThrows(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                    () -> ler(corpo));
        }
    }

    @Test
    void consumidorDeveAceitarAusenciaDeSituacaoMtrNaQuarentena() throws Exception {
        ObjectNode arvore = json.valueToTree(ResultadoFixture.quarentena(0));
        arvore.put("schemaVersion", 1);
        arvore.remove("situacaoMtr");

        var recebido = ler(arvore.toString());

        assertNull(recebido.situacaoMtr());
        assertEquals(0, recebido.tentativasRealizadas());
        assertEquals("QUARENTENA", recebido.resultadoMonitoramento());
    }

    @ParameterizedTest
    @MethodSource("camposObrigatorios")
    void consumidorDeveRejeitarCampoObrigatorioAusenteOuNulo(String campo, boolean nulo) throws Exception {
        var arvore = (ObjectNode) json.readTree(ResultadoFixture.JSON);
        if (nulo) {
            arvore.putNull(campo);
        } else {
            arvore.remove(campo);
        }
        String corpo = arvore.toString();

        assertThrows(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                () -> ler(corpo));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            schemaVersion | 0
            schemaVersion | 2
            schemaVersion | "1"
            schemaVersion | 1.5
            schemaVersion | 2147483648
            monitoramentoId | ""
            orquestracaoId | " "
            idDossiePreValidacao | ""
            idDossiePreValidacao | 123
            idDossieMtr | "0"
            idDossieMtr | "-1"
            idDossieMtr | "+1"
            idDossieMtr | "1.0"
            idDossieMtr | "1e1"
            idDossieMtr | "9223372036854775808"
            idDossieMtr | "١"
            idDossieMtr | 1
            resultadoMonitoramento | ""
            situacaoMtr | null
            situacaoMtr | ""
            situacaoMtr | " "
            situacaoMtr | "EM_ANALISE"
            tentativasRealizadas | -1
            tentativasRealizadas | 0
            situacaoPreValidacao | " "
            situacaoMtr | 123
            situacaoMtr | []
            situacaoMtr | {}
            motivo | ""
            tentativasRealizadas | "3"
            tentativasRealizadas | 1.5
            tentativasRealizadas | 2147483648
            inputSequenceNumber | "13527"
            inputSequenceNumber | 1.5
            inputSequenceNumber | 9223372036854775808
            inputSequenceNumber | -9223372036854775809
            iniciadoEm | "ontem"
            iniciadoEm | 123
            concluidoEm | "2026-09-04T12:00:00.122Z"
            """)
    void consumidorDeveRejeitarTipoOuValorForaDoContrato(String campo, String valor) throws Exception {
        var arvore = (ObjectNode) json.readTree(ResultadoFixture.JSON);
        arvore.set(campo, json.readTree(valor));
        String corpo = arvore.toString();

        assertThrows(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                () -> ler(corpo));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            monitoramentoId | null
            monitoramentoId | ""
            orquestracaoId | null
            orquestracaoId | " "
            idDossiePreValidacao | null
            idDossiePreValidacao | ""
            idDossieMtr | null
            idDossieMtr | "0"
            idDossieMtr | "9223372036854775808"
            resultadoMonitoramento | null
            resultadoMonitoramento | ""
            situacaoMtr | null
            situacaoMtr | ""
            situacaoMtr | " "
            situacaoMtr | "EM_ANALISE"
            tentativasRealizadas | -1
            tentativasRealizadas | 0
            situacaoPreValidacao | null
            situacaoPreValidacao | " "
            motivo | null
            motivo | ""
            iniciadoEm | null
            concluidoEm | null
            concluidoEm | "2026-09-04T12:00:00.122Z"
            """)
    void produtorDeveValidarAntesDeSerializar(String campo, String valor) throws Exception {
        ObjectNode arvore = json.valueToTree(ResultadoFixture.resultado());
        arvore.set(campo, json.readTree(valor));
        var resultado = json.treeToValue(arvore, ResultadoMonitoramento.class);

        var falha = assertThrows(MapeamentoResultadoException.class, () -> produtor.paraMensagem(resultado));

        assertEquals("MONITORAMENTO_RESULTADO_CONTRATO_INVALIDO", falha.codigoErro());
        assertNull(falha.getCause());
    }

    @Test
    void produtorDeveRejeitarResultadoAusente() {
        var falha = assertThrows(MapeamentoResultadoException.class, () -> produtor.paraMensagem(null));

        assertEquals("MONITORAMENTO_RESULTADO_CONTRATO_INVALIDO", falha.codigoErro());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "[]", "true", "{", "{} {}"})
    void consumidorDeveRejeitarCorpoInvalidoSemExporCausa(String corpo) {
        var falha = assertThrows(
                br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                () -> ler(corpo));

        assertNull(falha.getCause());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void consumidorDeveRejeitarCadaPropriedadeAmqpAusenteOuDivergente(int indice) {
        var propriedades = new String[]{"MON-1:resultado:v1", "ORQ-1",
                "RESULTADO_MONITORAMENTO_DOSSIE_MTR", "application/json"};
        for (String valor : new String[]{null, "divergente"}) {
            propriedades[indice] = valor;
            assertThrows(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                    () -> consumidor.paraResultado(ResultadoFixture.JSON, propriedades[0],
                            propriedades[1], propriedades[2], propriedades[3]));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "true", "conteudo-invalido"})
    void consumidorDeveRejeitarConteudoAposOObjeto(String sufixo) {
        String corpo = ResultadoFixture.JSON + sufixo;

        assertThrows(br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MapeamentoResultadoException.class,
                () -> ler(corpo));
    }

    @Test
    void deveAceitarConclusaoNoMesmoInstanteEIgnorarCamposDesconhecidos() throws Exception {
        var arvore = (ObjectNode) json.readTree(ResultadoFixture.JSON);
        arvore.set("concluidoEm", arvore.get("iniciadoEm"));
        var esperado = ler(arvore.toString());
        arvore.put("DeliveryCount", 99);
        arvore.put("campoFuturo", true);

        var recebido = ler(arvore.toString());

        assertEquals(esperado, recebido);
        assertEquals(recebido.iniciadoEm(), recebido.concluidoEm());
    }

    private br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento ler(String corpo) {
        return consumidor.paraResultado(corpo, "MON-1:resultado:v1", "ORQ-1",
                "RESULTADO_MONITORAMENTO_DOSSIE_MTR", "application/json");
    }

    static Stream<Arguments> camposObrigatorios() {
        return List.of("schemaVersion", "monitoramentoId", "orquestracaoId", "idDossiePreValidacao",
                        "idDossieMtr", "resultadoMonitoramento", "situacaoMtr", "situacaoPreValidacao", "motivo",
                        "tentativasRealizadas", "iniciadoEm", "concluidoEm", "inputSequenceNumber")
                .stream().flatMap(campo -> Stream.of(Arguments.of(campo, true), Arguments.of(campo, false)));
    }
}
