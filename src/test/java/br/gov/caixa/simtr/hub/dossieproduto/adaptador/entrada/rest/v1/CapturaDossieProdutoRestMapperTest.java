package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.CapturaDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;

class CapturaDossieProdutoRestMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String RECURSO_MTR = "simtr-dossie-produto";
    private static final String ID_ERRO = "captura-409";
    private static final String CODIGO_ERRO = "MTR-CAPTURA-409";
    private static final String MENSAGEM_OBSERVAVEL = "Falha ao capturar dossie produto";
    private static final String MENSAGEM_EXTERNA = "TOKEN_SENTINELA_BEARER";
    private static final String DETALHE_EXTERNO = "API_KEY_SENTINELA";
    private static final String STACKTRACE_EXTERNO = "STACKTRACE_EXTERNO_SENTINELA";
    private static final String ID_INTERNO_SENTINELA = "id-interno-sentinela";
    private static final String CODIGO_INTERNO_SENTINELA = "codigo-interno-sentinela";
    private static final String CAUSA_INTERNA_SENTINELA = "causa-interna-sentinela";

    @Test
    void serializaSucessoComExatamenteAChaveId() throws Exception {
        var resposta = CapturaDossieProdutoRestMapper.paraResposta(
                new ResultadoCapturaDossieProduto(123L));

        var json = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(resposta));

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":123}"), json);
        assertEquals(1, CapturaDossieProdutoResponse.class.getRecordComponents().length);
        var componente = CapturaDossieProdutoResponse.class.getRecordComponents()[0];
        assertEquals("id", componente.getName());
        assertEquals("id", componente.getAccessor().getAnnotation(JsonProperty.class).value());
        assertEquals(JsonInclude.Include.NON_NULL,
                CapturaDossieProdutoResponse.class.getAnnotation(JsonInclude.class).value());
    }

    @Test
    void preservaResultadoNuloNaBordaRest() {
        assertNull(CapturaDossieProdutoRestMapper.paraResposta(null));
    }

    @Test
    void preservaStatusECorpoMtrValidoSemPerda() {
        var falha = new FalhaCapturaDossieProduto(
                FalhaCapturaDossieProduto.Tipo.NEGOCIO,
                409,
                RECURSO_MTR,
                ID_ERRO,
                CODIGO_ERRO,
                Arrays.asList(MENSAGEM_EXTERNA, null),
                DETALHE_EXTERNO,
                STACKTRACE_EXTERNO,
                new IllegalStateException("causa externa"));

        var excecao = assertInstanceOf(
                MtrBusinessErrorException.class,
                CapturaDossieProdutoRestMapper.paraExcecaoRest(falha));

        assertEquals(409, excecao.status());
        assertEquals(409, excecao.erro().codigoHttp());
        assertEquals(RECURSO_MTR, excecao.erro().recurso());
        assertEquals(ID_ERRO, excecao.erro().idErro());
        assertEquals(CODIGO_ERRO, excecao.erro().codigoErro());
        assertEquals(MENSAGEM_EXTERNA, excecao.erro().erros().getFirst().mensagem());
        assertNull(excecao.erro().erros().get(1));
        assertEquals(DETALHE_EXTERNO, excecao.erro().detalhe());
        assertEquals(STACKTRACE_EXTERNO, excecao.erro().stacktrace());
        assertEquals(MENSAGEM_OBSERVAVEL, excecao.getMessage());
    }

    @Test
    void traduzClassificacoesComStatusParaExcecoesPublicasCorrespondentes() {
        var tecnica = assertInstanceOf(
                MtrClientTechnicalException.class,
                CapturaDossieProdutoRestMapper.paraExcecaoRest(falhaComStatus(
                        FalhaCapturaDossieProduto.Tipo.TECNICA_CLIENTE, 403)));
        var dependencia = assertInstanceOf(
                MtrServerErrorException.class,
                CapturaDossieProdutoRestMapper.paraExcecaoRest(falhaComStatus(
                        FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL, 500)));

        assertEquals(403, tecnica.status());
        assertEquals(500, dependencia.status());
        assertEquals(CODIGO_ERRO, tecnica.erro().codigoErro());
        assertEquals(CODIGO_ERRO, dependencia.erro().codigoErro());
        assertEquals(MENSAGEM_OBSERVAVEL, tecnica.getMessage());
        assertEquals(MENSAGEM_OBSERVAVEL, dependencia.getMessage());
    }

    @Test
    void falhaSemStatusUsa500ECorpoPublicoSeguroParaTodasAsClassificacoes()
            throws Exception {
        for (FalhaCapturaDossieProduto.Tipo tipo : FalhaCapturaDossieProduto.Tipo.values()) {
            var falha = new FalhaCapturaDossieProduto(
                    tipo,
                    null,
                    RECURSO_MTR,
                    ID_INTERNO_SENTINELA,
                    CODIGO_INTERNO_SENTINELA,
                    List.of(MENSAGEM_EXTERNA),
                    DETALHE_EXTERNO,
                    STACKTRACE_EXTERNO,
                    new IllegalStateException(CAUSA_INTERNA_SENTINELA));

            var excecao = assertInstanceOf(
                    MtrServerErrorException.class,
                    CapturaDossieProdutoRestMapper.paraExcecaoRest(falha),
                    tipo.name());
            var erro = excecao.erro();

            assertEquals(500, excecao.status());
            assertEquals(500, erro.codigoHttp());
            assertEquals("simtr-hub", erro.recurso());
            assertNotNull(erro.idErro());
            assertFalse(erro.idErro().isBlank());
            assertEquals("ARVDOCP9999", erro.codigoErro());
            assertEquals("Erro interno ao processar a requisição.",
                    erro.erros().getFirst().mensagem());
            assertNull(erro.detalhe());
            assertNull(erro.stacktrace());

            String json = OBJECT_MAPPER.writeValueAsString(erro);
            assertFalse(json.contains(MENSAGEM_EXTERNA));
            assertFalse(json.contains(DETALHE_EXTERNO));
            assertFalse(json.contains(STACKTRACE_EXTERNO));
            assertFalse(json.contains(CAUSA_INTERNA_SENTINELA));
            assertFalse(json.contains(ID_INTERNO_SENTINELA));
            assertFalse(json.contains(CODIGO_INTERNO_SENTINELA));
        }
    }

    private static FalhaCapturaDossieProduto falhaComStatus(
            FalhaCapturaDossieProduto.Tipo tipo,
            int status
    ) {
        return new FalhaCapturaDossieProduto(
                tipo,
                status,
                RECURSO_MTR,
                ID_ERRO,
                CODIGO_ERRO,
                List.of(MENSAGEM_EXTERNA),
                DETALHE_EXTERNO,
                STACKTRACE_EXTERNO,
                new IllegalStateException("causa externa"));
    }
}
