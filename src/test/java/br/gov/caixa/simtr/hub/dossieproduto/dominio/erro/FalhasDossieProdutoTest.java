package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

class FalhasDossieProdutoTest {

    private static final Integer STATUS = 422;
    private static final String RECURSO = "/dossies/123";
    private static final String ID_ERRO = "erro-123";
    private static final String CODIGO_ERRO = "MTR-123";
    private static final String MENSAGEM = "mensagem devolvida pela dependencia";
    private static final String DETALHE = "detalhe externo";
    private static final String STACKTRACE_EXTERNO = "stacktrace externo";
    private static final String MENSAGEM_CAUSA = "mensagem da causa";
    private static final String CANAL_CRIACAO = "SIMTRAPI";
    private static final String CPF_CLIENTE = "00000000000";
    private static final String NOME_CLIENTE = "CLIENTE SIMULADO";
    private static final String TIPO_VINCULO = "Proponente";
    private static final String NOME_PROCESSO = "Concessão Habitacional";
    private static final String MACROPROCESSO = "HABITAÇÃO";
    private static final String NOME_FASE = "Recepção de dados e documentos";
    private static final String DATA_REFERENCIA = "23/07/2026 10:24:00";
    private static final String NOME_SITUACAO = "Rascunho";

    @Test
    void preservaModeloDeLeituraComNulosListasVaziasEDatasOpacas() {
        var cliente = new DossieProdutoConsultado.Cliente(
                CPF_CLIENTE,
                null,
                NOME_CLIENTE,
                null,
                TIPO_VINCULO,
                40610702L,
                true);
        var processo = new DossieProdutoConsultado.Processo(
                5032,
                NOME_PROCESSO,
                1000016487L,
                MACROPROCESSO,
                null,
                true,
                true);
        var fase = new DossieProdutoConsultado.Fase(
                5033,
                NOME_FASE,
                1000016488L,
                DATA_REFERENCIA);
        var situacao = new DossieProdutoConsultado.Situacao(
                1,
                NOME_SITUACAO,
                DATA_REFERENCIA,
                CANAL_CRIACAO);
        var produto = new DossieProdutoConsultado.ProdutoContratado(
                null,
                null,
                null,
                null);
        List<DossieProdutoConsultado.Cliente> clientes = Arrays.asList(cliente, null);
        List<Integer> unidadesTratamento = List.of();
        List<DossieProdutoConsultado.ProdutoContratado> produtos = List.of(produto);

        var dossie = new DossieProdutoConsultado(
                4324680L,
                1000012592L,
                null,
                null,
                CANAL_CRIACAO,
                5402,
                null,
                clientes,
                processo,
                fase,
                situacao,
                unidadesTratamento,
                produtos);

        assertCabecalhoDossie(dossie, clientes, processo, fase, situacao,
                unidadesTratamento, produtos);
        assertCliente(cliente, clientes);
        assertProcesso(processo);
        assertFaseESituacao(fase, situacao);
        assertProduto(unidadesTratamento, produto);
    }

    private static void assertCabecalhoDossie(
            DossieProdutoConsultado dossie,
            List<DossieProdutoConsultado.Cliente> clientes,
            DossieProdutoConsultado.Processo processo,
            DossieProdutoConsultado.Fase fase,
            DossieProdutoConsultado.Situacao situacao,
            List<Integer> unidadesTratamento,
            List<DossieProdutoConsultado.ProdutoContratado> produtos
    ) {
        assertEquals(4324680L, dossie.id());
        assertEquals(1000012592L, dossie.chaveCorrelacaoCanal());
        assertNull(dossie.instanciaJbpm());
        assertNull(dossie.numeroNegocio());
        assertEquals(CANAL_CRIACAO, dossie.canalCriacao());
        assertEquals(5402, dossie.unidadeCriacao());
        assertNull(dossie.dataCriacao());
        assertSame(clientes, dossie.clientes());
        assertSame(processo, dossie.processo());
        assertSame(fase, dossie.faseAtual());
        assertSame(situacao, dossie.situacaoAtual());
        assertSame(unidadesTratamento, dossie.unidadesTratamento());
        assertSame(produtos, dossie.produtosContratados());
    }

