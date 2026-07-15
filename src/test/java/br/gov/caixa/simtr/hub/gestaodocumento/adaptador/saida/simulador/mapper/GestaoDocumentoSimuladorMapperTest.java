package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.mapper;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.dto.GestaoDocumentoSimuladorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GestaoDocumentoSimuladorMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void leContratoSnakeCaseProprioEPreservaSasEValidadeEstruturada() throws Exception {
        String json = """
                {
                  "sas": "sv=simulador&sp=rw&sig=valor-opaco",
                  "validade": {
                    "expira_em": "31/12/2099 23:59:59",
                    "origem": "fixture"
                  },
                  "url_storage": "https://simulador.blob.core.windows.net",
                  "nome_container": "container-simulado"
                }
                """;

        var resposta = OBJECT_MAPPER.readValue(
                json,
                GestaoDocumentoSimuladorResponse.class
        );
        var credencial = new GestaoDocumentoSimuladorMapper().paraDominio(resposta);

        assertEquals("sv=simulador&sp=rw&sig=valor-opaco", credencial.sas());
        assertEquals(
                "31/12/2099 23:59:59",
                ((Map<?, ?>) credencial.validade()).get("expira_em")
        );
        assertSame(resposta.validade(), credencial.validade());
        assertEquals("https://simulador.blob.core.windows.net", credencial.urlStorage());
        assertEquals("container-simulado", credencial.nomeContainer());
    }

    @Test
    void preservaRespostaEQuatroCamposNulosSemNormalizacao() {
        var mapper = new GestaoDocumentoSimuladorMapper();
        var resposta = new GestaoDocumentoSimuladorResponse(null, null, null, null);

        var credencial = mapper.paraDominio(resposta);

        assertNull(mapper.paraDominio(null));
        assertNull(credencial.sas());
        assertNull(credencial.validade());
        assertNull(credencial.urlStorage());
        assertNull(credencial.nomeContainer());
    }
}
