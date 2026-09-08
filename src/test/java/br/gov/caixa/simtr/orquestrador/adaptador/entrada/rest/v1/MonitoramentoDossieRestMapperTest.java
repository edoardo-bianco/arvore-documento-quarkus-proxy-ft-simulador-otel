package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieRequest;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MonitoramentoDossieRestMapperTest {

    @Inject
    ObjectMapper json;

    @Inject
    Validator validator;

    @Test
    void deveLerContratoCamelCaseEMapearParaModeloProprioSemNormalizarIdentificadores() throws Exception {
        var request = json.readValue("""
                {"idDossiePreValidacao":"pre-validacao-externa","idDossieMtr":"004324680"}
                """, IniciarMonitoramentoDossieRequest.class);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals(new SolicitacaoMonitoramento("pre-validacao-externa", "004324680"),
                MonitoramentoDossieRestMapper.paraSolicitacao(request));
    }

    @Test
    void deveIgnorarCamposDesconhecidosSemIncorporarParametrosControladosPeloServidor() throws Exception {
        var request = json.readValue("""
                {"idDossiePreValidacao":"pre-1","idDossieMtr":"4324680",
                 "monitoramentoId":"nao-confiavel","orquestracaoId":"nao-confiavel",
                 "tentativaAtual":999,"limiteEm":"2099-01-01T00:00:00Z",
                 "politicaMonitoramentoVersao":"nao-confiavel","campoFuturo":true}
                """, IniciarMonitoramentoDossieRequest.class);

        assertEquals(json.readTree("""
                {"idDossiePreValidacao":"pre-1","idDossieMtr":"4324680"}
                """), json.valueToTree(request));
        assertEquals(new SolicitacaoMonitoramento("pre-1", "4324680"),
                MonitoramentoDossieRestMapper.paraSolicitacao(request));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void deveRejeitarPreValidacaoAusenteOuEmBranco(String identificador) {
        var request = new IniciarMonitoramentoDossieRequest(identificador, "4324680");

        assertEquals(Set.of("idDossiePreValidacao"), camposInvalidos(request));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "0", "00", "-1", "+1", "1.5", "1e3", "abc", " 1", "1 ", "١",
            "9223372036854775808", "0009223372036854775808",
            "18446744073709551615", "999999999999999999999999999999"})
    void deveRejeitarMtrAusenteOuForaDoContratoLongPositivo(String identificador) {
        var request = new IniciarMonitoramentoDossieRequest("pre-1", identificador);

        assertEquals(Set.of("idDossieMtr"), camposInvalidos(request));
    }

    @Test
    void deveRejeitarObjetoSemOsDoisCamposObrigatorios() throws Exception {
        var request = json.readValue("{}", IniciarMonitoramentoDossieRequest.class);

        assertEquals(Set.of("idDossiePreValidacao", "idDossieMtr"), camposInvalidos(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "0001", "9223372036854775806", "9223372036854775807", "0009223372036854775807"})
    void deveAceitarMtrNosLimitesDoLongPositivoPreservandoStringEZeros(String identificador) throws Exception {
        var request = json.readValue("""
                {"idDossiePreValidacao":"pre-1","idDossieMtr":"%s"}
                """.formatted(identificador), IniciarMonitoramentoDossieRequest.class);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals(new SolicitacaoMonitoramento("pre-1", identificador),
                MonitoramentoDossieRestMapper.paraSolicitacao(request));
        assertEquals(json.getNodeFactory().textNode(identificador), json.valueToTree(request).get("idDossieMtr"));
    }

    @Test
    void deveMapearResultadoInternoParaRespostaComApenasOsDoisIdsTecnicos() throws Exception {
        var iniciado = new MonitoramentoIniciado(
                "MON-c5a13bd2-fc7e-45cd-9c47-8a79b5926c86", "ORQ-919db89b-96a9-4484-bb51-a5abaad03a6a");

        var resposta = MonitoramentoDossieRestMapper.paraResposta(iniciado);

        assertEquals(json.readTree("""
                {"monitoramentoId":"MON-c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
                 "orquestracaoId":"ORQ-919db89b-96a9-4484-bb51-a5abaad03a6a"}
                """), json.valueToTree(resposta));
    }

    private Set<String> camposInvalidos(IniciarMonitoramentoDossieRequest request) {
        return validator.validate(request).stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