    private static void assertCliente(
            DossieProdutoConsultado.Cliente cliente,
            List<DossieProdutoConsultado.Cliente> clientes
    ) {
        assertEquals(CPF_CLIENTE, cliente.cpf());
        assertNull(cliente.cnpj());
        assertEquals(NOME_CLIENTE, cliente.nome());
        assertNull(cliente.razaoSocial());
        assertEquals(TIPO_VINCULO, cliente.tipoVinculo());
        assertEquals(40610702L, cliente.identificadorNegocialVinculo());
        assertEquals(true, cliente.principal());
        assertNull(clientes.get(1));
    }

    private static void assertProcesso(DossieProdutoConsultado.Processo processo) {
        assertEquals(5032, processo.id());
        assertEquals(NOME_PROCESSO, processo.nome());
        assertEquals(1000016487L, processo.identificadorNegocial());
        assertEquals(MACROPROCESSO, processo.macroprocesso());
        assertNull(processo.data());
        assertEquals(true, processo.tratamentoSeletivo());
        assertEquals(true, processo.complementacaoSeletiva());
    }

    private static void assertFaseESituacao(
            DossieProdutoConsultado.Fase fase,
            DossieProdutoConsultado.Situacao situacao
    ) {
        assertEquals(5033, fase.id());
        assertEquals(NOME_FASE, fase.nome());
        assertEquals(1000016488L, fase.identificadorNegocial());
        assertEquals(DATA_REFERENCIA, fase.data());

        assertEquals(1, situacao.id());
        assertEquals(NOME_SITUACAO, situacao.nome());
        assertEquals(DATA_REFERENCIA, situacao.data());
        assertEquals(CANAL_CRIACAO, situacao.matricula());
    }

    private static void assertProduto(
            List<Integer> unidadesTratamento,
            DossieProdutoConsultado.ProdutoContratado produto
    ) {
        assertEquals(0, unidadesTratamento.size());
        assertNull(produto.id());
        assertNull(produto.codigoOperacao());
        assertNull(produto.codigoModalidade());
        assertNull(produto.nome());
    }

    @Test
    void preservaResultadoMinimoDaCaptura() {
        var resultado = new ResultadoCapturaDossieProduto(123L);
        var componentes = ResultadoCapturaDossieProduto.class.getRecordComponents();

        assertEquals(123L, resultado.identificadorDossieProduto());
        assertEquals(1, componentes.length);
        assertEquals("identificadorDossieProduto", componentes[0].getName());
        assertEquals(Long.class, componentes[0].getType());
    }

    @Test
    void caracterizaFalhaDeCaptura() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaCaptura(mensagens, causa);

        assertEquals(FalhaCapturaDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals("Falha ao capturar dossie produto", falha.getMessage());
        assertEquals(
                "Falha ao capturar dossie produto",
                falhaCaptura(null, causa).getMessage());
        assertEquals(
                "Falha ao capturar dossie produto",
                falhaCaptura(null, null).getMessage());
    }

    @Test
    void classificaTodosOsTiposDeFalhaDeCaptura() {
        assertArrayEquals(
                new FalhaCapturaDossieProduto.Tipo[]{
                        FalhaCapturaDossieProduto.Tipo.NEGOCIO,
                        FalhaCapturaDossieProduto.Tipo.TECNICA_CLIENTE,
                        FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                        FalhaCapturaDossieProduto.Tipo.TIMEOUT
                },
                FalhaCapturaDossieProduto.Tipo.values());
    }

    @Test
    void caracterizaFalhaDeConsulta() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaConsulta(mensagens, causa);

