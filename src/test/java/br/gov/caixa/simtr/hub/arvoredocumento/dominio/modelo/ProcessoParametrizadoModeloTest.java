package br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProcessoParametrizadoModeloTest {

    private static final String CODIGO_TIPO_DOCUMENTO = "TIP-01";
    private static final String NOME_TIPO_DOCUMENTO = "Contrato";
    private static final String NOME_FUNCAO_DOCUMENTAL = "Formalizacao";
    private static final String NOME_MACROPROCESSO = "Credito";
    private static final String DATA_ULTIMA_ALTERACAO_MACROPROCESSO = "2026-07-14T10:15:00";

    @Test
    void devePreservarIdentificadoresReferenciasOpcoesETipoDocumental() {
        var identificador = new IdentificadorNegocialProcesso(101L);
        var checklist = new ReferenciaChecklistProcessoParametrizado(202L, 3);
        var opcao = new OpcaoDisponivelProcessoParametrizado("S", "Sim", true);
        var tipoDocumento = new TipoDocumentoProcessoParametrizado(
                CODIGO_TIPO_DOCUMENTO, NOME_TIPO_DOCUMENTO, true, false, true, null
        );

        assertEquals(101L, identificador.valor());
        assertEquals(202L, checklist.identificadorChecklist());
        assertEquals(3, checklist.versaoChecklist());
        assertEquals("S", opcao.valorOpcao());
        assertEquals("Sim", opcao.descricaoOpcao());
        assertEquals(true, opcao.ativo());
        assertEquals(CODIGO_TIPO_DOCUMENTO, tipoDocumento.codigoTipologia());
        assertEquals(NOME_TIPO_DOCUMENTO, tipoDocumento.nome());
        assertEquals(true, tipoDocumento.permiteReuso());
        assertEquals(false, tipoDocumento.permiteMultiplo());
        assertEquals(true, tipoDocumento.ativo());
        assertNull(tipoDocumento.checklist());
    }

    @Test
    void devePreservarFuncaoDocumentoECampoFormularioSemAlterarListas() {
        var checklist = new ReferenciaChecklistProcessoParametrizado(202L, 3);
        var tipoDocumento = new TipoDocumentoProcessoParametrizado(
                CODIGO_TIPO_DOCUMENTO, NOME_TIPO_DOCUMENTO, true, false, true, checklist
        );
        var tiposDocumento = List.of(tipoDocumento);
        var funcaoDocumental = new FuncaoDocumentalProcessoParametrizado(
                NOME_FUNCAO_DOCUMENTAL, tiposDocumento, checklist
        );
        var documento = new DocumentoProcessoParametrizado(
                funcaoDocumental, tipoDocumento, true
        );
        var opcoes = List.of(new OpcaoDisponivelProcessoParametrizado("S", "Sim", true));
        var campo = new CampoFormularioProcessoParametrizado(
                303L,
                "Autoriza?",
                true,
                true,
                "produto.ativo",
                6,
                4,
                "SELECAO",
                null,
                "Selecione",
                1,
                10,
                "Escolha uma opcao",
                false,
                opcoes
        );

        assertEquals(NOME_FUNCAO_DOCUMENTAL, funcaoDocumental.nome());
        assertSame(tiposDocumento, funcaoDocumental.tiposDocumento());
        assertSame(checklist, funcaoDocumental.checklist());
        assertSame(funcaoDocumental, documento.funcaoDocumental());
        assertSame(tipoDocumento, documento.tipoDocumento());
        assertEquals(true, documento.obrigatorio());
        assertEquals(303L, campo.identificadorNegocial());
        assertEquals("Autoriza?", campo.label());
        assertEquals(true, campo.obrigatorio());
        assertEquals(true, campo.ativo());
        assertEquals("produto.ativo", campo.exibicaoCondicional());
        assertEquals(6, campo.tamanhoApresentacao());
        assertEquals(4, campo.ordemApresentacao());
        assertEquals("SELECAO", campo.tipo());
        assertNull(campo.mascara());
        assertEquals("Selecione", campo.placeholder());
        assertEquals(1, campo.tamanhoMinimo());
        assertEquals(10, campo.tamanhoMaximo());
        assertEquals("Escolha uma opcao", campo.orientacaoPreenchimento());
        assertEquals(false, campo.bloquearEdicao());
        assertSame(opcoes, campo.opcoesDisponiveis());
    }

    @Test
    void devePreservarMacroprocessoGarantiaEProdutoSemAlterarListas() {
        var checklist = new ReferenciaChecklistProcessoParametrizado(202L, 3);
        var campos = List.<CampoFormularioProcessoParametrizado>of();
        var documentos = List.<DocumentoProcessoParametrizado>of();
        var macroprocesso = new MacroprocessoParametrizado(
                401L, NOME_MACROPROCESSO, true, DATA_ULTIMA_ALTERACAO_MACROPROCESSO
        );
        var garantia = new GarantiaProcessoParametrizado(
                501L, "Aval", true, campos, documentos, checklist
        );
        var garantias = List.of(garantia);
        var produto = new ProdutoProcessoParametrizado(
                601L, 602L, "Capital de giro", campos, documentos, garantias, checklist
        );

        assertEquals(401L, macroprocesso.identificadorNegocial());
        assertEquals(NOME_MACROPROCESSO, macroprocesso.nome());
        assertEquals(true, macroprocesso.ativo());
        assertEquals(DATA_ULTIMA_ALTERACAO_MACROPROCESSO, macroprocesso.ultimaAlteracao());
        assertEquals(501L, garantia.codigoBacen());
        assertEquals("Aval", garantia.nomeGarantia());
        assertEquals(true, garantia.fidejussoria());
        assertSame(campos, garantia.camposFormulario());
        assertSame(documentos, garantia.documentos());
        assertSame(checklist, garantia.checklist());
        assertEquals(601L, produto.codigoOperacao());
        assertEquals(602L, produto.codigoModalidade());
        assertEquals("Capital de giro", produto.nome());
        assertSame(campos, produto.camposFormulario());
        assertSame(documentos, produto.documentos());
        assertSame(garantias, produto.garantias());
        assertSame(checklist, produto.checklist());
    }

    @Test
    void devePreservarRelacionamentoFaseERaizDoProcessoSemAlterarListas() {
        var checklist = new ReferenciaChecklistProcessoParametrizado(202L, 3);
        var macroprocesso = new MacroprocessoParametrizado(
                401L, NOME_MACROPROCESSO, true, DATA_ULTIMA_ALTERACAO_MACROPROCESSO
        );
        var campos = List.<CampoFormularioProcessoParametrizado>of();
        var documentos = List.<DocumentoProcessoParametrizado>of();
        var garantias = List.<GarantiaProcessoParametrizado>of();
        var produtos = List.<ProdutoProcessoParametrizado>of();
        var checklists = List.of(checklist);
        var relacionamento = new RelacionamentoProcessoParametrizado(
                701L, "Proponente", "PF", true, true, false, false, campos, documentos
        );
        var fase = new FaseProcessoParametrizado(
                801L,
                NOME_FUNCAO_DOCUMENTAL,
                true,
                "2026-07-14T10:20:00",
                2,
                "Envie os documentos",
                produtos,
                garantias,
                campos,
                documentos,
                checklists
        );
        var relacionamentos = List.of(relacionamento);
        var fases = List.of(fase);
        var processo = new ProcessoParametrizado(
                901L,
                "Contratacao",
                true,
                "2026-07-14T10:30:00",
                true,
                macroprocesso,
                relacionamentos,
                produtos,
                fases,
                documentos,
                checklist
        );

        assertEquals(701L, relacionamento.identificadorNegocial());
        assertEquals("Proponente", relacionamento.nome());
        assertEquals("PF", relacionamento.tipoPessoa());
        assertEquals(true, relacionamento.principal());
        assertEquals(true, relacionamento.obrigatorio());
        assertEquals(false, relacionamento.relacionado());
        assertEquals(false, relacionamento.sequencia());
        assertSame(campos, relacionamento.camposFormulario());
        assertSame(documentos, relacionamento.documentos());
        assertEquals(801L, fase.identificadorNegocial());
        assertEquals(NOME_FUNCAO_DOCUMENTAL, fase.nome());
        assertEquals(true, fase.ativo());
        assertEquals("2026-07-14T10:20:00", fase.ultimaAlteracao());
        assertEquals(2, fase.ordem());
        assertEquals("Envie os documentos", fase.orientacaoUsuario());
        assertSame(produtos, fase.produtos());
        assertSame(garantias, fase.garantias());
        assertSame(campos, fase.camposFormulario());
        assertSame(documentos, fase.documentos());
        assertSame(checklists, fase.checklist());
        assertEquals(901L, processo.identificadorNegocial());
        assertEquals("Contratacao", processo.nome());
        assertEquals(true, processo.ativo());
        assertEquals("2026-07-14T10:30:00", processo.ultimaAlteracao());
        assertEquals(true, processo.indicadorProdutoObrigatorio());
        assertSame(macroprocesso, processo.macroprocesso());
        assertSame(relacionamentos, processo.relacionamentos());
        assertSame(produtos, processo.produtos());
        assertSame(fases, processo.fases());
        assertSame(documentos, processo.documentos());
        assertSame(checklist, processo.checklist());
    }
}
