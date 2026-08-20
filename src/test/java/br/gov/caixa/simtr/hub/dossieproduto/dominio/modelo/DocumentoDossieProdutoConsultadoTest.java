package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DocumentoDossieProdutoConsultadoTest {

    private static final String DATA_REFERENCIA = "01/01/2030 10:00:00";

    @Test
    void preservaSchemaCompletoComDatasOpacasNulosEOrdemDasListas() {
        var tipoDocumento = new DocumentoDossieProdutoConsultado.TipoDocumento(
                9001L, "DOCUMENTO SIMULADO", "SIM-0001", false);
        var cliente = new DocumentoDossieProdutoConsultado.Cliente(
                "00000000000", null, "CLIENTE SIMULADO", null,
                "Vendedor PF", 9000003L, false);
        var produto = new DocumentoDossieProdutoConsultado.Produto(
                10, "PRODUTO SIMULADO", 20, 30);
        var avalista = new DocumentoDossieProdutoConsultado.ClienteAvalista(
                "11111111111", null);
        List<DocumentoDossieProdutoConsultado.ClienteAvalista> avalistas =
                Arrays.asList(avalista, null);
        var garantia = new DocumentoDossieProdutoConsultado.Garantia(
                40L, "GARANTIA SIMULADA", 50, produto, avalistas);
        var fase = new DocumentoDossieProdutoConsultado.Fase(
                60, "FASE SIMULADA", 70L);
        var processo = new DocumentoDossieProdutoConsultado.Processo(
                80, "PROCESSO SIMULADO", 90L, "MACROPROCESSO SIMULADO");
        var vinculo = new DocumentoDossieProdutoConsultado.VinculoDossie(
                cliente, produto, garantia, fase, processo);

        var atributo = new DocumentoDossieProdutoConsultado.Atributo(
                "chave-atributo", null, List.of("opcao-1", "opcao-2"));
        var assinatura = new DocumentoDossieProdutoConsultado.AssinaturaDigital(
                DATA_REFERENCIA, "EMISSOR SIMULADO", "22222222222", null,
                "ASSINANTE SIMULADO", null, null);
        var apontamento = new DocumentoDossieProdutoConsultado.Apontamento(
                100L, 101L, "APONTAMENTO SIMULADO", "DOCUMENTAL", "BAIXA",
                true, "ORIENTACAO SIMULADA", null);
        var checklist = new DocumentoDossieProdutoConsultado.Checklist(
                102L, 103L, "CHECKLIST SIMULADO", "1", List.of(apontamento));
        var conformidade = new DocumentoDossieProdutoConsultado.Conformidade(
                104L, "INTERNA", 105L, DATA_REFERENCIA, 4081899L,
                "FORNECEDOR SIMULADO", "CONFORME", checklist);
        var propriedade = new DocumentoDossieProdutoConsultado.Propriedade(
                "chave-propriedade", "valor-propriedade");
        var resultadoApontamento =
                new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento(
                        106L, false, "COMENTARIO SIMULADO");
        var resultadoValidacao =
                new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial(
                        107L, 2, List.of(resultadoApontamento));
        var outsourcing = novoOutsourcing(resultadoValidacao);
        var armazenamento = new DocumentoDossieProdutoConsultado.Armazenamento(
                9000004L,
                "01/01/2030 10:00:01",
                "GED_RECEBIDO",
                null,
                "OBJECT_STORE_SIMULADO",
                "GED-SIMULADO-0001",
                null,
                null);

        List<DocumentoDossieProdutoConsultado.Atributo> atributos =
                Arrays.asList(atributo, null);
        List<DocumentoDossieProdutoConsultado.AssinaturaDigital> assinaturas =
                List.of(assinatura);
        List<DocumentoDossieProdutoConsultado.Conformidade> conformidades =
                List.of(conformidade);
        List<DocumentoDossieProdutoConsultado.Propriedade> propriedades =
                List.of(propriedade);
        List<DocumentoDossieProdutoConsultado.Outsourcing> outsourcings =
                List.of(outsourcing);
        List<DocumentoDossieProdutoConsultado.Armazenamento> armazenamentos =
                List.of(armazenamento);

        var documento = new DocumentoDossieProdutoConsultado(
                9000001L,
                9000002L,
                "GED-SIMULADO-0001",
                DATA_REFERENCIA,
                null,
                "SIM0001",
                tipoDocumento,
                "Criado",
                vinculo,
                null,
                atributos,
                assinaturas,
                conformidades,
                propriedades,
                outsourcings,
                armazenamentos);

        assertCabecalho(documento, tipoDocumento, vinculo);
        assertListas(documento, atributos, assinaturas, conformidades,
                propriedades, outsourcings, armazenamentos);
        assertVinculo(vinculo, cliente, produto, garantia, fase, processo, avalistas);
        assertColecoes(atributo, assinatura, conformidade, apontamento, checklist,
                propriedade, resultadoApontamento, armazenamento);
        assertOutsourcing(outsourcing, resultadoValidacao, resultadoApontamento);
    }

    private static DocumentoDossieProdutoConsultado.Outsourcing novoOutsourcing(
            DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial resultadoValidacao
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
                resultadoValidacao);
    }

    private static void assertCabecalho(
            DocumentoDossieProdutoConsultado documento,
            DocumentoDossieProdutoConsultado.TipoDocumento tipoDocumento,
            DocumentoDossieProdutoConsultado.VinculoDossie vinculo
    ) {
        assertEquals(9000001L, documento.idInstanciaDocumento());
        assertEquals(9000002L, documento.idDocumento());
        assertEquals("GED-SIMULADO-0001", documento.codigoGed());
        assertEquals(DATA_REFERENCIA, documento.dataHoraCaptura());
        assertNull(documento.dataHoraValidade());
        assertEquals("SIM0001", documento.matriculaCaptura());
        assertSame(tipoDocumento, documento.tipoDocumento());
        assertEquals(9001L, tipoDocumento.id());
        assertEquals("DOCUMENTO SIMULADO", tipoDocumento.nome());
        assertEquals("SIM-0001", tipoDocumento.codigoTipologia());
        assertFalse(tipoDocumento.ativo());
        assertEquals("Criado", documento.situacaoDocumento());
        assertSame(vinculo, documento.vinculoDossie());
        assertNull(documento.url());
    }

    private static void assertListas(
            DocumentoDossieProdutoConsultado documento,
            List<DocumentoDossieProdutoConsultado.Atributo> atributos,
            List<DocumentoDossieProdutoConsultado.AssinaturaDigital> assinaturas,
            List<DocumentoDossieProdutoConsultado.Conformidade> conformidades,
            List<DocumentoDossieProdutoConsultado.Propriedade> propriedades,
            List<DocumentoDossieProdutoConsultado.Outsourcing> outsourcings,
            List<DocumentoDossieProdutoConsultado.Armazenamento> armazenamentos
    ) {
        assertSame(atributos, documento.atributos());
        assertNull(documento.atributos().get(1));
        assertSame(assinaturas, documento.assinaturasDigitais());
        assertSame(conformidades, documento.conformidades());
        assertSame(propriedades, documento.propriedades());
        assertSame(outsourcings, documento.outsourcings());
        assertSame(armazenamentos, documento.armazenamentos());
    }

    private static void assertVinculo(
            DocumentoDossieProdutoConsultado.VinculoDossie vinculo,
            DocumentoDossieProdutoConsultado.Cliente cliente,
            DocumentoDossieProdutoConsultado.Produto produto,
            DocumentoDossieProdutoConsultado.Garantia garantia,
            DocumentoDossieProdutoConsultado.Fase fase,
            DocumentoDossieProdutoConsultado.Processo processo,
            List<DocumentoDossieProdutoConsultado.ClienteAvalista> avalistas
    ) {
        assertSame(cliente, vinculo.cliente());
        assertEquals("00000000000", cliente.cpf());
        assertNull(cliente.cnpj());
        assertEquals("CLIENTE SIMULADO", cliente.nome());
        assertNull(cliente.razaoSocial());
        assertEquals("Vendedor PF", cliente.tipoVinculo());
        assertEquals(9000003L, cliente.identificadorNegocialVinculo());
        assertFalse(cliente.principal());

        assertSame(produto, vinculo.produto());
        assertEquals(10, produto.id());
        assertEquals("PRODUTO SIMULADO", produto.nome());
        assertEquals(20, produto.modalidade());
        assertEquals(30, produto.operacao());

        assertSame(garantia, vinculo.garantia());
        assertEquals(40L, garantia.id());
        assertEquals("GARANTIA SIMULADA", garantia.nome());
        assertEquals(50, garantia.codigoBacen());
        assertSame(produto, garantia.produto());
        assertSame(avalistas, garantia.clientesAvalistas());
        assertEquals("11111111111", garantia.clientesAvalistas().getFirst().cpf());
        assertNull(garantia.clientesAvalistas().getFirst().cnpj());
        assertNull(garantia.clientesAvalistas().get(1));

        assertSame(fase, vinculo.fase());
        assertEquals(60, fase.id());
        assertEquals("FASE SIMULADA", fase.nome());
        assertEquals(70L, fase.identificadorNegocial());

        assertSame(processo, vinculo.processo());
        assertEquals(80, processo.id());
        assertEquals("PROCESSO SIMULADO", processo.nome());
        assertEquals(90L, processo.identificadorNegocial());
        assertEquals("MACROPROCESSO SIMULADO", processo.macroprocesso());
    }

    @SuppressWarnings("java:S107") // A assercao percorre o schema aprovado sem ocultar subtipos.
    private static void assertColecoes(
            DocumentoDossieProdutoConsultado.Atributo atributo,
            DocumentoDossieProdutoConsultado.AssinaturaDigital assinatura,
            DocumentoDossieProdutoConsultado.Conformidade conformidade,
            DocumentoDossieProdutoConsultado.Apontamento apontamento,
            DocumentoDossieProdutoConsultado.Checklist checklist,
            DocumentoDossieProdutoConsultado.Propriedade propriedade,
            DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento
                    resultadoApontamento,
            DocumentoDossieProdutoConsultado.Armazenamento armazenamento
    ) {
        assertEquals("chave-atributo", atributo.chave());
        assertNull(atributo.valor());
        assertEquals(List.of("opcao-1", "opcao-2"), atributo.opcoesSelecionadas());

        assertEquals(DATA_REFERENCIA, assinatura.dataAssinatura());
        assertEquals("EMISSOR SIMULADO", assinatura.emissor());
        assertEquals("22222222222", assinatura.cpf());
        assertNull(assinatura.cnpj());
        assertEquals("ASSINANTE SIMULADO", assinatura.nome());
        assertNull(assinatura.cpfResponsavelPj());
        assertNull(assinatura.nomeResponsavelPj());

        assertEquals(104L, conformidade.id());
        assertEquals("INTERNA", conformidade.tipoConformidade());
        assertEquals(105L, conformidade.unidadeConformidade());
        assertEquals(DATA_REFERENCIA, conformidade.dataHoraVerificacao());
        assertEquals(4081899L, conformidade.dossieProduto());
        assertEquals("FORNECEDOR SIMULADO", conformidade.fornecedor());
        assertEquals("CONFORME", conformidade.resultado());
        assertSame(checklist, conformidade.checklist());
        assertEquals(102L, checklist.id());
        assertEquals(103L, checklist.identificadorNegocial());
        assertEquals("CHECKLIST SIMULADO", checklist.nome());
        assertEquals("1", checklist.versao());
        assertEquals(List.of(apontamento), checklist.apontamentos());
        assertEquals(100L, apontamento.id());
        assertEquals(101L, apontamento.identificadorNegocial());
        assertEquals("APONTAMENTO SIMULADO", apontamento.nome());
        assertEquals("DOCUMENTAL", apontamento.tipo());
        assertEquals("BAIXA", apontamento.complexidade());
        assertTrue(apontamento.aprovado());
        assertEquals("ORIENTACAO SIMULADA", apontamento.orientacao());
        assertNull(apontamento.comentario());

        assertEquals("chave-propriedade", propriedade.chave());
        assertEquals("valor-propriedade", propriedade.valor());
        assertEquals(106L, resultadoApontamento.identificadorApontamento());
        assertFalse(resultadoApontamento.aprovado());
        assertEquals("COMENTARIO SIMULADO", resultadoApontamento.comentario());

        assertEquals(9000004L, armazenamento.id());
        assertEquals("01/01/2030 10:00:01", armazenamento.dataHoraArmazenamento());
        assertEquals("GED_RECEBIDO", armazenamento.tipoArmazenamento());
        assertNull(armazenamento.pathStorage());
        assertEquals("OBJECT_STORE_SIMULADO", armazenamento.objectStoreGed());
        assertEquals("GED-SIMULADO-0001", armazenamento.codigoGed());
        assertNull(armazenamento.dataHoraPrevisaoExclusao());
        assertNull(armazenamento.dataHoraExclusao());
    }

    private static void assertOutsourcing(
            DocumentoDossieProdutoConsultado.Outsourcing outsourcing,
            DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial resultadoValidacao,
            DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento apontamento
    ) {
        assertEquals("SOLICITACAO SIMULADA", outsourcing.solicitacao());
        assertEquals("PROCESSO SIMULADO", outsourcing.processo());
        assertTrue(outsourcing.analiseConjunta());
        assertEquals(108L, outsourcing.codigoControle());
        assertEquals("FORNECEDOR-01", outsourcing.codigoFornecedor());
        assertEquals("FORNECEDOR", outsourcing.siglaFornecedor());
        assertEquals("CANAL", outsourcing.siglaCanal());
        assertEquals("CANAL-01", outsourcing.codigoCanal());
        assertFalse(outsourcing.retornoAgrupado());
        assertEquals(DATA_REFERENCIA, outsourcing.dataHoraEnvio());
        assertEquals("CONCLUIDO", outsourcing.statusEnvio());
        assertTrue(outsourcing.solicitacaoTratamentoImagem());
        assertFalse(outsourcing.solicitacaoExtracao());
        assertTrue(outsourcing.solicitacaoValidacaoNegocial());
        assertFalse(outsourcing.solicitacaoClassificacao());
        assertTrue(outsourcing.solicitacaoAvaliacaoCadastral());
        assertFalse(outsourcing.solicitacaoAvaliacaoAutenticidade());
        assertTrue(outsourcing.solicitacaoGrafoscopia());
        assertFalse(outsourcing.solicitacaoValidacaoExterna());
        assertTrue(outsourcing.solicitacaoConsultaExterna());
        assertEquals("M0", outsourcing.janelaExtracao());
        assertEquals("01/01/2030 10:00:01", outsourcing.dataHoraRetornoClassificacao());
        assertEquals("01/01/2030 10:00:02", outsourcing.dataHoraRetornoExtracao());
        assertEquals("01/01/2030 10:00:03", outsourcing.dataHoraRetornoValidacaoNegocial());
        assertEquals("01/01/2030 10:00:04", outsourcing.dataHoraRetornoGrafoscopia());
        assertEquals("01/01/2030 10:00:05",
                outsourcing.dataHoraRetornoAvaliacaoAutenticidade());
        assertEquals("01/01/2030 10:00:06", outsourcing.dataHoraRetornoImagemTratada());
        assertEquals("01/01/2030 10:00:07", outsourcing.dataHoraValidacaoExterna());
        assertEquals("01/01/2030 10:00:08", outsourcing.dataConsultaExterna());
        assertEquals("01/01/2030 10:00:09", outsourcing.dataAvaliacaoCadastral());
        assertEquals(0.75, outsourcing.resultadoIndiceAvaliacaoAutenticidade());
        assertEquals(0.85, outsourcing.resultadoIndiceGrafoscopia());
        assertEquals("DOC001", outsourcing.resultadoCodigoRejeicao());
        assertEquals("REJEICAO SIMULADA", outsourcing.resultadoDescricaoRejeicao());
        assertEquals("CONSULTA SIMULADA", outsourcing.resultadoConsultaExterna());
        assertEquals("CLASSIFICACAO SIMULADA", outsourcing.resultadoClassificacao());
        assertEquals("EXTRACAO SIMULADA", outsourcing.resultadoExtracao());
        assertSame(resultadoValidacao, outsourcing.resultadoValidacaoNegocial());
        assertEquals(107L, resultadoValidacao.identificadorChecklist());
        assertEquals(2, resultadoValidacao.versaoChecklist());
        assertEquals(List.of(apontamento), resultadoValidacao.apontamentos());
    }
}