        assertEquals(FalhaConsultaDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaConsulta,
                "Falha ao consultar dossie produto");
    }

    @Test
    void classificaTodosOsTiposDeFalhaDeConsulta() {
        assertArrayEquals(
                new FalhaConsultaDossieProduto.Tipo[]{
                        FalhaConsultaDossieProduto.Tipo.NEGOCIO,
                        FalhaConsultaDossieProduto.Tipo.TECNICA_CLIENTE,
                        FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                        FalhaConsultaDossieProduto.Tipo.TIMEOUT
                },
                FalhaConsultaDossieProduto.Tipo.values());
    }

    @Test
    void caracterizaFalhaDeCriacao() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaCriacao(mensagens, causa);

        assertEquals(FalhaCriacaoDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaCriacao,
                "Falha ao criar dossie produto");
    }

    @Test
    void caracterizaFalhaDeAtualizacaoDeFormulario() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaAtualizacaoFormulario(mensagens, causa);

        assertEquals(FalhaAtualizacaoFormularioDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaAtualizacaoFormulario,
                "Falha ao atualizar formulario do dossie produto");
    }

    @Test
    void caracterizaFalhaDeAlteracaoDeProdutosContratados() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaAlteracaoProdutosContratados(mensagens, causa);

        assertEquals(FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaAlteracaoProdutosContratados,
                "Falha ao alterar produtos contratados do dossie produto");
    }

    @Test
    void caracterizaFalhaDeInclusaoDeDocumento() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaInclusaoDocumento(mensagens, causa);

        assertEquals(FalhaInclusaoDocumentoDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaInclusaoDocumento,
                "Falha ao incluir documento no dossie produto");
    }

    @Test
    void caracterizaFalhaDeRegistroDeValidacaoNegocial() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaRegistroValidacaoNegocial(mensagens, causa);

        assertEquals(FalhaRegistroValidacaoNegocialDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaRegistroValidacaoNegocial,
                "Falha ao registrar validacao negocial no dossie produto");
    }

    @Test
    void caracterizaFalhaDeWorkflow() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaWorkflow(mensagens, causa);

        assertEquals(FalhaWorkflowDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaWorkflow,
                "Falha ao avancar workflow do dossie produto");
    }

    private static void assertMensagensDefensivas(
            BiFunction<List<String>, Throwable, RuntimeException> fabrica,
            String fallback
    ) {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);

        assertEquals(MENSAGEM_CAUSA, fabrica.apply(null, causa).getMessage());
        assertEquals(MENSAGEM_CAUSA, fabrica.apply(List.of(), causa).getMessage());
        assertEquals(
                MENSAGEM_CAUSA,
                fabrica.apply(Collections.singletonList(null), causa).getMessage());
        assertEquals(fallback, fabrica.apply(null, null).getMessage());
    }

    private static FalhaCriacaoDossieProduto falhaCriacao(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaCriacaoDossieProduto(
                FalhaCriacaoDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaCapturaDossieProduto falhaCaptura(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaCapturaDossieProduto(
                FalhaCapturaDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaConsultaDossieProduto falhaConsulta(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaConsultaDossieProduto(
                FalhaConsultaDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaAtualizacaoFormularioDossieProduto falhaAtualizacaoFormulario(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaAtualizacaoFormularioDossieProduto(
                FalhaAtualizacaoFormularioDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaInclusaoDocumentoDossieProduto falhaInclusaoDocumento(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaInclusaoDocumentoDossieProduto(
                FalhaInclusaoDocumentoDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaAlteracaoProdutosContratadosDossieProduto
            falhaAlteracaoProdutosContratados(
                    List<String> mensagens,
                    Throwable causa
            ) {
        return new FalhaAlteracaoProdutosContratadosDossieProduto(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaRegistroValidacaoNegocialDossieProduto falhaRegistroValidacaoNegocial(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaRegistroValidacaoNegocialDossieProduto(
                FalhaRegistroValidacaoNegocialDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaWorkflowDossieProduto falhaWorkflow(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaWorkflowDossieProduto(
                FalhaWorkflowDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }
}
