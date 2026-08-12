package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ConsultaDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultaDossieProdutoRestMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String RECURSO = "simtr-dossie-produto";
    private static final String ID_ERRO = "dossie-erro";
    private static final String CODIGO_ERRO = "MTR-DOSSIE";
    private static final String MENSAGEM = "mensagem externa";
    private static final String DETALHE = "detalhe";
    private static final String STACKTRACE = "stacktrace";
    private static final String DATA_CRIACAO = "24/07/2026 08:30:00";
    private static final String DATA_PROCESSO = "24/07/2026 08:31:00";

    @Test
    void mapeiaContratoPublicoExatoPreservandoNulosListasOrdemEDatas() throws Exception {
        ConsultaDossieProdutoResponse resposta =
                ConsultaDossieProdutoRestMapper.paraResposta(dossieCompleto());

        var json = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(resposta));
        var esperado = OBJECT_MAPPER.readTree("""
                {
                  "id": 4324680,
                  "chave_correlacao_canal": 1000012592,
                  "instancia_jbpm": null,
                  "numero_negocio": null,
                  "canal_criacao": "SIMTRAPI",
                  "unidade_criacao": 5402,
                  "data_criacao": null,
                  "clientes": [
                    {
                      "cpf": "00000000000",
                      "cnpj": null,
                      "nome": "CLIENTE SIMULADO",
                      "razao_social": null,
                      "tipo_vinculo": "Proponente",
                      "identificador_negocial_vinculo": 40610702,
                      "principal": true
                    },
                    null
                  ],
                  "processo": {
                    "id": 5032,
                    "nome": "Concessão Habitacional",
                    "identificador_negocial": 1000016487,
                    "macroprocesso": "HABITAÇÃO",
                    "tratamento_seletivo": true,
                    "complementacao_seletiva": true
                  },
                  "fase_atual": {
                    "id": 5033,
                    "nome": "Recepção de dados e documentos",
                    "identificador_negocial": 1000016488,
                    "data": "23/07/2026 10:24:00"
                  },
                  "situacao_atual": {
                    "id": 1,
                    "nome": "Rascunho",
                    "data": "23/07/2026 10:24:00",
                    "matricula": "SIMTRAPI"
                  },
                  "unidades_tratamento": [],
                  "produtos_contratados": [
                    null,
                    {
                      "id": null,
                      "codigo_operacao": null,
                      "codigo_modalidade": null,
                      "nome": null
                    }
                  ]
                }
                """);

        assertEquals(esperado, json);
        assertNull(resposta.clientes().get(1));
        assertNull(resposta.produtosContratados().getFirst());
        assertFalse(json.path("processo").has("data"));
    }

    @Test
    void preservaCamposObjetosEListasNulosESerializaProcessoDataQuandoPresente() {
        var dossie = new DossieProdutoConsultado(
                null, null, null, null, null, null, DATA_CRIACAO,
                null,
                new DossieProdutoConsultado.Processo(
                        null, null, null, null, DATA_PROCESSO, null, null
                ),
                null, null, null, null
        );

        var resposta = ConsultaDossieProdutoRestMapper.paraResposta(dossie);
        var json = OBJECT_MAPPER.valueToTree(resposta);

        assertTrue(json.has("id"));
        assertTrue(json.path("id").isNull());
        assertTrue(json.path("clientes").isNull());
        assertEquals(DATA_CRIACAO, json.path("data_criacao").textValue());
        assertEquals(DATA_PROCESSO, json.path("processo").path("data").textValue());
        assertTrue(json.path("fase_atual").isNull());
        assertTrue(json.path("situacao_atual").isNull());
        assertTrue(json.path("unidades_tratamento").isNull());
        assertTrue(json.path("produtos_contratados").isNull());
        assertNull(ConsultaDossieProdutoRestMapper.paraResposta(null));
    }

    @Test
    void declaraNomesJsonExplicitosEmTodoContratoPublico() {
        assertJsonProperties(ConsultaDossieProdutoResponse.class,
                "id", "chave_correlacao_canal", "instancia_jbpm", "numero_negocio",
                "canal_criacao", "unidade_criacao", "data_criacao", "clientes", "processo",
                "fase_atual", "situacao_atual", "unidades_tratamento", "produtos_contratados");
        assertJsonProperties(ConsultaDossieProdutoResponse.Cliente.class,
                "cpf", "cnpj", "nome", "razao_social", "tipo_vinculo",
                "identificador_negocial_vinculo", "principal");
        assertJsonProperties(ConsultaDossieProdutoResponse.Processo.class,
                "id", "nome", "identificador_negocial", "macroprocesso", "data",
                "tratamento_seletivo", "complementacao_seletiva");
        assertJsonProperties(ConsultaDossieProdutoResponse.Fase.class,
                "id", "nome", "identificador_negocial", "data");
        assertJsonProperties(ConsultaDossieProdutoResponse.Situacao.class,
                "id", "nome", "data", "matricula");
        assertJsonProperties(ConsultaDossieProdutoResponse.ProdutoContratado.class,
                "id", "codigo_operacao", "codigo_modalidade", "nome");
    }

    @Test
    void traduzTodasAsFalhasInternasSemPerderPayload() {
        var negocio = assertInstanceOf(MtrBusinessErrorException.class,
                ConsultaDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDossieProduto.Tipo.NEGOCIO, 404)));
        assertEquals(404, negocio.status());
        assertEquals(404, negocio.erro().codigoHttp());
        assertEquals(RECURSO, negocio.erro().recurso());
        assertEquals(ID_ERRO, negocio.erro().idErro());
        assertEquals(CODIGO_ERRO, negocio.erro().codigoErro());
        assertEquals(MENSAGEM, negocio.erro().erros().getFirst().mensagem());
        assertEquals(DETALHE, negocio.erro().detalhe());
        assertEquals(STACKTRACE, negocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(MtrClientTechnicalException.class,
                ConsultaDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDossieProduto.Tipo.TECNICA_CLIENTE, 422))).status());
        assertEquals(503, assertInstanceOf(MtrServerErrorException.class,
                ConsultaDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL, 503))).status());

        var timeout = assertInstanceOf(MtrServerErrorException.class,
                ConsultaDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDossieProduto.Tipo.TIMEOUT, null)));
        assertEquals(500, timeout.status());
        assertNull(timeout.erro().codigoHttp());
    }

    @Test
    void preservaListaEElementosNulosNaFalhaPublica() {
        var falhaSemMensagens = new FalhaConsultaDossieProduto(
                FalhaConsultaDossieProduto.Tipo.NEGOCIO,
                404, RECURSO, ID_ERRO, CODIGO_ERRO, null, null, null, null
        );
        var falhaComElementoNulo = new FalhaConsultaDossieProduto(
                FalhaConsultaDossieProduto.Tipo.NEGOCIO,
                404, RECURSO, ID_ERRO, CODIGO_ERRO,
                Arrays.asList(null, MENSAGEM), null, null, null
        );

        var semMensagens = assertInstanceOf(MtrBusinessErrorException.class,
                ConsultaDossieProdutoRestMapper.paraExcecaoRest(falhaSemMensagens));
        var comElementoNulo = assertInstanceOf(MtrBusinessErrorException.class,
                ConsultaDossieProdutoRestMapper.paraExcecaoRest(falhaComElementoNulo));

        assertNull(semMensagens.erro().erros());
        assertNull(comElementoNulo.erro().erros().getFirst());
        assertEquals(MENSAGEM, comElementoNulo.erro().erros().get(1).mensagem());
    }

    private static DossieProdutoConsultado dossieCompleto() {
        return new DossieProdutoConsultado(
                4324680L,
                1000012592L,
                null,
                null,
                "SIMTRAPI",
                5402,
                null,
                Arrays.asList(
                        new DossieProdutoConsultado.Cliente(
                                "00000000000", null, "CLIENTE SIMULADO", null,
                                "Proponente", 40610702L, true
                        ),
                        null
                ),
                new DossieProdutoConsultado.Processo(
                        5032, "Concessão Habitacional", 1000016487L, "HABITAÇÃO",
                        null, true, true
                ),
                new DossieProdutoConsultado.Fase(
                        5033, "Recepção de dados e documentos", 1000016488L,
                        "23/07/2026 10:24:00"
                ),
                new DossieProdutoConsultado.Situacao(
                        1, "Rascunho", "23/07/2026 10:24:00", "SIMTRAPI"
                ),
                List.of(),
                Arrays.asList(
                        null,
                        new DossieProdutoConsultado.ProdutoContratado(null, null, null, null)
                )
        );
    }

    private static FalhaConsultaDossieProduto falha(
            FalhaConsultaDossieProduto.Tipo tipo,
            Integer status
    ) {
        return new FalhaConsultaDossieProduto(
                tipo,
                status,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                List.of(MENSAGEM),
                DETALHE,
                STACKTRACE,
                new IllegalStateException("causa")
        );
    }

    private static void assertJsonProperties(Class<?> tipo, String... nomesEsperados) {
        var nomesDeclarados = Arrays.stream(tipo.getRecordComponents())
                .map(componente -> componente.getAccessor().getAnnotation(JsonProperty.class))
                .map(anotacao -> anotacao == null ? null : anotacao.value())
                .toList();

        assertEquals(List.of(nomesEsperados), nomesDeclarados);
    }
}
