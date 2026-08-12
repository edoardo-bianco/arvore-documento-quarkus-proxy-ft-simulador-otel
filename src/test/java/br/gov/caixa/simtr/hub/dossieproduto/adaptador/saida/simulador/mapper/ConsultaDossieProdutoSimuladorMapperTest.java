package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ConsultaDossieProdutoSimuladorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultaDossieProdutoSimuladorMapperTest {

    private static final String FIXTURE =
            "mock/dossieproduto/4324680-v2-consulta-dossie-produto.md";
    private static final String CANAL_CRIACAO = "SIMTRAPI";
    private static final String DATA_CRIACAO = "24/07/2026 08:30:00";
    private static final String DATA_FASE = "24/07/2026 08:31:00";
    private static final String RAZAO_SOCIAL = "EMPRESA SIMULADA";

    @Test
    void leFixturePropriaEMapeiaContratoCompletoComIdentidadeSintetica() {
        var resposta = new MarkdownJsonMockReader(new ObjectMapper())
                .readFirstJsonObject(FIXTURE, ConsultaDossieProdutoSimuladorResponse.class);

        var resultado = new ConsultaDossieProdutoSimuladorMapper().paraDominio(resposta);

        assertEquals(4324680L, resultado.id());
        assertEquals(1000012592L, resultado.chaveCorrelacaoCanal());
        assertNull(resultado.instanciaJbpm());
        assertNull(resultado.numeroNegocio());
        assertEquals(CANAL_CRIACAO, resultado.canalCriacao());
        assertEquals(5402, resultado.unidadeCriacao());
        assertNull(resultado.dataCriacao());
        assertEquals(1, resultado.clientes().size());
        assertEquals("00000000000", resultado.clientes().getFirst().cpf());
        assertEquals("CLIENTE SIMULADO", resultado.clientes().getFirst().nome());
        assertEquals(5032, resultado.processo().id());
        assertNull(resultado.processo().data());
        assertEquals("23/07/2026 10:24:00", resultado.faseAtual().data());
        assertEquals(CANAL_CRIACAO, resultado.situacaoAtual().matricula());
        assertEquals(List.of(), resultado.unidadesTratamento());
        assertEquals(1, resultado.produtosContratados().size());
        assertNull(resultado.produtosContratados().getFirst().id());
        assertNull(resultado.produtosContratados().getFirst().codigoOperacao());
        assertNull(resultado.produtosContratados().getFirst().codigoModalidade());
        assertNull(resultado.produtosContratados().getFirst().nome());
    }

    @Test
    void preservaRespostaCamposListasEElementosNulosSemCompartilharDtoMtr() {
        var resposta = new ConsultaDossieProdutoSimuladorResponse(
                null, null, null, null, null, null, DATA_CRIACAO,
                Arrays.asList(null, new ConsultaDossieProdutoSimuladorResponse.Cliente(
                        null, "00000000000000", null, RAZAO_SOCIAL, "Avalista",
                        40610703L, false
                )),
                null,
                new ConsultaDossieProdutoSimuladorResponse.Fase(
                        null, null, null, DATA_FASE
                ),
                null,
                List.of(),
                Arrays.asList(null, new ConsultaDossieProdutoSimuladorResponse.ProdutoContratado(
                        null, null, null, null
                ))
        );

        var mapper = new ConsultaDossieProdutoSimuladorMapper();
        var resultado = mapper.paraDominio(resposta);

        assertNull(mapper.paraDominio(null));
        assertEquals(DATA_CRIACAO, resultado.dataCriacao());
        assertNull(resultado.clientes().getFirst());
        assertEquals(RAZAO_SOCIAL, resultado.clientes().get(1).razaoSocial());
        assertNull(resultado.processo());
        assertEquals(DATA_FASE, resultado.faseAtual().data());
        assertNull(resultado.situacaoAtual());
        assertEquals(List.of(), resultado.unidadesTratamento());
        assertNull(resultado.produtosContratados().getFirst());
        assertNull(resultado.produtosContratados().get(1).nome());
    }

    @Test
    void fixtureDocumentaGetV2EUsaIdentidadeSintetica() throws IOException {
        var classLoader = Thread.currentThread().getContextClassLoader();
        try (var entrada = classLoader.getResourceAsStream(FIXTURE)) {
            assertNotNull(entrada);
            var markdown = new String(entrada.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");

            assertTrue(markdown.contains(
                    "GET /simtr-dossie-produto/v2/dossie-produto/{id}"));
            assertTrue(markdown.contains("\"cpf\": \"00000000000\""));
            assertTrue(markdown.contains("\"nome\": \"CLIENTE SIMULADO\""));
        }
    }

    @Test
    void declaraNomesJsonExplicitosEmTodoContratoDoSimulador() {
        assertJsonProperties(ConsultaDossieProdutoSimuladorResponse.class,
                "id", "chave_correlacao_canal", "instancia_jbpm", "numero_negocio",
                "canal_criacao", "unidade_criacao", "data_criacao", "clientes", "processo",
                "fase_atual", "situacao_atual", "unidades_tratamento", "produtos_contratados");
        assertJsonProperties(ConsultaDossieProdutoSimuladorResponse.Cliente.class,
                "cpf", "cnpj", "nome", "razao_social", "tipo_vinculo",
                "identificador_negocial_vinculo", "principal");
        assertJsonProperties(ConsultaDossieProdutoSimuladorResponse.Processo.class,
                "id", "nome", "identificador_negocial", "macroprocesso", "data",
                "tratamento_seletivo", "complementacao_seletiva");
        assertJsonProperties(ConsultaDossieProdutoSimuladorResponse.Fase.class,
                "id", "nome", "identificador_negocial", "data");
        assertJsonProperties(ConsultaDossieProdutoSimuladorResponse.Situacao.class,
                "id", "nome", "data", "matricula");
        assertJsonProperties(ConsultaDossieProdutoSimuladorResponse.ProdutoContratado.class,
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
