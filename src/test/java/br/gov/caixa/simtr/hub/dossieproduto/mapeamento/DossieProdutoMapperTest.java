package br.gov.caixa.simtr.hub.dossieproduto.mapeamento;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialVerificacaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoMapperTest {

    @Inject
    DossieProdutoMapper mapper;

    @Test
    void deveInjetarMapperGeradoPeloCdi() {
        assertNotNull(mapper);
    }

    @Test
    void devePreservarValidacaoNegocialCompleta() {
        DossieProdutoValidacaoNegocialDto dto = TestFixtures.validacaoNegocialDto();

        DossieProdutoValidacaoNegocialVo vo = mapper.toVo(dto);
        DossieProdutoValidacaoNegocialDto dtoFinal = mapper.toDto(vo);

        assertEquals(1122928L, dtoFinal.verificacoes().getFirst().identificadorInstanciaDocumento());
        assertEquals(6592L, dtoFinal.verificacoes().getFirst().identificadorChecklist());
        assertEquals(2, dtoFinal.verificacoes().getFirst().versaoChecklist());
        assertTrue(dtoFinal.verificacoes().getFirst().analiseRealizada());
        assertEquals(3, dtoFinal.verificacoes().getFirst().parecerApontamentos().size());
        assertEquals(1000012877L,
                dtoFinal.verificacoes().getFirst().parecerApontamentos().getFirst().identificadorApontamento());
        assertEquals("APROVADO", dtoFinal.verificacoes().getFirst().parecerApontamentos().getFirst().resultado());
        assertEquals(1.0, dtoFinal.verificacoes().getFirst().parecerApontamentos().getFirst().indiceIa());
        assertEquals(300, dtoFinal.verificacoes().getFirst().garantia().codigoBacen());
        assertEquals("12345678901",
                dtoFinal.verificacoes().getFirst().garantia().clientesAvalistas().getFirst().cpf());
        assertEquals(100, dtoFinal.verificacoes().getFirst().produto().codigoOperacao());
        assertTrue(dtoFinal.verificacoes().getFirst().previo());
        assertEquals(2, dtoFinal.respostasFormulario().size());
        assertEquals(1000011689L, dtoFinal.respostasFormulario().getFirst().campoFormulario());
        assertEquals(List.of("2"), dtoFinal.respostasFormulario().get(1).opcoesSelecionadas());
    }

    @Test
    void deveRetornarNullParaContratosNulos() {
        assertNull(mapper.toVo((DossieProdutoValidacaoNegocialDto) null));
        assertNull(mapper.toDto((DossieProdutoValidacaoNegocialVo) null));
    }

    @Test
    void devePreservarItensNulosEmListasQuandoContratoPermitir() {
        DossieProdutoValidacaoNegocialDto validacaoFinal = mapper.toDto(mapper.toVo(
                new DossieProdutoValidacaoNegocialDto(
                        Collections.singletonList(null),
                        Collections.singletonList(null)
                )
        ));

        assertEquals(1, validacaoFinal.verificacoes().size());
        assertNull(validacaoFinal.verificacoes().getFirst());
        assertEquals(1, validacaoFinal.respostasFormulario().size());
        assertNull(validacaoFinal.respostasFormulario().getFirst());
    }

    @Test
    void devePreservarObjetosAninhadosNulos() {
        DossieProdutoValidacaoNegocialDto validacao = new DossieProdutoValidacaoNegocialDto(
                List.of(new DossieProdutoValidacaoNegocialVerificacaoDto(
                        null,
                        1L,
                        1,
                        true,
                        Collections.singletonList(null),
                        null,
                        null,
                        null
                )),
                null
        );
        DossieProdutoValidacaoNegocialDto validacaoFinal = mapper.toDto(mapper.toVo(validacao));

        assertNull(validacaoFinal.verificacoes().getFirst().identificadorInstanciaDocumento());
        assertEquals(1, validacaoFinal.verificacoes().getFirst().parecerApontamentos().size());
        assertNull(validacaoFinal.verificacoes().getFirst().parecerApontamentos().getFirst());
        assertNull(validacaoFinal.verificacoes().getFirst().garantia());
        assertNull(validacaoFinal.verificacoes().getFirst().produto());
        assertNull(validacaoFinal.verificacoes().getFirst().previo());
        assertNull(validacaoFinal.respostasFormulario());
    }
}
