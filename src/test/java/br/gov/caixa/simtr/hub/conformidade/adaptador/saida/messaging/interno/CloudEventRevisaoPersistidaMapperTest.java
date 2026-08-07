package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_CONCLUIDA;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_CORRELATION_ID;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EXTENSAO_FLOW_INSTANCE_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CloudEventRevisaoPersistidaMapperTest {

    private static final String CORRELATION_ID =
            "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String INSTANCE_ID = "01J3FLOWTESTE00000000000000";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloudEventMapper mapper = new CloudEventMapper(objectMapper);
    private final ReferenciasDocumentoAnaliseConformidade referencias =
            new ReferenciasDocumentoAnaliseConformidade(objectMapper);

    @Test
    void criaEventoReferencialDeterministicoAPartirDaRevisaoPersistida() throws Exception {
        var revisao = revisao();
        var referencia = referencias.revisao(CORRELATION_ID, revisao);
        var documento = objectMapper.createObjectNode()
                .put("id", referencia.documentoRef())
                .put("tipo", "revisao-humana")
                .put("instanceId", INSTANCE_ID)
                .put("correlationId", CORRELATION_ID)
                .put("versaoSchema", referencia.versaoSchema())
                .put("hashConteudo", referencia.hashConteudo())
                .set("revisao", objectMapper.valueToTree(revisao));

        var primeiro = mapper.mapearRevisaoPersistida(documento);
        var replay = mapper.mapearRevisaoPersistida(documento.deepCopy());
        var data = objectMapper.readTree(primeiro.getData().toBytes());

        assertAll(
                () -> assertEquals(primeiro.getId(), replay.getId()),
                () -> assertEquals(
                        "conformidade:" + referencia.documentoRef(),
                        primeiro.getId()),
                () -> assertEquals(CloudEventMapper.SOURCE, primeiro.getSource()),
                () -> assertEquals(EVENTO_REVISAO_CONCLUIDA, primeiro.getType()),
                () -> assertNotNull(primeiro.getTime()),
                () -> assertEquals("application/json", primeiro.getDataContentType()),
                () -> assertEquals(
                        INSTANCE_ID,
                        primeiro.getExtension(EXTENSAO_FLOW_INSTANCE_ID)),
                () -> assertEquals(
                        CORRELATION_ID,
                        primeiro.getExtension(EXTENSAO_CORRELATION_ID)),
                () -> assertEquals(3, data.size()),
                () -> assertEquals(referencia.documentoRef(), data.path("documentoRef").asText()),
                () -> assertEquals(referencia.hashConteudo(), data.path("hashConteudo").asText()),
                () -> assertEquals(1, data.path("versaoSchema").asInt()));
    }

    @Test
    void rejeitaDocumentoComHashOuCorrelacaoAdulterados() {
        var revisao = revisao();
        var referencia = referencias.revisao(CORRELATION_ID, revisao);
        var documento = objectMapper.createObjectNode()
                .put("id", referencia.documentoRef())
                .put("tipo", "revisao-humana")
                .put("instanceId", INSTANCE_ID)
                .put("correlationId", "correlacao-adulterada")
                .put("versaoSchema", 1)
                .put("hashConteudo", "0".repeat(64))
                .set("revisao", objectMapper.valueToTree(revisao));

        assertThrows(
                CloudEventInvalidoException.class,
                () -> mapper.mapearRevisaoPersistida(documento));
    }

    private static RevisaoHumanaConformidade revisao() {
        return new RevisaoHumanaConformidade(
                "Aprovado pelo revisor",
                List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.CONFORME,
                        "Confirmado",
                        "Trecho",
                        0.9d)));
    }
}
