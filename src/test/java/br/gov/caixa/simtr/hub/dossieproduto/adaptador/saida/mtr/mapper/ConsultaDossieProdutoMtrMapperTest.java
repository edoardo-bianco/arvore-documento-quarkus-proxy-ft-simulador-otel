package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.consulta.ConsultaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultaDossieProdutoMtrMapperTest {

    private static final String CANAL_CRIACAO = "SIMTRAPI";
    private static final String DATA_RESPOSTA = "23/07/2026 10:24:00";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void desserializaEMapeiaContratoCompletoPreservandoNulosOrdemEDatasTextuais()
            throws Exception {
        var resposta = OBJECT_MAPPER.readValue("""
                {
                  "id": 4324680,
                  "chave_correlacao_canal": 1000012592,
                  "instancia_jbpm": null,
                  "numero_negocio": null,
                  "canal_criacao": "SIMTRAPI",
                  "unidade_criacao": 5402,
                  "data_criacao": "23/07/2026 10:24:00",
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
                    null,
                    {
                      "cpf": null,
                      "cnpj": "00000000000000",
                      "nome": null,
                      "razao_social": "EMPRESA SIMULADA",
                      "tipo_vinculo": "Avalista",
                      "identificador_negocial_vinculo": 40610703,
                      "principal": false
                    }
                  ],
                  "processo": {
                    "id": 5032,
                    "nome": "Concessao Habitacional",
                    "identificador_negocial": 1000016487,
                    "macroprocesso": "HABITACAO",
                    "tratamento_seletivo": true,
                    "complementacao_seletiva": true
                  },
                  "fase_atual": {
                    "id": 5033,
                    "nome": "Recepcao de dados e documentos",
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
                    {
                      "id": 12,
                      "codigo_operacao": 101,
                      "codigo_modalidade": 202,
                      "nome": "PRODUTO SIMULADO"
                    },
                    null,
                    {
                      "id": null,
                      "codigo_operacao": null,
                      "codigo_modalidade": null,
                      "nome": null
                    }
                  ]
                }
                """, ConsultaDossieProdutoMtrResponse.class);

        var resultado = new ConsultaDossieProdutoMtrMapper().paraDominio(resposta);

        assertCabecalho(resultado);
        assertClientes(resultado);
        assertProcessoEFase(resultado);
        assertSituacaoEListas(resultado);
        assertProdutos(resultado);
    }

    private static void assertCabecalho(DossieProdutoConsultado resultado) {
        assertEquals(4324680L, resultado.id());
        assertEquals(1000012592L, resultado.chaveCorrelacaoCanal());
        assertNull(resultado.instanciaJbpm());
        assertNull(resultado.numeroNegocio());
        assertEquals(CANAL_CRIACAO, resultado.canalCriacao());
        assertEquals(5402, resultado.unidadeCriacao());
        assertEquals(DATA_RESPOSTA, resultado.dataCriacao());
    }

    private static void assertClientes(DossieProdutoConsultado resultado) {
        assertEquals(3, resultado.clientes().size());
        assertEquals("00000000000", resultado.clientes().getFirst().cpf());
        assertEquals("Proponente", resultado.clientes().getFirst().tipoVinculo());
        assertNull(resultado.clientes().get(1));
        assertEquals("00000000000000", resultado.clientes().get(2).cnpj());
        assertEquals("EMPRESA SIMULADA", resultado.clientes().get(2).razaoSocial());
    }

    private static void assertProcessoEFase(DossieProdutoConsultado resultado) {
        assertEquals(5032, resultado.processo().id());
        assertEquals(1000016487L, resultado.processo().identificadorNegocial());
        assertEquals("HABITACAO", resultado.processo().macroprocesso());
        assertNull(resultado.processo().data());
        assertEquals(true, resultado.processo().tratamentoSeletivo());
        assertEquals(true, resultado.processo().complementacaoSeletiva());
        assertEquals(5033, resultado.faseAtual().id());
        assertEquals(DATA_RESPOSTA, resultado.faseAtual().data());
    }

    private static void assertSituacaoEListas(DossieProdutoConsultado resultado) {
        assertEquals(1, resultado.situacaoAtual().id());
        assertEquals(CANAL_CRIACAO, resultado.situacaoAtual().matricula());
        assertEquals(List.of(), resultado.unidadesTratamento());
    }

    private static void assertProdutos(DossieProdutoConsultado resultado) {
        assertEquals(3, resultado.produtosContratados().size());
        assertEquals(101, resultado.produtosContratados().getFirst().codigoOperacao());
        assertEquals("PRODUTO SIMULADO", resultado.produtosContratados().getFirst().nome());
        assertNull(resultado.produtosContratados().get(1));
        assertNull(resultado.produtosContratados().get(2).id());
        assertNull(resultado.produtosContratados().get(2).codigoModalidade());
    }

    @Test
    void preservaCamposNulosEListasVazias() {
        var resposta = new ConsultaDossieProdutoMtrResponse(
                null, null, null, null, null, null, null,
                List.of(), null, null, null, List.of(), List.of());

        var resultado = new ConsultaDossieProdutoMtrMapper().paraDominio(resposta);

        assertNull(resultado.id());
        assertNull(resultado.chaveCorrelacaoCanal());
        assertNull(resultado.canalCriacao());
        assertNull(resultado.processo());
        assertNull(resultado.faseAtual());
        assertNull(resultado.situacaoAtual());
        assertEquals(List.of(), resultado.clientes());
        assertEquals(List.of(), resultado.unidadesTratamento());
        assertEquals(List.of(), resultado.produtosContratados());
    }

    @Test
    void mantemRespostaExternaNulaDetectavelPeloAdapter() {
        assertNull(new ConsultaDossieProdutoMtrMapper().paraDominio(null));
    }

    @Test
    void declaraNomesJsonExplicitosEmTodoContratoMtr() {
        assertJsonProperties(ConsultaDossieProdutoMtrResponse.class,
                "id", "chave_correlacao_canal", "instancia_jbpm", "numero_negocio",
                "canal_criacao", "unidade_criacao", "data_criacao", "clientes", "processo",
                "fase_atual", "situacao_atual", "unidades_tratamento", "produtos_contratados");
        assertJsonProperties(ConsultaDossieProdutoMtrResponse.Cliente.class,
                "cpf", "cnpj", "nome", "razao_social", "tipo_vinculo",
                "identificador_negocial_vinculo", "principal");
        assertJsonProperties(ConsultaDossieProdutoMtrResponse.Processo.class,
                "id", "nome", "identificador_negocial", "macroprocesso", "data",
                "tratamento_seletivo", "complementacao_seletiva");
        assertJsonProperties(ConsultaDossieProdutoMtrResponse.Fase.class,
                "id", "nome", "identificador_negocial", "data");
        assertJsonProperties(ConsultaDossieProdutoMtrResponse.Situacao.class,
                "id", "nome", "data", "matricula");
        assertJsonProperties(ConsultaDossieProdutoMtrResponse.ProdutoContratado.class,
                "id", "codigo_operacao", "codigo_modalidade", "nome");
    }

    private static void assertJsonProperties(Class<?> tipo, String... nomesEsperados) {
        var nomesDeclarados = Arrays.stream(tipo.getRecordComponents())
                .map(componente -> componente.getAccessor().getAnnotation(JsonProperty.class))
                .map(anotacao -> anotacao == null ? null : anotacao.value())
                .toList();

        assertEquals(List.of(nomesEsperados), nomesDeclarados);
    }
}
