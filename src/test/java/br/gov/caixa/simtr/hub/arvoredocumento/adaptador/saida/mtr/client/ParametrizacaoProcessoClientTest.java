package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.dto.v2.processo.ProcessoParametrizadoMtrResponse;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.erro.ProcessoParametrizadoMtrException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

class ParametrizacaoProcessoClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SERVICO_MTR = "simtr-parametrizacao";
    private static final String PREFIXO_STATUS = "status ";

    @Test
    void desserializaArvoreMtrSnakeCaseEChecklistComoObjetoOuArray() throws Exception {
        String json = """
                {
                  "identificador_negocial": 100,
                  "nome": "Contratacao",
                  "ativo": true,
                  "ultima_alteracao": "2026-07-14T12:00:00",
                  "indicador_produto_obrigatorio": false,
                  "macroprocesso": {
                    "identificador_negocial": 200,
                    "nome": "Credito",
                    "ativo": true,
                    "ultima_alteracao": null
                  },
                  "relacionamentos": [{
                    "identificador_negocial": 300,
                    "nome": "Proponente",
                    "tipo_pessoa": "PF",
                    "principal": true,
                    "obrigatorio": true,
                    "relacionado": false,
                    "sequencia": false,
                    "campos_formulario": [{
                      "identificador_negocial": 400,
                      "label": "Autoriza?",
                      "obrigatorio": true,
                      "ativo": true,
                      "exibicao_condicional": "produto.ativo",
                      "tamanho_apresentacao": 6,
                      "ordem_apresentacao": 4,
                      "tipo": "SELECAO",
                      "mascara": null,
                      "placeholder": "Selecione",
                      "tamanho_minimo": 1,
                      "tamanho_maximo": 10,
                      "orientacao_preenchimento": "Escolha",
                      "bloquear_edicao": false,
                      "opcoes_disponiveis": [{
                        "valor_opcao": "S",
                        "descricao_opcao": "Sim",
                        "ativo": true
                      }]
                    }],
                    "documentos": []
                  }],
                  "produtos": [{
                    "codigo_operacao": 500,
                    "codigo_modalidade": 501,
                    "nome": "Capital de giro",
                    "campos_formulario": [],
                    "documentos": [{
                      "funcao_documental": {
                        "nome": "Formalizacao",
                        "tipos_documento": [{
                          "codigo_tipologia": "TIP-01",
                          "nome": "Contrato",
                          "permite_reuso": true,
                          "permite_multiplo": false,
                          "ativo": true,
                          "checklist": {"identificador_checklist": 600, "versao_checklist": 1}
                        }],
                        "checklist": null
                      },
                      "tipo_documento": null,
                      "obrigatorio": true
                    }],
                    "garantias": [{
                      "codigo_bacen": 700,
                      "nome_garantia": "Aval",
                      "fidejussoria": true,
                      "campos_formulario": [],
                      "documentos": [],
                      "checklist": null
                    }],
                    "checklist": [{"identificador_checklist": "800", "versao_checklist": "2"}]
                  }],
                  "fases": [{
                    "identificador_negocial": 900,
                    "nome": "Formalizacao",
                    "ativo": true,
                    "ultima_alteracao": null,
                    "ordem": 2,
                    "orientacao_usuario": "Envie os documentos",
                    "produtos": [],
                    "garantias": [],
                    "campos_formulario": [],
                    "documentos": [],
                    "checklist": [{"identificador_checklist": 901, "versao_checklist": 3}]
                  }],
                  "documentos": [],
                  "checklist": [{"identificador_checklist": 1000, "versao_checklist": 5}]
                }
                """;

        var resposta = OBJECT_MAPPER.readValue(json, ProcessoParametrizadoMtrResponse.class);

        assertEquals(100L, resposta.identificadorNegocial());
        assertEquals("2026-07-14T12:00:00", resposta.ultimaAlteracao());
        assertEquals(200L, resposta.macroprocesso().identificadorNegocial());
        assertNull(resposta.macroprocesso().ultimaAlteracao());
        assertEquals("PF", resposta.relacionamentos().getFirst().tipoPessoa());
        assertEquals("produto.ativo", resposta.relacionamentos().getFirst()
                .camposFormulario().getFirst().exibicaoCondicional());
        assertEquals("S", resposta.relacionamentos().getFirst().camposFormulario().getFirst()
                .opcoesDisponiveis().getFirst().valorOpcao());
        assertEquals("TIP-01", resposta.produtos().getFirst().documentos().getFirst()
                .funcaoDocumental().tiposDocumento().getFirst().codigoTipologia());
        assertEquals(600L, resposta.produtos().getFirst().documentos().getFirst()
                .funcaoDocumental().tiposDocumento().getFirst().checklist()
                .identificadorChecklist());
        assertEquals(800L, resposta.produtos().getFirst().checklist().identificadorChecklist());
        assertEquals(2, resposta.produtos().getFirst().checklist().versaoChecklist());
        assertEquals(901L, resposta.fases().getFirst().checklist().getFirst()
                .identificadorChecklist());
        assertEquals(1000L, resposta.checklist().identificadorChecklist());
    }

    @Test
    void preservaPayloadDoErroDeNegocio() {
        var erro = new ProcessoParametrizadoMtrException.Erro(
                404,
                SERVICO_MTR,
                "processo-404",
                "MTR-PROCESSO-404",
                List.of(new ProcessoParametrizadoMtrException.Mensagem("processo nao localizado")),
                "negocio",
                "stacktrace externo"
        );
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ProcessoParametrizadoMtrException.Erro.class)).thenReturn(erro);

        var falha = assertInstanceOf(
                ProcessoParametrizadoMtrException.Negocio.class,
                ParametrizacaoProcessoClient.toException(response)
        );

        assertEquals(404, falha.status());
        assertEquals(erro, falha.erro());
    }

    @Test
    void normalizaSomenteCamposAusentesDoPayloadDeErro() {
        var erroIncompleto = new ProcessoParametrizadoMtrException.Erro(
                null, null, null, null, null, null, null
        );
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ProcessoParametrizadoMtrException.Erro.class))
                .thenReturn(erroIncompleto);

        var falha = assertInstanceOf(
                ProcessoParametrizadoMtrException.Negocio.class,
                ParametrizacaoProcessoClient.toException(response)
        );

        assertEquals(404, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertNotNull(falha.erro().idErro());
        assertNull(falha.erro().codigoErro());
        assertNull(falha.erro().erros());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
    }

    @Test
    void preservaClassificacaoDeStatusDoClienteLegado() {
        assertNull(ParametrizacaoProcessoClient.toException(responseSemPayload(399)));

        for (int status : new int[]{400, 404, 409, 422}) {
            assertInstanceOf(ProcessoParametrizadoMtrException.Negocio.class,
                    ParametrizacaoProcessoClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status);
        }
        for (int status : new int[]{401, 403, 405, 429}) {
            assertInstanceOf(ProcessoParametrizadoMtrException.TecnicaCliente.class,
                    ParametrizacaoProcessoClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status);
        }
        for (int status : new int[]{500, 502, 503}) {
            assertInstanceOf(ProcessoParametrizadoMtrException.Servidor.class,
                    ParametrizacaoProcessoClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status);
        }
    }

    @Test
    void usaFallbackContratualQuandoPayloadDeErroForInvalido() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ProcessoParametrizadoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        var falha = assertInstanceOf(
                ProcessoParametrizadoMtrException.Servidor.class,
                ParametrizacaoProcessoClient.toException(response)
        );

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertEquals("ARVDOCP0002", falha.erro().codigoErro());
        assertEquals("Erro retornado pelo serviço MTR fora do contrato esperado.",
                falha.erro().erros().getFirst().mensagem());
    }

    @Test
    void preservaMatrizDeFaultToleranceNoNovoClient() throws NoSuchMethodException {
        Method method = ParametrizacaoProcessoClient.class.getMethod(
                "consultarPorIdentificadorNegocial", Long.class
        );

        Timeout timeout = method.getAnnotation(Timeout.class);
        assertEquals(2_000, timeout.value());
        assertEquals(ChronoUnit.MILLIS, timeout.unit());

        Retry retry = method.getAnnotation(Retry.class);
        assertEquals(3, retry.maxRetries());
        assertEquals(300, retry.delay());
        assertEquals(ChronoUnit.MILLIS, retry.delayUnit());
        assertEquals(100, retry.jitter());
        assertEquals(ChronoUnit.MILLIS, retry.jitterDelayUnit());
        assertArrayEquals(new Class<?>[]{
                ProcessoParametrizadoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ProcessoParametrizadoMtrException.Negocio.class,
                ProcessoParametrizadoMtrException.TecnicaCliente.class
        }, retry.abortOn());

        CircuitBreaker circuitBreaker = method.getAnnotation(CircuitBreaker.class);
        assertEquals(10, circuitBreaker.requestVolumeThreshold());
        assertEquals(0.5, circuitBreaker.failureRatio());
        assertEquals(10_000, circuitBreaker.delay());
        assertEquals(ChronoUnit.MILLIS, circuitBreaker.delayUnit());
        assertEquals(2, circuitBreaker.successThreshold());
        assertArrayEquals(retry.retryOn(), circuitBreaker.failOn());
        assertArrayEquals(retry.abortOn(), circuitBreaker.skipOn());
    }

    private static Response responseSemPayload(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(false);
        return response;
    }
}
