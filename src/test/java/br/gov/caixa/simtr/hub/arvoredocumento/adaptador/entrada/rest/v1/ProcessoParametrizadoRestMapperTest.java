package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.erro.FalhaConsultaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.CampoFormularioProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.DocumentoProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.FaseProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.FuncaoDocumentalProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.GarantiaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.MacroprocessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.OpcaoDisponivelProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProdutoProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ReferenciaChecklistProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.RelacionamentoProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.TipoDocumentoProcessoParametrizado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessoParametrizadoRestMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void mapeiaArvoreCompletaPreservandoNulabilidadeEContratoJson() {
        ProcessoDto resposta = ProcessoParametrizadoRestMapper.paraResposta(processo());

        assertEquals(100L, resposta.identificadorNegocial());
        assertEquals(10L, resposta.macroprocesso().identificadorNegocial());
        assertEquals(20L, resposta.relacionamentos().getFirst().identificadorNegocial());
        assertNull(resposta.relacionamentos().get(1));
        assertNull(resposta.produtos());
        assertEquals(30L, resposta.fases().getFirst().identificadorNegocial());
        assertEquals(40L, resposta.fases().getFirst().produtos().getFirst().codigoOperacao());
        assertEquals(50L, resposta.fases().getFirst().garantias().getFirst().codigoBacen());
        assertEquals(60L, resposta.fases().getFirst().camposFormulario().getFirst()
                .identificadorNegocial());
        assertEquals("A", resposta.fases().getFirst().camposFormulario().getFirst()
                .opcoesDisponiveis().getFirst().valorOpcao());
        assertEquals("FUNCAO", resposta.documentos().getFirst().funcaoDocumental().nome());
        assertEquals("TIPO", resposta.documentos().getFirst().tipoDocumento().codigoTipologia());
        assertEquals(70L, resposta.checklist().identificadorChecklist());

        JsonNode json = OBJECT_MAPPER.valueToTree(resposta);
        assertEquals(100L, json.path("identificador_negocial").longValue());
        assertEquals(false, json.path("indicador_produto_obrigatorio").booleanValue());
        assertFalse(json.has("ultima_alteracao"));
        assertFalse(json.has("produtos"));
        assertEquals(true, json.path("relacionamentos").get(1).isNull());
        assertEquals("orientacao", json.path("fases").get(0)
                .path("orientacao_usuario").textValue());
        assertEquals("A", json.path("fases").get(0).path("campos_formulario").get(0)
                .path("opcoes_disponiveis").get(0).path("valor_opcao").textValue());
        assertEquals("TIPO", json.path("documentos").get(0).path("tipo_documento")
                .path("codigo_tipologia").textValue());
    }

    @Test
    void aceitaRespostaNula() {
        assertNull(ProcessoParametrizadoRestMapper.paraResposta(null));
    }

    @Test
    void traduzTodasAsFalhasInternasSemPerderPayload() {
        var negocio = falha(FalhaConsultaProcessoParametrizado.Tipo.NEGOCIO, 404);
        var tecnica = falha(FalhaConsultaProcessoParametrizado.Tipo.TECNICA_CLIENTE, 422);
        var dependencia = falha(
                FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL, 503);
        var timeout = falha(FalhaConsultaProcessoParametrizado.Tipo.TIMEOUT, null);

        MtrBusinessErrorException erroNegocio = assertInstanceOf(
                MtrBusinessErrorException.class,
                ProcessoParametrizadoRestMapper.paraExcecaoRest(negocio));
        assertEquals(404, erroNegocio.status());
        assertEquals(404, erroNegocio.erro().codigoHttp());
        assertEquals("simtr-parametrizacao", erroNegocio.erro().recurso());
        assertEquals("processo-erro", erroNegocio.erro().idErro());
        assertEquals("MTR-PROCESSO", erroNegocio.erro().codigoErro());
        assertEquals("mensagem externa", erroNegocio.erro().erros().getFirst().mensagem());
        assertEquals("detalhe", erroNegocio.erro().detalhe());
        assertEquals("stacktrace", erroNegocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(MtrClientTechnicalException.class,
                ProcessoParametrizadoRestMapper.paraExcecaoRest(tecnica)).status());
        assertEquals(503, assertInstanceOf(MtrServerErrorException.class,
                ProcessoParametrizadoRestMapper.paraExcecaoRest(dependencia)).status());

        MtrServerErrorException erroTimeout = assertInstanceOf(
                MtrServerErrorException.class,
                ProcessoParametrizadoRestMapper.paraExcecaoRest(timeout));
        assertEquals(500, erroTimeout.status());
        assertNull(erroTimeout.erro().codigoHttp());
    }

    @Test
    void preservaElementoNuloNaListaDeErrosSemFalharAoCriarExcecao() {
        FalhaConsultaProcessoParametrizado falha = new FalhaConsultaProcessoParametrizado(
                FalhaConsultaProcessoParametrizado.Tipo.NEGOCIO,
                404,
                "simtr-parametrizacao",
                "processo-erro",
                "MTR-PROCESSO",
                Arrays.asList(null, "mensagem externa"),
                null,
                null,
                null);

        MtrBusinessErrorException excecao = assertInstanceOf(
                MtrBusinessErrorException.class,
                ProcessoParametrizadoRestMapper.paraExcecaoRest(falha));

        assertNull(excecao.erro().erros().getFirst());
        assertEquals("mensagem externa", excecao.erro().erros().get(1).mensagem());
    }

    private static ProcessoParametrizado processo() {
        ReferenciaChecklistProcessoParametrizado checklist =
                new ReferenciaChecklistProcessoParametrizado(70L, 7);
        TipoDocumentoProcessoParametrizado tipoDocumento =
                new TipoDocumentoProcessoParametrizado(
                        "TIPO", "Tipo", true, false, true, checklist);
        FuncaoDocumentalProcessoParametrizado funcao =
                new FuncaoDocumentalProcessoParametrizado(
                        "FUNCAO", List.of(tipoDocumento), checklist);
        DocumentoProcessoParametrizado documento =
                new DocumentoProcessoParametrizado(funcao, tipoDocumento, true);
        CampoFormularioProcessoParametrizado campo =
                new CampoFormularioProcessoParametrizado(
                        60L, "Campo", true, true, "condicao", 10, 1,
                        "texto", "mascara", "placeholder", 1, 20,
                        "preencha", false,
                        List.of(new OpcaoDisponivelProcessoParametrizado("A", "Opcao A", true)));
        GarantiaProcessoParametrizado garantia = new GarantiaProcessoParametrizado(
                50L, "Garantia", false, List.of(campo), List.of(documento), checklist);
        ProdutoProcessoParametrizado produto = new ProdutoProcessoParametrizado(
                40L, 41L, "Produto", List.of(campo), List.of(documento),
                List.of(garantia), checklist);
        RelacionamentoProcessoParametrizado relacionamento =
                new RelacionamentoProcessoParametrizado(
                        20L, "Relacionamento", "PF", true, true, false, false,
                        List.of(campo), List.of(documento));
        FaseProcessoParametrizado fase = new FaseProcessoParametrizado(
                30L, "Fase", true, "2026-07-14", 1, "orientacao",
                List.of(produto), List.of(garantia), List.of(campo),
                List.of(documento), List.of(checklist));

        return new ProcessoParametrizado(
                100L, "Processo", true, null, false,
                new MacroprocessoParametrizado(10L, "Macro", true, "2026-07-14"),
                Arrays.asList(relacionamento, null), null, List.of(fase),
                List.of(documento), checklist);
    }

    private static FalhaConsultaProcessoParametrizado falha(
            FalhaConsultaProcessoParametrizado.Tipo tipo,
            Integer status
    ) {
        return new FalhaConsultaProcessoParametrizado(
                tipo,
                status,
                "simtr-parametrizacao",
                "processo-erro",
                "MTR-PROCESSO",
                List.of("mensagem externa"),
                "detalhe",
                "stacktrace",
                new IllegalStateException("causa"));
    }
}
