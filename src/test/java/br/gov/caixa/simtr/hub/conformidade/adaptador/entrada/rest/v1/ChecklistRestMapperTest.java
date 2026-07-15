package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChecklistRestMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void mapeiaRespostaPreservandoContratoJsonNulabilidadeEElementosNulos() {
        ChecklistDto resposta = ChecklistRestMapper.paraResposta(new Checklist(
                "Checklist",
                100L,
                7,
                null,
                "14/07/2026 12:00:00",
                false,
                null,
                Arrays.asList(null, new ApontamentoChecklist(
                        200L, null, "Descricao", null, false, 1))
        ));

        assertEquals("Checklist", resposta.nome());
        assertNull(resposta.apontamentos().getFirst());
        assertEquals(200L, resposta.apontamentos().get(1).identificadorNegocial());

        JsonNode json = OBJECT_MAPPER.valueToTree(resposta);
        assertEquals(100L, json.path("identificador_negocial").longValue());
        assertEquals(7, json.path("versao").intValue());
        assertEquals(false, json.path("verificacao_previa").booleanValue());
        assertFalse(json.has("data_hora_criacao"));
        assertFalse(json.has("orientacao_operador"));
        assertEquals(true, json.path("apontamentos").get(0).isNull());
        assertFalse(json.path("apontamentos").get(1).has("nome"));
        assertFalse(json.path("apontamentos").get(1).has("orientacao_operador"));
    }

    @Test
    void aceitaRespostaNulaEListaNula() {
        assertNull(ChecklistRestMapper.paraResposta(null));

        ChecklistDto resposta = ChecklistRestMapper.paraResposta(new Checklist(
                null, null, null, null, null, null, null, null));

        assertNull(resposta.apontamentos());
        assertEquals(0, OBJECT_MAPPER.valueToTree(resposta).size());
    }

    @Test
    void traduzTodasAsFalhasInternasSemPerderPayload() {
        MtrBusinessErrorException negocio = assertInstanceOf(
                MtrBusinessErrorException.class,
                ChecklistRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaChecklist.Tipo.NEGOCIO, 404)));
        assertEquals(404, negocio.status());
        assertEquals(404, negocio.erro().codigoHttp());
        assertEquals("simtr-parametrizacao", negocio.erro().recurso());
        assertEquals("checklist-erro", negocio.erro().idErro());
        assertEquals("MTR-CHECKLIST", negocio.erro().codigoErro());
        assertEquals("mensagem externa", negocio.erro().erros().getFirst().mensagem());
        assertEquals("detalhe", negocio.erro().detalhe());
        assertEquals("stacktrace", negocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(
                MtrClientTechnicalException.class,
                ChecklistRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaChecklist.Tipo.TECNICA_CLIENTE, 422))).status());
        assertEquals(503, assertInstanceOf(
                MtrServerErrorException.class,
                ChecklistRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL, 503))).status());

        MtrServerErrorException timeout = assertInstanceOf(
                MtrServerErrorException.class,
                ChecklistRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaChecklist.Tipo.TIMEOUT, null)));
        assertEquals(500, timeout.status());
        assertNull(timeout.erro().codigoHttp());
    }

    @Test
    void preservaMensagensNulasNaFalhaPublica() {
        FalhaConsultaChecklist falha = new FalhaConsultaChecklist(
                FalhaConsultaChecklist.Tipo.NEGOCIO,
                404,
                "simtr-parametrizacao",
                "checklist-erro",
                "MTR-CHECKLIST",
                Arrays.asList(null, "mensagem externa"),
                null,
                null,
                null);

        MtrBusinessErrorException excecao = assertInstanceOf(
                MtrBusinessErrorException.class,
                ChecklistRestMapper.paraExcecaoRest(falha));

        assertNull(excecao.erro().erros().getFirst());
        assertEquals("mensagem externa", excecao.erro().erros().get(1).mensagem());
    }

    private static FalhaConsultaChecklist falha(
            FalhaConsultaChecklist.Tipo tipo,
            Integer status
    ) {
        return new FalhaConsultaChecklist(
                tipo,
                status,
                "simtr-parametrizacao",
                "checklist-erro",
                "MTR-CHECKLIST",
                List.of("mensagem externa"),
                "detalhe",
                "stacktrace",
                new IllegalStateException("causa"));
    }
}
