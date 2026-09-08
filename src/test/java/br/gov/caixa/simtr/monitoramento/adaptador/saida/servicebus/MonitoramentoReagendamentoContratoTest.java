package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.MonitoramentoEntradaServiceBusMapper;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MonitoramentoReagendamentoContratoTest {

    @Inject
    ObjectMapper json;

    @Inject
    MonitoramentoReagendamentoServiceBusMapper mapper;

    @Inject
    MonitoramentoEntradaServiceBusMapper consumidor;

    @Test
    void deveEmitirJsonV1PreservandoOsParametrosOriginais() throws Exception {
        var mensagem = mapper.paraMensagem(ReagendamentoFixture.tentativa());

        assertEquals(json.readTree("""
                {"schemaVersion":1,"monitoramentoId":"MON-1","orquestracaoId":"ORQ-1",
                 "idDossiePreValidacao":"pre-externa","idDossieMtr":"0009223372036854775807",
                 "tentativaAtual":2,"iniciadoEm":"2026-09-04T12:00:00.123Z",
                 "limiteEm":"2026-09-05T12:00:00.123Z","politicaMonitoramentoVersao":"politica-v7"}
                """), json.readTree(mensagem.getBody().toString()));
    }

    @Test
    void deveMontarEnvelopeDeterministicoSemAgendarOuIncrementarATentativa() {
        var tentativa = ReagendamentoFixture.tentativa();

        var mensagem = mapper.paraMensagem(tentativa);
        var repetida = mapper.paraMensagem(tentativa);

        assertEquals("MON-1:tentativa:2", mensagem.getMessageId());
        assertEquals("ORQ-1", mensagem.getCorrelationId());
        assertEquals("MONITORAR_DOSSIE_MTR", mensagem.getSubject());
        assertEquals("application/json", mensagem.getContentType());
        assertNull(mensagem.getScheduledEnqueueTime());
        assertTrue(mensagem.getApplicationProperties().isEmpty());
        assertEquals(mensagem.getMessageId(), repetida.getMessageId());
        assertEquals(mensagem.getBody().toString(), repetida.getBody().toString());
    }

    @ParameterizedTest
    @CsvSource({"1,1", "0001,2", "9223372036854775807,2147483647", "0009223372036854775807,3"})
    void deveSerCompativelComConsumidorNosLimitesInclusivos(String idMtr, int numero) {
        var original = ReagendamentoFixture.tentativa();
        var tentativa = new TentativaMonitoramento(original.monitoramentoId(), original.orquestracaoId(),
                original.idDossiePreValidacao(), idMtr, numero, original.iniciadoEm(), original.limiteEm(),
                original.politicaMonitoramentoVersao());

        var mensagem = mapper.paraMensagem(tentativa);
        var recebida = consumidor.paraTentativa(mensagem.getBody().toString(), mensagem.getMessageId(),
                mensagem.getCorrelationId(), mensagem.getSubject(), mensagem.getContentType());

        assertEquals(tentativa, recebida);
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource(textBlock = """
            monitoramentoId,<nulo>
            monitoramentoId,<vazio>
            monitoramentoId,<branco>
            orquestracaoId,<nulo>
            orquestracaoId,<vazio>
            orquestracaoId,<branco>
            idDossiePreValidacao,<nulo>
            idDossiePreValidacao,<vazio>
            idDossiePreValidacao,<branco>
            politicaMonitoramentoVersao,<nulo>
            politicaMonitoramentoVersao,<vazio>
            politicaMonitoramentoVersao,<branco>
            idDossieMtr,<nulo>
            idDossieMtr,<vazio>
            idDossieMtr,<branco>
            idDossieMtr,0
            idDossieMtr,0000
            idDossieMtr,-1
            idDossieMtr,+1
            idDossieMtr,1.0
            idDossieMtr,1e1
            idDossieMtr,abc
            idDossieMtr,9223372036854775808
            iniciadoEm,<nulo>
            limiteEm,<nulo>
            limiteEm,2026-09-04T12:00:00.123Z
            limiteEm,2026-09-04T12:00:00.122Z
            """)
    void deveRejeitarCamposInvalidosAntesDaSerializacao(String campo, String valor) throws Exception {
        ObjectNode dados = json.valueToTree(ReagendamentoFixture.tentativa());
        switch (valor) {
            case "<nulo>" -> dados.putNull(campo);
            case "<vazio>" -> dados.put(campo, "");
            case "<branco>" -> dados.put(campo, " ");
            default -> dados.put(campo, valor);
        }
        var tentativa = json.treeToValue(dados, TentativaMonitoramento.class);

        var falha = assertThrows(MapeamentoReagendamentoException.class, () -> mapper.paraMensagem(tentativa));

        assertEquals("MONITORAMENTO_REAGENDAMENTO_CONTRATO_INVALIDO", falha.codigoErro());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void deveRejeitarNumeroDaTentativaNaoPositivo(int numero) {
        var original = ReagendamentoFixture.tentativa();
        var tentativa = new TentativaMonitoramento(original.monitoramentoId(), original.orquestracaoId(),
                original.idDossiePreValidacao(), original.idDossieMtr(), numero, original.iniciadoEm(),
                original.limiteEm(), original.politicaMonitoramentoVersao());

        var falha = assertThrows(MapeamentoReagendamentoException.class, () -> mapper.paraMensagem(tentativa));

        assertEquals("MONITORAMENTO_REAGENDAMENTO_CONTRATO_INVALIDO", falha.codigoErro());
    }

    @Test
    void deveClassificarTentativaAusente() {
        var falha = assertThrows(MapeamentoReagendamentoException.class, () -> mapper.paraMensagem(null));

        assertEquals("MONITORAMENTO_REAGENDAMENTO_CONTRATO_INVALIDO", falha.codigoErro());
    }
}
