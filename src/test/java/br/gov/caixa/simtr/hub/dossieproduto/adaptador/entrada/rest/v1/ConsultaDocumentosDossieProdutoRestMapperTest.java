package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ConsultaDocumentosDossieProdutoQueryParams;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ConsultaDocumentosDossieProdutoResponse;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaDocumentosDossieProdutoRestMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DATA_REFERENCIA = "01/01/2030 10:00:00";

    @Test
    void mapeiaIdentificadorETodosOsFiltrosSemAlterarValores() {
        var query = new ConsultaDocumentosDossieProdutoQueryParams(
                "00000000000000",
                "00000000000",
                5033L,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                "192.0.2.10",
                "SIM-0001");

        var criterios = ConsultaDocumentosDossieProdutoRestMapper.paraCriterios(
                4081899L, query);

        assertEquals(4081899L, criterios.identificador().valor());
        assertEquals("00000000000000", criterios.cnpj());
        assertEquals("00000000000", criterios.cpf());
        assertEquals(5033L, criterios.fase());
        assertTrue(criterios.incluiArmazenamento());
        assertFalse(criterios.incluiAssinaturas());
        assertTrue(criterios.incluiAtributos());
        assertFalse(criterios.incluiConformidade());
        assertTrue(criterios.incluiOutsourcing());
        assertFalse(criterios.incluiPropriedades());
        assertTrue(criterios.incluiUrl());
        assertEquals("192.0.2.10", criterios.ipUsuario());
        assertEquals("SIM-0001", criterios.tipologia());
    }

    @Test
    void preservaTodosOsFiltrosAusentesSemAplicarDefaults() {
        var criterios = ConsultaDocumentosDossieProdutoRestMapper.paraCriterios(
                4081900L, null);

        assertEquals(4081900L, criterios.identificador().valor());
        assertNull(criterios.cnpj());
        assertNull(criterios.cpf());
        assertNull(criterios.fase());
        assertNull(criterios.incluiArmazenamento());
        assertNull(criterios.incluiAssinaturas());
        assertNull(criterios.incluiAtributos());
        assertNull(criterios.incluiConformidade());
        assertNull(criterios.incluiOutsourcing());
        assertNull(criterios.incluiPropriedades());
        assertNull(criterios.incluiUrl());
        assertNull(criterios.ipUsuario());
        assertNull(criterios.tipologia());
    }

    @Test
    void mapeiaRespostaProfundaPreservandoNulosListasEOrdem() {
        var respostas = ConsultaDocumentosDossieProdutoRestMapper.paraResposta(
                Arrays.asList(documentoCompleto(), null));

        assertEquals(2, respostas.size());
        assertNull(respostas.get(1));

        var resposta = respostas.getFirst();
        assertEquals(9000001L, resposta.idInstanciaDocumento());
        assertEquals(9000002L, resposta.idDocumento());
        assertEquals("GED-SIMULADO-0001", resposta.codigoGed());
        assertEquals(DATA_REFERENCIA, resposta.dataHoraCaptura());
        assertNull(resposta.dataHoraValidade());
        assertNull(resposta.url());
        assertEquals("SIM-0001", resposta.tipoDocumento().codigoTipologia());
        assertEquals("00000000000", resposta.vinculoDossie().cliente().cpf());
        assertEquals(20, resposta.vinculoDossie().produto().modalidade());
        assertNull(resposta.vinculoDossie().garantia().clientesAvalistas().get(1));
        assertEquals(70L, resposta.vinculoDossie().fase().identificadorNegocial());
        assertEquals("MACROPROCESSO SIMULADO",
                resposta.vinculoDossie().processo().macroprocesso());
        assertNull(resposta.atributos().getFirst().valor());
        assertNull(resposta.atributos().get(1));
        assertEquals("22222222222", resposta.assinaturasDigitais().getFirst().cpf());
        assertEquals("ORIENTACAO SIMULADA", resposta.conformidade().getFirst()
                .checklist().apontamentos().getFirst().orientacao());
        assertEquals("valor-propriedade", resposta.propriedades().getFirst().valor());
        assertEquals("DOC001", resposta.outsourcing().getFirst()
                .resultadoCodigoRejeicao());
        assertFalse(resposta.outsourcing().getFirst().resultadoValidacaoNegocial()
                .apontamentos().getFirst().aprovado());
        assertNull(resposta.armazenamento().getFirst().pathStorage());
    }

    @Test
    void omiteSomenteProjecoesENosParciaisPreservandoCamposNulosDoContrato() {
        var documento = new DocumentoDossieProdutoConsultado(
                1L,
                2L,
                null,
                DATA_REFERENCIA,
                null,
                "SIM0001",
                null,
                "Criado",
                new DocumentoDossieProdutoConsultado.VinculoDossie(
                        new DocumentoDossieProdutoConsultado.Cliente(
                                "00000000000", null, "CLIENTE SIMULADO", null,
                                "Vendedor PF", 3L, false),
                        null, null, null, null),
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var resposta = ConsultaDocumentosDossieProdutoRestMapper.paraResposta(
                List.of(documento)).getFirst();
        var json = OBJECT_MAPPER.valueToTree(resposta);

        assertTrue(json.path("codigo_ged").isNull());
        assertTrue(json.path("data_hora_validade").isNull());
        assertFalse(json.has("url"));
        assertFalse(json.has("atributos"));
        assertFalse(json.has("assinaturas_digitais"));
        assertFalse(json.has("conformidade"));
        assertFalse(json.has("propriedades"));
        assertFalse(json.has("outsourcing"));
        assertFalse(json.has("armazenamento"));
        assertFalse(json.path("vinculo_dossie").has("produto"));
        assertFalse(json.path("vinculo_dossie").path("cliente").has("cnpj"));
        assertFalse(json.path("vinculo_dossie").path("cliente").has("razao_social"));
    }

    @Test
    void declaraNomesJsonExplicitosEmTodoContratoPublico() {
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.class,
                "id_instancia_documento", "id_documento", "codigo_ged",
                "data_hora_captura", "data_hora_validade", "matricula_captura",
                "tipo_documento", "situacao_documento", "vinculo_dossie", "url",
                "atributos", "assinaturas_digitais", "conformidade", "propriedades",
                "outsourcing", "armazenamento");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.TipoDocumento.class,
                "id", "nome", "codigo_tipologia", "ativo");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.VinculoDossie.class,
                "cliente", "produto", "garantia", "fase", "processo");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Cliente.class,
                "cpf", "cnpj", "nome", "razao_social", "tipo_vinculo",
                "identificador_negocial_vinculo", "principal");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Produto.class,
                "id", "nome", "modalidade", "operacao");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Garantia.class,
                "id", "nome", "codigo_bacen", "produto", "clientes_avalistas");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.ClienteAvalista.class,
                "cpf", "cnpj");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Fase.class,
                "id", "nome", "identificador_negocial");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Processo.class,
                "id", "nome", "identificador_negocial", "macroprocesso");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Atributo.class,
                "chave", "valor", "opcoes_selecionadas");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.AssinaturaDigital.class,
                "data_assinatura", "emissor", "cpf", "cnpj", "nome",
                "cpf_responsavel_pj", "nome_responsavel_pj");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Conformidade.class,
                "id", "tipo_conformidade", "unidade_conformidade",
                "data_hora_verificacao", "dossie_produto", "fornecedor", "resultado",
                "checklist");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Checklist.class,
                "id", "identificador_negocial", "nome", "versao", "apontamentos");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Apontamento.class,
                "id", "identificador_negocial", "nome", "tipo", "complexidade",
                "aprovado", "orientacao", "comentario");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Propriedade.class,
                "chave", "valor");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Outsourcing.class,
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
                ConsultaDocumentosDossieProdutoResponse.ResultadoValidacaoNegocial.class,
                "identificador_checklist", "versao_checklist", "apontamentos");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse
                        .ResultadoValidacaoNegocialApontamento.class,
                "identificador_apontamento", "aprovado", "comentario");
        assertJsonProperties(ConsultaDocumentosDossieProdutoResponse.Armazenamento.class,
                "id", "data_hora_armazenamento", "tipo_armazenamento", "path_storage",
                "object_store_ged", "codigo_ged", "data_hora_previsao_exclusao",
                "data_hora_exclusao");
    }

    @Test
    void traduzTodasAsFalhasInternasSemPerderPayload() {
        var negocio = assertInstanceOf(MtrBusinessErrorException.class,
                ConsultaDocumentosDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO, 404)));
        assertEquals(404, negocio.status());
        assertEquals(404, negocio.erro().codigoHttp());
        assertEquals("/dossies/4081899/documentos", negocio.erro().recurso());
        assertEquals("erro-sintetico", negocio.erro().idErro());
        assertEquals("MTRPRD0002", negocio.erro().codigoErro());
        assertEquals("mensagem sintetica", negocio.erro().erros().getFirst().mensagem());
        assertEquals("detalhe sintetico", negocio.erro().detalhe());
        assertEquals("stacktrace externo sintetico", negocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(MtrClientTechnicalException.class,
                ConsultaDocumentosDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDocumentosDossieProduto.Tipo.TECNICA_CLIENTE,
                        422))).status());
        assertEquals(503, assertInstanceOf(MtrServerErrorException.class,
                ConsultaDocumentosDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                        503))).status());

        var timeout = assertInstanceOf(MtrServerErrorException.class,
                ConsultaDocumentosDossieProdutoRestMapper.paraExcecaoRest(falha(
                        FalhaConsultaDocumentosDossieProduto.Tipo.TIMEOUT, null)));
        assertEquals(500, timeout.status());
        assertNull(timeout.erro().codigoHttp());
    }

    @Test
    void preservaListaEElementosNulosNaFalhaPublica() {
        var semMensagens = falha(
                FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO, 404, null);
        var comElementoNulo = falha(
                FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO,
                404,
                Arrays.asList(null, "mensagem sintetica"));

        var erroSemMensagens = assertInstanceOf(MtrBusinessErrorException.class,
                ConsultaDocumentosDossieProdutoRestMapper.paraExcecaoRest(semMensagens));
        var erroComElementoNulo = assertInstanceOf(MtrBusinessErrorException.class,
                ConsultaDocumentosDossieProdutoRestMapper.paraExcecaoRest(
                        comElementoNulo));

        assertNull(erroSemMensagens.erro().erros());
        assertNull(erroComElementoNulo.erro().erros().getFirst());
        assertEquals("mensagem sintetica",
                erroComElementoNulo.erro().erros().get(1).mensagem());
    }

    private static DocumentoDossieProdutoConsultado documentoCompleto() {
        var produto = new DocumentoDossieProdutoConsultado.Produto(
                10, "PRODUTO SIMULADO", 20, 30);
        var apontamento = new DocumentoDossieProdutoConsultado.Apontamento(
                100L, 101L, "APONTAMENTO SIMULADO", "DOCUMENTAL", "BAIXA",
                true, "ORIENTACAO SIMULADA", null);
        var checklist = new DocumentoDossieProdutoConsultado.Checklist(
                102L, 103L, "CHECKLIST SIMULADO", "1", List.of(apontamento));
        var resultadoApontamento =
                new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento(
                        106L, false, "COMENTARIO SIMULADO");
        var resultadoValidacao =
                new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial(
                        107L, 2, List.of(resultadoApontamento));

        return new DocumentoDossieProdutoConsultado(
                9000001L,
                9000002L,
                "GED-SIMULADO-0001",
                DATA_REFERENCIA,
                null,
                "SIM0001",
                new DocumentoDossieProdutoConsultado.TipoDocumento(
                        9001L, "DOCUMENTO SIMULADO", "SIM-0001", false),
                "Criado",
                new DocumentoDossieProdutoConsultado.VinculoDossie(
                        new DocumentoDossieProdutoConsultado.Cliente(
                                "00000000000", null, "CLIENTE SIMULADO", null,
                                "Vendedor PF", 9000003L, false),
                        produto,
                        new DocumentoDossieProdutoConsultado.Garantia(
                                40L,
                                "GARANTIA SIMULADA",
                                50,
                                produto,
                                Arrays.asList(
                                        new DocumentoDossieProdutoConsultado.ClienteAvalista(
                                                "11111111111", null),
                                        null)),
                        new DocumentoDossieProdutoConsultado.Fase(
                                60, "FASE SIMULADA", 70L),
                        new DocumentoDossieProdutoConsultado.Processo(
                                80, "PROCESSO SIMULADO", 90L,
                                "MACROPROCESSO SIMULADO")),
                null,
                Arrays.asList(
                        new DocumentoDossieProdutoConsultado.Atributo(
                                "chave-atributo", null, List.of("opcao-1", "opcao-2")),
                        null),
                List.of(new DocumentoDossieProdutoConsultado.AssinaturaDigital(
                        DATA_REFERENCIA, "EMISSOR SIMULADO", "22222222222", null,
                        "ASSINANTE SIMULADO", null, null)),
                List.of(new DocumentoDossieProdutoConsultado.Conformidade(
                        104L, "INTERNA", 105L, DATA_REFERENCIA, 4081899L,
                        "FORNECEDOR SIMULADO", "CONFORME", checklist)),
                List.of(new DocumentoDossieProdutoConsultado.Propriedade(
                        "chave-propriedade", "valor-propriedade")),
                List.of(novoOutsourcing(resultadoValidacao)),
                List.of(new DocumentoDossieProdutoConsultado.Armazenamento(
                        9000004L,
                        "01/01/2030 10:00:01",
                        "GED_RECEBIDO",
                        null,
                        "OBJECT_STORE_SIMULADO",
                        "GED-SIMULADO-0001",
                        null,
                        null)));
    }

    private static DocumentoDossieProdutoConsultado.Outsourcing novoOutsourcing(
            DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial resultado
    ) {
        return new DocumentoDossieProdutoConsultado.Outsourcing(
                "SOLICITACAO SIMULADA",
                "PROCESSO SIMULADO",
                true,
                108L,
                "FORNECEDOR-01",
                "FORNECEDOR",
                "CANAL",
                "CANAL-01",
                false,
                DATA_REFERENCIA,
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
                "01/01/2030 10:00:01",
                "01/01/2030 10:00:02",
                "01/01/2030 10:00:03",
                "01/01/2030 10:00:04",
                "01/01/2030 10:00:05",
                "01/01/2030 10:00:06",
                "01/01/2030 10:00:07",
                "01/01/2030 10:00:08",
                "01/01/2030 10:00:09",
                0.75,
                0.85,
                "DOC001",
                "REJEICAO SIMULADA",
                "CONSULTA SIMULADA",
                "CLASSIFICACAO SIMULADA",
                "EXTRACAO SIMULADA",
                resultado);
    }

    private static FalhaConsultaDocumentosDossieProduto falha(
            FalhaConsultaDocumentosDossieProduto.Tipo tipo,
            Integer status
    ) {
        return falha(tipo, status, List.of("mensagem sintetica"));
    }

    private static FalhaConsultaDocumentosDossieProduto falha(
            FalhaConsultaDocumentosDossieProduto.Tipo tipo,
            Integer status,
            List<String> mensagens
    ) {
        return new FalhaConsultaDocumentosDossieProduto(
                tipo,
                status,
                "/dossies/4081899/documentos",
                "erro-sintetico",
                "MTRPRD0002",
                mensagens,
                "detalhe sintetico",
                "stacktrace externo sintetico",
                new IllegalStateException("causa sintetica"));
    }

    private static void assertJsonProperties(Class<?> tipo, String... nomesEsperados) {
        var nomesDeclarados = Arrays.stream(tipo.getRecordComponents())
                .map(componente -> componente.getAccessor().getAnnotation(JsonProperty.class))
                .map(anotacao -> anotacao == null ? null : anotacao.value())
                .toList();

        assertEquals(List.of(nomesEsperados), nomesDeclarados);
    }
}
