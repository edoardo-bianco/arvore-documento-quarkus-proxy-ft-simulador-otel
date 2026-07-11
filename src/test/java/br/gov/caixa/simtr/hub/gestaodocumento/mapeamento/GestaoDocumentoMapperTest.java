package br.gov.caixa.simtr.hub.gestaodocumento.mapeamento;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.GestaoDocumentoCredencialContainerVo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class GestaoDocumentoMapperTest {

    @Inject
    GestaoDocumentoMapper mapper;

    @Test
    void deveInjetarMapperGeradoPeloCdi() {
        assertNotNull(mapper);
    }

    @Test
    void devePreservarCredencialContainerComValidadeString() {
        GestaoDocumentoCredencialContainerDto dto = TestFixtures.gestaoDocumentoCredencialContainerDto();

        GestaoDocumentoCredencialContainerVo vo = mapper.toVo(dto);
        GestaoDocumentoCredencialContainerDto dtoFinal = mapper.toDto(vo);

        assertEquals(dto.sas(), dtoFinal.sas());
        assertEquals(dto.validade(), dtoFinal.validade());
        assertEquals(dto.urlStorage(), dtoFinal.urlStorage());
        assertEquals(dto.nomeContainer(), dtoFinal.nomeContainer());
    }

    @Test
    void devePreservarCredencialContainerComValidadeObjeto() {
        GestaoDocumentoCredencialContainerDto dto = new GestaoDocumentoCredencialContainerDto(
                "sas",
                Map.of("time", 1783702800000L),
                "https://storage.example",
                "pre-validacao"
        );

        GestaoDocumentoCredencialContainerDto dtoFinal = mapper.toDto(mapper.toVo(dto));

        assertEquals(dto.validade(), dtoFinal.validade());
    }

    @Test
    void deveRetornarNullParaContratosNulos() {
        assertNull(mapper.toVo(null));
        assertNull(mapper.toDto(null));
    }
}
