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
    private static final String CAMPO_APONTAMENTOS = "apontamentos";
    private static final String RECURSO_MTR = "simtr-parametrizacao";
    private static final String ID_ERRO_CHECKLIST = "checklist-erro";
    private static final String CODIGO_ERRO_CHECKLIST = "MTR-CHECKLIST";
    private static final String MENSAGEM_EXTERNA = "mensagem externa";

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
        assertEquals(true, json.path(CAMPO_APONTAMENTOS).get(0).isNull());
        assertFalse(json.path(CAMPO_APONTAMENTOS).get(1).has("nome"));
        assertFalse(json.path(CAMPO_APONTAMENTOS).get(1).has("orientacao_operador"));
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
        assertEquals(RECURSO_MTR, negocio.erro().recurso());
        assertEquals(ID_ERRO_CHECKLIST, negocio.erro().idErro());
        assertEquals(CODIGO_ERRO_CHECKLIST, negocio.erro().codigoErro());
        assertEquals(MENSAGEM_EXTERNA, negocio.erro().erros().getFirst().mensagem());
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
                RECURSO_MTR,
                ID_ERRO_CHECKLIST,
                CODIGO_ERRO_CHECKLIST,
                Arrays.asList(null, MENSAGEM_EXTERNA),
                null,
                null,
                null);

        MtrBusinessErrorException excecao = assertInstanceOf(
                MtrBusinessErrorException.class,
                ChecklistRestMapper.paraExcecaoRest(falha));

        assertNull(excecao.erro().erros().getFirst());
        assertEquals(MENSAGEM_EXTERNA, excecao.erro().erros().get(1).mensagem());
    }

    private static FalhaConsultaChecklist falha(
            FalhaConsultaChecklist.Tipo tipo,
            Integer status
    ) {
        return new FalhaConsultaChecklist(
                tipo,
                status,
                RECURSO_MTR,
                ID_ERRO_CHECKLIST,
                CODIGO_ERRO_CHECKLIST,
                List.of(MENSAGEM_EXTERNA),
                "detalhe",
                "stacktrace",
                new IllegalStateException("causa"));
    }
}
