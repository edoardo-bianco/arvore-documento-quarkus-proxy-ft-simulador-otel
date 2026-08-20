package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ConsultaDocumentosDossieProdutoSimuladorResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConsultaDocumentosDossieProdutoSimuladorMapperTest {

    private static final String FIXTURE =
            "mock/dossieproduto/4081899-v4-consulta-documentos-dossie-produto.md";

    @Inject
    ConsultaDocumentosDossieProdutoSimuladorMapper mapper;

    @Test
    void leFixtureSanitizadaEmWrapperInternoEPreservaOrdemListasENulos() {
        var resposta = new MarkdownJsonMockReader(new ObjectMapper())
                .readFirstJsonObject(
                        FIXTURE,
                        ConsultaDocumentosDossieProdutoSimuladorResponse.class);

        var documentos = mapper.paraDominio(resposta);

        assertNotNull(resposta);
        assertEquals(14, resposta.documentos().size());
        assertEquals(List.of(
                1132220L, 1132221L, 1132178L, 1132224L, 1132225L, 1132226L,
                1132227L, 1132222L, 1132223L, 1132179L, 1132180L, 1132175L,
                1132176L, 1132177L),
                documentos.stream().map(documento -> documento.idInstanciaDocumento()).toList());
        assertEquals("GED-SIMULADO-0001", documentos.getFirst().codigoGed());
        assertEquals("SIM0001", documentos.getFirst().matriculaCaptura());
        assertEquals("00000000000", documentos.getFirst().vinculoDossie().cliente().cpf());
        assertEquals("CLIENTE SIMULADO 01",
                documentos.getFirst().vinculoDossie().cliente().nome());
        assertEquals(List.of(), documentos.getFirst().atributos());
        assertEquals(List.of(), documentos.getFirst().assinaturasDigitais());
        assertEquals(1, documentos.getFirst().armazenamentos().size());
        assertNull(documentos.get(2).codigoGed());
        assertEquals("simulador/documentos/documento-0003.pdf",
                documentos.get(2).armazenamentos().getFirst().pathStorage());
        assertEquals("GED-SIMULADO-0014", documentos.getLast().codigoGed());
    }

    @Test
    void mapeiaContratoCompletoSemCompartilharDtoMtr() {
        var resposta = new ConsultaDocumentosDossieProdutoSimuladorResponse(
                Arrays.asList(documentoCompleto(), null));

        var documentos = mapper.paraDominio(resposta);
        var documento = documentos.getFirst();

        assertEquals(2, documentos.size());
        assertNull(documentos.get(1));
        assertEquals("SIM-0001", documento.tipoDocumento().codigoTipologia());
        assertEquals("CLIENTE SIMULADO", documento.vinculoDossie().cliente().nome());
        assertEquals("PRODUTO", documento.vinculoDossie().produto().nome());
        assertEquals("GARANTIA", documento.vinculoDossie().garantia().nome());
        assertNull(documento.vinculoDossie().garantia().clientesAvalistas().get(1));
        assertEquals("FASE", documento.vinculoDossie().fase().nome());
        assertEquals("MACROPROCESSO", documento.vinculoDossie().processo().macroprocesso());
        assertEquals(Arrays.asList("A", null, "B"),
                documento.atributos().getFirst().opcoesSelecionadas());
        assertNull(documento.atributos().get(1));
        assertEquals("ASSINANTE", documento.assinaturasDigitais().getFirst().nome());
        assertEquals("CHECKLIST", documento.conformidades().getFirst().checklist().nome());
        assertNull(documento.conformidades().getFirst().checklist().apontamentos().get(1));
        assertEquals("valor", documento.propriedades().getFirst().valor());
        assertEquals("CONCLUIDO", documento.outsourcings().getFirst().statusEnvio());
        assertNull(documento.outsourcings().getFirst()
                .resultadoValidacaoNegocial().apontamentos().get(1));
        assertEquals("simulador/documentos/teste.pdf",
                documento.armazenamentos().getFirst().pathStorage());
    }

    @Test
    void converteRespostaOuDocumentosAusentesEmListaVaziaEPreservaColecoesNulas() {
        var respostaSemDocumentos =
                new ConsultaDocumentosDossieProdutoSimuladorResponse(null);
        var respostaComDocumentoVazio = new ConsultaDocumentosDossieProdutoSimuladorResponse(
                List.of(new ConsultaDocumentosDossieProdutoSimuladorResponse.Documento(
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null)));

        var documento = mapper.paraDominio(respostaComDocumentoVazio).getFirst();

        assertEquals(List.of(), mapper.paraDominio(null));
        assertEquals(List.of(), mapper.paraDominio(respostaSemDocumentos));
        assertNull(documento.tipoDocumento());
        assertNull(documento.vinculoDossie());
        assertNull(documento.atributos());
        assertNull(documento.assinaturasDigitais());
        assertNull(documento.conformidades());
        assertNull(documento.propriedades());
        assertNull(documento.outsourcings());
        assertNull(documento.armazenamentos());
    }

    @Test
    void fixtureDocumentaConsultaAprovadaESomenteIdentidadesSinteticas() throws IOException {
        var classLoader = Thread.currentThread().getContextClassLoader();
        try (var entrada = classLoader.getResourceAsStream(FIXTURE)) {
            assertNotNull(entrada);
            var markdown = new String(entrada.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");

            assertEquals("# 4081899", markdown.lines().findFirst().orElseThrow());
            assertTrue(markdown.contains(
                    "GET /simtr-dossie-produto/v4/dossie-produto/4081899/documentos?"
                            + "inclui-armazenamento=true&inclui-assinaturas=true&"
                            + "inclui-atributos=true&inclui-conformidade=true&"
                            + "inclui-outsourcing=true&inclui-propriedades=true"));
            assertTrue(markdown.contains("\"documentos\": ["));
            assertFalse(markdown.contains("\"url\": \"http"));
            assertAusentes(markdown, literaisOriginaisSensiveis());
        }
    }

    @Test
    void declaraNomesJsonExplicitosEmTodoContratoDoSimulador() {
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.class,
                "documentos");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Documento.class,
                "id_instancia_documento", "id_documento", "codigo_ged",
                "data_hora_captura", "data_hora_validade", "matricula_captura",
                "tipo_documento", "situacao_documento", "vinculo_dossie", "url",
                "atributos", "assinaturas_digitais", "conformidade", "propriedades",
                "outsourcing", "armazenamento");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.TipoDocumento.class,
                "id", "nome", "codigo_tipologia", "ativo");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.VinculoDossie.class,
                "cliente", "produto", "garantia", "fase", "processo");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Cliente.class,
                "cpf", "cnpj", "nome", "razao_social", "tipo_vinculo",
                "identificador_negocial_vinculo", "principal");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Produto.class,
                "id", "nome", "modalidade", "operacao");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Garantia.class,
                "id", "nome", "codigo_bacen", "produto", "clientes_avalistas");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.ClienteAvalista.class,
                "cpf", "cnpj");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Fase.class,
                "id", "nome", "identificador_negocial");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Processo.class,
                "id", "nome", "identificador_negocial", "macroprocesso");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Atributo.class,
                "chave", "valor", "opcoes_selecionadas");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.AssinaturaDigital.class,
                "data_assinatura", "emissor", "cpf", "cnpj", "nome",
                "cpf_responsavel_pj", "nome_responsavel_pj");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Conformidade.class,
                "id", "tipo_conformidade", "unidade_conformidade",
                "data_hora_verificacao", "dossie_produto", "fornecedor", "resultado",
                "checklist");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Checklist.class,
                "id", "identificador_negocial", "nome", "versao", "apontamentos");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Apontamento.class,
                "id", "identificador_negocial", "nome", "tipo", "complexidade",
                "aprovado", "orientacao", "comentario");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Propriedade.class,
                "chave", "valor");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Outsourcing.class,
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
                ConsultaDocumentosDossieProdutoSimuladorResponse
                        .ResultadoValidacaoNegocial.class,
                "identificador_checklist", "versao_checklist", "apontamentos");
        assertJsonProperties(
                ConsultaDocumentosDossieProdutoSimuladorResponse
                        .ResultadoValidacaoNegocialApontamento.class,
                "identificador_apontamento", "aprovado", "comentario");
        assertJsonProperties(ConsultaDocumentosDossieProdutoSimuladorResponse.Armazenamento.class,
                "id", "data_hora_armazenamento", "tipo_armazenamento", "path_storage",
                "object_store_ged", "codigo_ged", "data_hora_previsao_exclusao",
                "data_hora_exclusao");
    }

    private static ConsultaDocumentosDossieProdutoSimuladorResponse.Documento documentoCompleto() {
        return new ConsultaDocumentosDossieProdutoSimuladorResponse.Documento(
                1L,
                2L,
                "GED-SIMULADO-TESTE",
                "captura-opaca",
                null,
                "SIM0001",
                new ConsultaDocumentosDossieProdutoSimuladorResponse.TipoDocumento(
                        3L, "DOCUMENTO", "SIM-0001", false),
                "Criado",
                vinculoCompleto(),
                "https://simulador.invalid/documentos/1",
                Arrays.asList(
                        new ConsultaDocumentosDossieProdutoSimuladorResponse.Atributo(
                                "chave", null, Arrays.asList("A", null, "B")),
                        null),
                List.of(new ConsultaDocumentosDossieProdutoSimuladorResponse.AssinaturaDigital(
                        "assinatura-opaca", "EMISSOR SIMULADO", "22222222222", null,
                        "ASSINANTE", null, null)),
                List.of(conformidadeCompleta()),
                List.of(new ConsultaDocumentosDossieProdutoSimuladorResponse.Propriedade(
                        "chave", "valor")),
                List.of(outsourcingCompleto()),
                List.of(new ConsultaDocumentosDossieProdutoSimuladorResponse.Armazenamento(
                        16L, "armazenamento-opaco", "STORAGE_RECEBIDO",
                        "simulador/documentos/teste.pdf", null, null, null, null)));
    }

    private static ConsultaDocumentosDossieProdutoSimuladorResponse.VinculoDossie
            vinculoCompleto() {
        var produto = new ConsultaDocumentosDossieProdutoSimuladorResponse.Produto(
                5, "PRODUTO", 6, 7);
        return new ConsultaDocumentosDossieProdutoSimuladorResponse.VinculoDossie(
                new ConsultaDocumentosDossieProdutoSimuladorResponse.Cliente(
                        "00000000000", null, "CLIENTE SIMULADO", null,
                        "Proponente", 4L, true),
                produto,
                new ConsultaDocumentosDossieProdutoSimuladorResponse.Garantia(
                        8L, "GARANTIA", 9, produto,
                        Arrays.asList(
                                new ConsultaDocumentosDossieProdutoSimuladorResponse
                                        .ClienteAvalista("11111111111", null),
                                null)),
                new ConsultaDocumentosDossieProdutoSimuladorResponse.Fase(10, "FASE", 11L),
                new ConsultaDocumentosDossieProdutoSimuladorResponse.Processo(
                        12, "PROCESSO", 13L, "MACROPROCESSO"));
    }

    private static ConsultaDocumentosDossieProdutoSimuladorResponse.Conformidade
            conformidadeCompleta() {
        var apontamentos = Arrays.asList(
                new ConsultaDocumentosDossieProdutoSimuladorResponse.Apontamento(
                        20L, 21L, "APONTAMENTO", "DOCUMENTAL", "BAIXA", true,
                        "ORIENTACAO", null),
                null);
        var checklist = new ConsultaDocumentosDossieProdutoSimuladorResponse.Checklist(
                18L, 19L, "CHECKLIST", "1", apontamentos);
        return new ConsultaDocumentosDossieProdutoSimuladorResponse.Conformidade(
                14L, "INTERNA", 15L, "verificacao-opaca", 4081899L,
                "FORNECEDOR SIMULADO", "CONFORME", checklist);
    }

    private static ConsultaDocumentosDossieProdutoSimuladorResponse.Outsourcing
            outsourcingCompleto() {
        var resultado = new ConsultaDocumentosDossieProdutoSimuladorResponse
                .ResultadoValidacaoNegocial(
                        22L,
                        1,
                        Arrays.asList(
                                new ConsultaDocumentosDossieProdutoSimuladorResponse
                                        .ResultadoValidacaoNegocialApontamento(
                                                23L, true, "COMENTARIO"),
                                null));
        return new ConsultaDocumentosDossieProdutoSimuladorResponse.Outsourcing(
                "SOLICITACAO", "PROCESSO", true, 24L, "FORNECEDOR-01",
                "FORNECEDOR", "CANAL", "CANAL-01", false, "envio-opaco",
                "CONCLUIDO", true, false, true, false, true, false, true, false,
                true, "M0", "classificacao-opaca", "extracao-opaca",
                "validacao-negocial-opaca", "grafoscopia-opaca", "autenticidade-opaca",
                "imagem-opaca", "validacao-externa-opaca", "consulta-externa-opaca",
                "avaliacao-cadastral-opaca", 0.75, 0.85, "DOC-SIMULADO",
                "REJEICAO", "CONSULTA", "CLASSIFICACAO", "EXTRACAO", resultado);
    }

    private static List<String> literaisOriginaisSensiveis() {
        return List.of(
                "51718219687", "00155905694", "00013888000105", "01226470688",
                "95981985615", "ANTONIA SOUZA DE MOURA",
                "TEREZINHA FERREIRA DA SILVA", "CENTELHA ELETRICA COMERCIAL LTDA",
                "JUSSARA DE LOURDES DOS SANTOS", "JOAQUIM EUGENIO DOS SANTOS",
                "f751917", "SIMTRAPI", "7013429F-0000-C026-A366-CA335E5A825C",
                "7013429F-0000-C346-A919-B52F4524AF77",
                "7013429F-0000-C5B0-804C-654625D1468F",
                "7013429F-0000-C4D5-968E-14432AA3AD01",
                "7013429F-0000-CF63-AC78-7886081A0211",
                "7013429F-0000-C684-AE8A-99EA248A204B",
                "A0B73E9F-0000-C211-B48E-45CF746BF61A",
                "D_cli-ser-mtr/PERFORMANCE_DOC_24365.pdf",
                "cli-ser-mtr/DOC_TESTE_642249.pdf",
                "D_cli-ser-mtr/PERFORMANCE_DOC_13368.pdf",
                "cli-web-mtr-bpm/imagem_teste1.jpg",
                "cli-ser-mtr/DOC_TESTE_642252.pdf",
                "cli-ser-mtr/DOC_TESTE_642290.pdf",
                "D_cli-ser-mtr/PERFORMANCE_DOC_13374.pdf");
    }

    private static void assertAusentes(String conteudo, List<String> literais) {
        literais.forEach(literal -> assertFalse(conteudo.contains(literal), literal));
    }

    private static void assertJsonProperties(Class<?> tipo, String... nomesEsperados) {
        var nomesDeclarados = Arrays.stream(tipo.getRecordComponents())
                .map(componente -> componente.getAccessor().getAnnotation(JsonProperty.class))
                .map(anotacao -> anotacao == null ? null : anotacao.value())
                .toList();

        assertEquals(List.of(nomesEsperados), nomesDeclarados);
    }
}
