package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.client.ParametrizacaoProcessoClient;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.dto.v2.processo.ProcessoParametrizadoMtrResponse;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.erro.ProcessoParametrizadoMtrException;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.mtr.mapper.ProcessoParametrizadoMtrMapper;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.erro.FalhaConsultaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProcessoParametrizadoMtrAdapterTest {

    private static final String NOME_FUNCAO_DOCUMENTAL = "Formalizacao";
    private static final String SERVICO_MTR = "simtr-parametrizacao";

    @Test
    void mapeiaArvoreCompletaNulosEListasMutaveisParaOAgregadoDeLeitura() {
        var checklist = new ProcessoParametrizadoMtrResponse.ReferenciaChecklist(1000L, 5);
        var opcao = new ProcessoParametrizadoMtrResponse.OpcaoDisponivel("S", "Sim", true);
        var campo = new ProcessoParametrizadoMtrResponse.CampoFormulario(
                400L, "Autoriza?", true, true, "produto.ativo", 6, 4, "SELECAO",
                null, "Selecione", 1, 10, "Escolha", false, List.of(opcao)
        );
        var tipoDocumento = new ProcessoParametrizadoMtrResponse.TipoDocumento(
                "TIP-01", "Contrato", true, false, true, checklist
        );
        var funcao = new ProcessoParametrizadoMtrResponse.FuncaoDocumental(
                NOME_FUNCAO_DOCUMENTAL, List.of(tipoDocumento), checklist
        );
        var documento = new ProcessoParametrizadoMtrResponse.Documento(
                funcao, tipoDocumento, true
        );
        var garantia = new ProcessoParametrizadoMtrResponse.Garantia(
                700L, "Aval", true, List.of(campo), List.of(documento), checklist
        );
        var produto = new ProcessoParametrizadoMtrResponse.Produto(
                500L, 501L, "Capital de giro", List.of(campo), List.of(documento),
                List.of(garantia), checklist
        );
        var relacionamento = new ProcessoParametrizadoMtrResponse.Relacionamento(
                300L, "Proponente", "PF", true, true, false, false,
                List.of(campo), List.of(documento)
        );
        var fase = new ProcessoParametrizadoMtrResponse.Fase(
                900L, NOME_FUNCAO_DOCUMENTAL, true, null, 2, "Envie os documentos",
                List.of(produto), List.of(garantia), List.of(campo), List.of(documento),
                List.of(checklist)
        );
        var resposta = new ProcessoParametrizadoMtrResponse(
                100L,
                "Contratacao",
                true,
                "2026-07-14T12:00:00",
                false,
                new ProcessoParametrizadoMtrResponse.Macroprocesso(
                        200L, "Credito", true, null
                ),
                Arrays.asList(relacionamento, null),
                List.of(produto),
                List.of(fase),
                new ArrayList<>(List.of(documento)),
                checklist
        );
        var identificadorRecebido = new AtomicReference<Long>();
        var client = mock(ParametrizacaoProcessoClient.class);
        when(client.consultarPorIdentificadorNegocial(any())).thenAnswer(invocacao -> {
            identificadorRecebido.set(invocacao.getArgument(0));
            return Uni.createFrom().item(resposta);
        });
        var adapter = new ProcessoParametrizadoMtrAdapter(
                client, new ProcessoParametrizadoMtrMapper()
        );

        var resultado = adapter.obter(new IdentificadorNegocialProcesso(100L))
                .await().indefinitely();

        assertEquals(100L, identificadorRecebido.get());
        assertEquals(100L, resultado.identificadorNegocial());
        assertEquals("Contratacao", resultado.nome());
        assertEquals(false, resultado.indicadorProdutoObrigatorio());
        assertEquals(200L, resultado.macroprocesso().identificadorNegocial());
        assertNull(resultado.macroprocesso().ultimaAlteracao());
        assertEquals("PF", resultado.relacionamentos().getFirst().tipoPessoa());
        assertNull(resultado.relacionamentos().get(1));
        assertEquals("produto.ativo", resultado.relacionamentos().getFirst()
                .camposFormulario().getFirst().exibicaoCondicional());
        assertEquals("S", resultado.relacionamentos().getFirst().camposFormulario().getFirst()
                .opcoesDisponiveis().getFirst().valorOpcao());
        assertEquals(500L, resultado.produtos().getFirst().codigoOperacao());
        assertEquals(501L, resultado.produtos().getFirst().codigoModalidade());
        assertEquals(700L, resultado.produtos().getFirst().garantias().getFirst().codigoBacen());
        assertEquals(NOME_FUNCAO_DOCUMENTAL,
                resultado.produtos().getFirst().documentos().getFirst()
                .funcaoDocumental().nome());
        assertEquals("TIP-01", resultado.produtos().getFirst().documentos().getFirst()
                .tipoDocumento().codigoTipologia());
        assertEquals(2, resultado.fases().getFirst().ordem());
        assertEquals("Envie os documentos", resultado.fases().getFirst().orientacaoUsuario());
        assertEquals(1000L, resultado.fases().getFirst().checklist().getFirst()
                .identificadorChecklist());
        assertEquals(5, resultado.checklist().versaoChecklist());
        assertDoesNotThrow(() -> resultado.relacionamentos().add(null));
        assertDoesNotThrow(() -> resultado.documentos().clear());
    }

    @Test
    void traduzErroDeNegocioSemPerderCamposOuElementosNulos() {
        var erro = new ProcessoParametrizadoMtrException.Erro(
                404,
                SERVICO_MTR,
                "processo-404",
                "MTR-PROCESSO-404",
                Arrays.asList(
                        new ProcessoParametrizadoMtrException.Mensagem("processo nao localizado"),
                        null
                ),
                "negocio",
                "stacktrace externo"
        );
        var origem = new ProcessoParametrizadoMtrException.Negocio(404, erro);
        var adapter = adapterComFalha(origem);

        var falha = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                () -> adapter.obter(new IdentificadorNegocialProcesso(100L)).await().indefinitely()
        );

        assertEquals(FalhaConsultaProcessoParametrizado.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals("processo-404", falha.idErro());
        assertEquals("MTR-PROCESSO-404", falha.codigoErro());
        assertEquals(Arrays.asList("processo nao localizado", null), falha.mensagens());
        assertEquals("negocio", falha.detalhe());
        assertEquals("stacktrace externo", falha.stacktraceExterno());
        assertSame(origem, falha.getCause());
    }

    @Test
    void traduzFalhasTecnicaClienteEServidorParaClassificacoesInternas() {
        var erro = new ProcessoParametrizadoMtrException.Erro(
                429, null, null, null, null, null, null
        );
        var tecnica = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                () -> adapterComFalha(new ProcessoParametrizadoMtrException.TecnicaCliente(
                        429, erro
                )).obter(new IdentificadorNegocialProcesso(100L)).await().indefinitely()
        );
        var servidor = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                () -> adapterComFalha(new ProcessoParametrizadoMtrException.Servidor(
                        503, erro
                )).obter(new IdentificadorNegocialProcesso(100L)).await().indefinitely()
        );

        assertEquals(FalhaConsultaProcessoParametrizado.Tipo.TECNICA_CLIENTE, tecnica.tipo());
        assertEquals(FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL,
                servidor.tipo());
    }

    @Test
    void traduzTimeoutSomenteDepoisDaFalhaDoClient() {
        var origem = new TimeoutException("tempo esgotado");

        var falha = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                () -> adapterComFalha(origem).obter(new IdentificadorNegocialProcesso(100L))
                        .await().indefinitely()
        );

        assertEquals(FalhaConsultaProcessoParametrizado.Tipo.TIMEOUT, falha.tipo());
        assertSame(origem, falha.getCause());
    }

    @Test
    void preservaAusenciaDeIdentificadorEResposta() {
        var client = mock(ParametrizacaoProcessoClient.class);
        when(client.consultarPorIdentificadorNegocial(isNull()))
                .thenReturn(Uni.createFrom().nullItem());
        var adapter = new ProcessoParametrizadoMtrAdapter(
                client, new ProcessoParametrizadoMtrMapper()
        );

        var resultado = adapter.obter(new IdentificadorNegocialProcesso(null))
                .await().indefinitely();

        assertNull(resultado);
        verify(client).consultarPorIdentificadorNegocial(isNull());
    }

    @Test
    void preservaCamposOpcionaisNulosNaRespostaParcial() {
        var resposta = new ProcessoParametrizadoMtrResponse(
                null, null, null, null, null, null, null, null, null, null, null
        );
        var client = mock(ParametrizacaoProcessoClient.class);
        when(client.consultarPorIdentificadorNegocial(100L))
                .thenReturn(Uni.createFrom().item(resposta));
        var adapter = new ProcessoParametrizadoMtrAdapter(
                client, new ProcessoParametrizadoMtrMapper()
        );

        var resultado = adapter.obter(new IdentificadorNegocialProcesso(100L))
                .await().indefinitely();

        assertNull(resultado.identificadorNegocial());
        assertNull(resultado.nome());
        assertNull(resultado.macroprocesso());
        assertNull(resultado.relacionamentos());
        assertNull(resultado.produtos());
        assertNull(resultado.fases());
        assertNull(resultado.documentos());
        assertNull(resultado.checklist());
    }

    @Test
    void traduzErroTecnicoSemCorpo() {
        var origem = new ProcessoParametrizadoMtrException.TecnicaCliente(422, null);
        var espera = adapterComFalha(origem).obter(new IdentificadorNegocialProcesso(100L))
                .await();

        var falha = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                espera::indefinitely
        );

        assertEquals(FalhaConsultaProcessoParametrizado.Tipo.TECNICA_CLIENTE, falha.tipo());
        assertEquals(422, falha.status());
        assertNull(falha.recurso());
        assertNull(falha.idErro());
        assertNull(falha.codigoErro());
        assertNull(falha.mensagens());
        assertNull(falha.detalhe());
        assertNull(falha.stacktraceExterno());
        assertSame(origem, falha.getCause());
    }

    @Test
    void traduzErroDeServidorSemListaDeMensagens() {
        var erro = new ProcessoParametrizadoMtrException.Erro(
                503, SERVICO_MTR, "processo-503", "MTR-PROCESSO-503",
                null, "indisponivel", null
        );
        var origem = new ProcessoParametrizadoMtrException.Servidor(503, erro);
        var espera = adapterComFalha(origem).obter(new IdentificadorNegocialProcesso(100L))
                .await();

        var falha = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                espera::indefinitely
        );

        assertEquals(
                FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo()
        );
        assertEquals(503, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertNull(falha.mensagens());
        assertSame(origem, falha.getCause());
    }

    @Test
    void classificaSubtipoMtrDesconhecidoEPreservaMensagemNula() {
        var erro = new ProcessoParametrizadoMtrException.Erro(
                500, SERVICO_MTR, "processo-500", "MTR-PROCESSO-500",
                Collections.singletonList(null), null, null
        );
        var origem = mock(ProcessoParametrizadoMtrException.class);
        when(origem.status()).thenReturn(500);
        when(origem.erro()).thenReturn(erro);
        var espera = adapterComFalha(origem).obter(new IdentificadorNegocialProcesso(100L))
                .await();

        var falha = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                espera::indefinitely
        );

        assertEquals(
                FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo()
        );
        assertEquals(Collections.singletonList(null), falha.mensagens());
        assertSame(origem, falha.getCause());
    }

    @Test
    void traduzFalhaInesperadaComoDependenciaIndisponivel() {
        var origem = new IllegalStateException("falha inesperada");
        var espera = adapterComFalha(origem).obter(new IdentificadorNegocialProcesso(100L))
                .await();

        var falha = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                espera::indefinitely
        );

        assertEquals(
                FalhaConsultaProcessoParametrizado.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo()
        );
        assertEquals(SERVICO_MTR, falha.recurso());
        assertSame(origem, falha.getCause());
    }

    private static ProcessoParametrizadoMtrAdapter adapterComFalha(Throwable falha) {
        var client = mock(ParametrizacaoProcessoClient.class);
        when(client.consultarPorIdentificadorNegocial(any()))
                .thenReturn(Uni.createFrom().failure(falha));
        return new ProcessoParametrizadoMtrAdapter(
                client,
                new ProcessoParametrizadoMtrMapper()
        );
    }
}
