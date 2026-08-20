package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConsultaDocumentosDossieProdutoMtrMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    ConsultaDocumentosDossieProdutoMtrMapper mapper;

    @Test
    void desserializaEMapeiaContratoCompletoPreservandoNulosListasOrdemEDatasTextuais()
            throws Exception {
        List<ConsultaDocumentosDossieProdutoMtrResponse> respostas =
                OBJECT_MAPPER.readValue(jsonCompleto(), new TypeReference<>() { });

        var documentos = mapper.paraDominio(respostas);

        assertEquals(Arrays.asList(documentoEsperado(), null), documentos);
    }

    @Test
    void converteRespostaNulaEColecaoVaziaEmListaVazia() {
        assertEquals(List.of(), mapper.paraDominio(null));
        assertEquals(List.of(), mapper.paraDominio(List.of()));
    }

    @Test
    void preservaColecoesNulasDoDocumento() {
        var resposta = new ConsultaDocumentosDossieProdutoMtrResponse(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);

        var documento = mapper.paraDominio(List.of(resposta)).getFirst();

        assertNull(documento.atributos());
        assertNull(documento.assinaturasDigitais());
        assertNull(documento.conformidades());
        assertNull(documento.propriedades());
        assertNull(documento.outsourcings());
        assertNull(documento.armazenamentos());
    }

    @Test
    void declaraNomesJsonExplicitosEmTodoContratoMtrV4() {
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.class,
                "id_instancia_documento", "id_documento", "codigo_ged",
                "data_hora_captura", "data_hora_validade", "matricula_captura",
                "tipo_documento", "situacao_documento", "vinculo_dossie", "url",
                "atributos", "assinaturas_digitais", "conformidade", "propriedades",
                "outsourcing", "armazenamento");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.TipoDocumento.class,
                "id", "nome", "codigo_tipologia", "ativo");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.VinculoDossie.class,
                "cliente", "produto", "garantia", "fase", "processo");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Cliente.class,
                "cpf", "cnpj", "nome", "razao_social", "tipo_vinculo",
                "identificador_negocial_vinculo", "principal");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Produto.class,
                "id", "nome", "modalidade", "operacao");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Garantia.class,
                "id", "nome", "codigo_bacen", "produto", "clientes_avalistas");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.ClienteAvalista.class,
                "cpf", "cnpj");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Fase.class,
                "id", "nome", "identificador_negocial");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Processo.class,
                "id", "nome", "identificador_negocial", "macroprocesso");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Atributo.class,
                "chave", "valor", "opcoes_selecionadas");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.AssinaturaDigital.class,
                "data_assinatura", "emissor", "cpf", "cnpj", "nome",
                "cpf_responsavel_pj", "nome_responsavel_pj");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Conformidade.class,
                "id", "tipo_conformidade", "unidade_conformidade",
                "data_hora_verificacao", "dossie_produto", "fornecedor", "resultado",
                "checklist");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Checklist.class,
                "id", "identificador_negocial", "nome", "versao", "apontamentos");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Apontamento.class,
                "id", "identificador_negocial", "nome", "tipo", "complexidade",
                "aprovado", "orientacao", "comentario");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Propriedade.class,
                "chave", "valor");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Outsourcing.class,
                "solicitacao", "processo", "analise_conjunta", "codigo_controle",
                "codigo_fornecedor", "sigla_fornecedor", "sigla_canal", "codigo_canal",
                "retorno_agrupado", "data_hora_envio", "status_envio",
                "solicitacao_tratamento_imagem", "solicitacao_extracao",
                "solicitacao_validacao_negocial", "solicitacao_classificacao",
                "solicitacao_avaliacao_cadastral",
                "solicitacao_avaliacao_autenticidade", "solicitacao_grafoscopia",
                "solicitacao_validacao_externa", "solicitacao_consulta_externa",
                "janela_extracao", "data_hora_retorno_classificacao",
                "data_hora_retorno_extracao", "data_hora_retorno_validacao_negocial",
                "data_hora_retorno_grafoscopia",
                "data_hora_retorno_avaliacao_autenticidade",
                "data_hora_retorno_imagem_tratada", "data_hora_validacao_externa",
                "data_consulta_externa", "data_avaliacao_cadastral",
                "resultado_indice_avaliacao_autenticidade",
                "resultado_indice_grafoscopia", "resultado_codigo_rejeicao",
                "resultado_descricao_rejeicao", "resultado_consulta_externa",
                "resultado_classificacao", "resultado_extracao",
                "resultado_validacao_negocial");
        assertJsonProperties(
                ConsultaDocumentosDossieProdutoMtrResponse.ResultadoValidacaoNegocial.class,
                "identificador_checklist", "versao_checklist", "apontamentos");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse
                        .ResultadoValidacaoNegocialApontamento.class,
                "identificador_apontamento", "aprovado", "comentario");
        assertJsonProperties(ConsultaDocumentosDossieProdutoMtrResponse.Armazenamento.class,
                "id", "data_hora_armazenamento", "tipo_armazenamento", "path_storage",
                "object_store_ged", "codigo_ged", "data_hora_previsao_exclusao",
                "data_hora_exclusao");
    }

    private static DocumentoDossieProdutoConsultado documentoEsperado() {
        var produtoVinculo = new DocumentoDossieProdutoConsultado.Produto(
                10, "PRODUTO VINCULO", 20, 30);
        var produtoGarantia = new DocumentoDossieProdutoConsultado.Produto(
                11, "PRODUTO GARANTIA", 21, 31);
        var apontamento = new DocumentoDossieProdutoConsultado.Apontamento(
                100L, 101L, "APONTAMENTO", "DOCUMENTAL", "BAIXA", true,
                "ORIENTACAO", null);
        var resultadoApontamento =
                new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento(
                        106L, false, "COMENTARIO");

        return new DocumentoDossieProdutoConsultado(
                9000001L,
                9000002L,
                "GED-SIMULADO-0001",
                "captura-opaca",
                null,
                "SIM0001",
                new DocumentoDossieProdutoConsultado.TipoDocumento(
                        9001L, "DOCUMENTO", "SIM-0001", false),
                "Criado",
                new DocumentoDossieProdutoConsultado.VinculoDossie(
                        new DocumentoDossieProdutoConsultado.Cliente(
                                "00000000000", null, "CLIENTE", null,
                                "Vendedor PF", 9000003L, false),
                        produtoVinculo,
                        new DocumentoDossieProdutoConsultado.Garantia(
                                40L, "GARANTIA", 50, produtoGarantia,
                                Arrays.asList(
                                        new DocumentoDossieProdutoConsultado.ClienteAvalista(
                                                "11111111111", null),
                                        null)),
                        new DocumentoDossieProdutoConsultado.Fase(60, "FASE", 70L),
                        new DocumentoDossieProdutoConsultado.Processo(
                                80, "PROCESSO", 90L, "MACROPROCESSO")),
                null,
                Arrays.asList(
                        new DocumentoDossieProdutoConsultado.Atributo(
                                "chave-atributo", null,
                                Arrays.asList("opcao-1", null, "opcao-2")),
                        null),
                List.of(new DocumentoDossieProdutoConsultado.AssinaturaDigital(
                        "assinatura-opaca", "EMISSOR", "22222222222", null,
                        "ASSINANTE", null, null)),
                List.of(new DocumentoDossieProdutoConsultado.Conformidade(
                        104L, "INTERNA", 105L, "verificacao-opaca", 4081899L,
                        "FORNECEDOR", "CONFORME",
                        new DocumentoDossieProdutoConsultado.Checklist(
                                102L, 103L, "CHECKLIST", "1", List.of(apontamento)))),
                List.of(new DocumentoDossieProdutoConsultado.Propriedade(
                        "chave-propriedade", "valor-propriedade")),
                List.of(novoOutsourcing(resultadoApontamento)),
                List.of(new DocumentoDossieProdutoConsultado.Armazenamento(
                        9000004L, "armazenamento-opaco", "GED_RECEBIDO", null,
                        "OBJECT_STORE", "GED-SIMULADO-0001", "previsao-opaca",
                        "exclusao-opaca")));
    }

    private static DocumentoDossieProdutoConsultado.Outsourcing novoOutsourcing(
            DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento apontamento
    ) {
        return new DocumentoDossieProdutoConsultado.Outsourcing(
                "SOLICITACAO",
                "PROCESSO OUTSOURCING",
                true,
                108L,
                "FORNECEDOR-01",
                "FORNECEDOR",
                "CANAL",
                "CANAL-01",
                false,
                "envio-opaco",
                "CONCLUIDO",
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                "M0",
                "classificacao-opaca",
                "extracao-opaca",
                "validacao-negocial-opaca",
                "grafoscopia-opaca",
                "autenticidade-opaca",
                "imagem-opaca",
                "validacao-externa-opaca",
                "consulta-externa-opaca",
                "avaliacao-cadastral-opaca",
                0.75,
                0.85,
                "DOC001",
                "REJEICAO",
                "CONSULTA",
                "CLASSIFICACAO",
                "EXTRACAO",
                new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial(
                        107L, 2, List.of(apontamento)));
    }

    private static String jsonCompleto() {
        return """
                [
                  {
                    "id_instancia_documento": 9000001,
                    "id_documento": 9000002,
                    "codigo_ged": "GED-SIMULADO-0001",
                    "data_hora_captura": "captura-opaca",
                    "data_hora_validade": null,
                    "matricula_captura": "SIM0001",
                    "tipo_documento": {
                      "id": 9001,
                      "nome": "DOCUMENTO",
                      "codigo_tipologia": "SIM-0001",
                      "ativo": false
                    },
                    "situacao_documento": "Criado",
                    "vinculo_dossie": {
                      "cliente": {
                        "cpf": "00000000000",
                        "cnpj": null,
                        "nome": "CLIENTE",
                        "razao_social": null,
                        "tipo_vinculo": "Vendedor PF",
                        "identificador_negocial_vinculo": 9000003,
                        "principal": false
                      },
                      "produto": {
                        "id": 10,
                        "nome": "PRODUTO VINCULO",
                        "modalidade": 20,
                        "operacao": 30
                      },
                      "garantia": {
                        "id": 40,
                        "nome": "GARANTIA",
                        "codigo_bacen": 50,
                        "produto": {
                          "id": 11,
                          "nome": "PRODUTO GARANTIA",
                          "modalidade": 21,
                          "operacao": 31
                        },
                        "clientes_avalistas": [
                          { "cpf": "11111111111", "cnpj": null },
                          null
                        ]
                      },
                      "fase": {
                        "id": 60,
                        "nome": "FASE",
                        "identificador_negocial": 70
                      },
                      "processo": {
                        "id": 80,
                        "nome": "PROCESSO",
                        "identificador_negocial": 90,
                        "macroprocesso": "MACROPROCESSO"
                      }
                    },
                    "url": null,
                    "atributos": [
                      {
                        "chave": "chave-atributo",
                        "valor": null,
                        "opcoes_selecionadas": ["opcao-1", null, "opcao-2"]
                      },
                      null
                    ],
                    "assinaturas_digitais": [
                      {
                        "data_assinatura": "assinatura-opaca",
                        "emissor": "EMISSOR",
                        "cpf": "22222222222",
                        "cnpj": null,
                        "nome": "ASSINANTE",
                        "cpf_responsavel_pj": null,
                        "nome_responsavel_pj": null
                      }
                    ],
                    "conformidade": [
                      {
                        "id": 104,
                        "tipo_conformidade": "INTERNA",
                        "unidade_conformidade": 105,
                        "data_hora_verificacao": "verificacao-opaca",
                        "dossie_produto": 4081899,
                        "fornecedor": "FORNECEDOR",
                        "resultado": "CONFORME",
                        "checklist": {
                          "id": 102,
                          "identificador_negocial": 103,
                          "nome": "CHECKLIST",
                          "versao": "1",
                          "apontamentos": [
                            {
                              "id": 100,
                              "identificador_negocial": 101,
                              "nome": "APONTAMENTO",
                              "tipo": "DOCUMENTAL",
                              "complexidade": "BAIXA",
                              "aprovado": true,
                              "orientacao": "ORIENTACAO",
                              "comentario": null
                            }
                          ]
                        }
                      }
                    ],
                    "propriedades": [
                      { "chave": "chave-propriedade", "valor": "valor-propriedade" }
                    ],
                    "outsourcing": [
                      {
                        "solicitacao": "SOLICITACAO",
                        "processo": "PROCESSO OUTSOURCING",
                        "analise_conjunta": true,
                        "codigo_controle": 108,
                        "codigo_fornecedor": "FORNECEDOR-01",
                        "sigla_fornecedor": "FORNECEDOR",
                        "sigla_canal": "CANAL",
                        "codigo_canal": "CANAL-01",
                        "retorno_agrupado": false,
                        "data_hora_envio": "envio-opaco",
                        "status_envio": "CONCLUIDO",
                        "solicitacao_tratamento_imagem": true,
                        "solicitacao_extracao": false,
                        "solicitacao_validacao_negocial": true,
                        "solicitacao_classificacao": false,
                        "solicitacao_avaliacao_cadastral": true,
                        "solicitacao_avaliacao_autenticidade": false,
                        "solicitacao_grafoscopia": true,
                        "solicitacao_validacao_externa": false,
                        "solicitacao_consulta_externa": true,
                        "janela_extracao": "M0",
                        "data_hora_retorno_classificacao": "classificacao-opaca",
                        "data_hora_retorno_extracao": "extracao-opaca",
                        "data_hora_retorno_validacao_negocial": "validacao-negocial-opaca",
                        "data_hora_retorno_grafoscopia": "grafoscopia-opaca",
                        "data_hora_retorno_avaliacao_autenticidade": "autenticidade-opaca",
                        "data_hora_retorno_imagem_tratada": "imagem-opaca",
                        "data_hora_validacao_externa": "validacao-externa-opaca",
                        "data_consulta_externa": "consulta-externa-opaca",
                        "data_avaliacao_cadastral": "avaliacao-cadastral-opaca",
                        "resultado_indice_avaliacao_autenticidade": 0.75,
                        "resultado_indice_grafoscopia": 0.85,
                        "resultado_codigo_rejeicao": "DOC001",
                        "resultado_descricao_rejeicao": "REJEICAO",
                        "resultado_consulta_externa": "CONSULTA",
                        "resultado_classificacao": "CLASSIFICACAO",
                        "resultado_extracao": "EXTRACAO",
                        "resultado_validacao_negocial": {
                          "identificador_checklist": 107,
                          "versao_checklist": 2,
                          "apontamentos": [
                            {
                              "identificador_apontamento": 106,
                              "aprovado": false,
                              "comentario": "COMENTARIO"
                            }
                          ]
                        }
                      }
                    ],
                    "armazenamento": [
                      {
                        "id": 9000004,
                        "data_hora_armazenamento": "armazenamento-opaco",
                        "tipo_armazenamento": "GED_RECEBIDO",
                        "path_storage": null,
                        "object_store_ged": "OBJECT_STORE",
                        "codigo_ged": "GED-SIMULADO-0001",
                        "data_hora_previsao_exclusao": "previsao-opaca",
                        "data_hora_exclusao": "exclusao-opaca"
                      }
                    ]
                  },
                  null
                ]
                """;
    }

    private static void assertJsonProperties(Class<?> tipo, String... nomesEsperados) {
        var nomesDeclarados = Arrays.stream(tipo.getRecordComponents())
                .map(componente -> componente.getAccessor().getAnnotation(JsonProperty.class))
                .map(anotacao -> anotacao == null ? null : anotacao.value())
                .toList();

        assertEquals(List.of(nomesEsperados), nomesDeclarados);
    }
}
