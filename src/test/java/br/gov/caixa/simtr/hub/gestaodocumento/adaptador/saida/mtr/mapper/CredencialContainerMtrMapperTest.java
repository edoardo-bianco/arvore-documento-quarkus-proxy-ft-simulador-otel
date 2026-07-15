package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.dto.v1.credencial.CredencialContainerMtrResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CredencialContainerMtrMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void desserializaSnakeCaseEMapeiaValidadeEstruturadaSemInterpretacao() throws Exception {
        String json = """
                {
                  "sas": "sv=teste&sp=rw&sig=valor-opaco",
                  "validade": {
                    "expira_em": "31/12/2099 23:59:47",
                    "origem": "contrato-mtr"
                  },
                  "url_storage": "https://storage.example.test",
                  "nome_container": "container-teste"
                }
                """;
        var resposta = OBJECT_MAPPER.readValue(json, CredencialContainerMtrResponse.class);

        var credencial = new CredencialContainerMtrMapper().paraDominio(resposta);

        assertEquals("sv=teste&sp=rw&sig=valor-opaco", credencial.sas());
        assertEquals(
                Map.of("expira_em", "31/12/2099 23:59:47", "origem", "contrato-mtr"),
                credencial.validade()
        );
        assertSame(resposta.validade(), credencial.validade());
        assertEquals("https://storage.example.test", credencial.urlStorage());
        assertEquals("container-teste", credencial.nomeContainer());
    }

    @Test
    void preservaValidadeTextualENulabilidade() {
        var mapper = new CredencialContainerMtrMapper();
        var validade = "31/12/2099 23:59:47";
        var resposta = new CredencialContainerMtrResponse(null, validade, null, null);

        var credencial = mapper.paraDominio(resposta);

        assertNull(credencial.sas());
        assertSame(validade, credencial.validade());
        assertNull(credencial.urlStorage());
        assertNull(credencial.nomeContainer());
        assertNull(mapper.paraDominio(null));
    }
}
