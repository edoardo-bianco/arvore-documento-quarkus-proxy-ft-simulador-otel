package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.client.ParametrizacaoChecklistClient;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.dto.v1.checklist.ChecklistMtrResponse;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.erro.ChecklistMtrException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.mapper.ChecklistMtrMapper;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ChecklistMtrAdapterTest {

    private static final String SERVICO_MTR = "simtr-parametrizacao";

    @Test
    void enviaIdentificadorEVersaoEMapeiaRespostaParaODominio() {
        var resposta = new ChecklistMtrResponse(
                "Checklist documental",
                101L,
                3,
                "2026-07-14T10:00:00",
                null,
                true,
                "Validar documentos",
                List.of(new ChecklistMtrResponse.Apontamento(
                        201L, "Documento", null, "Conferir", false, 1
                ))
        );
        var identificadorRecebido = new AtomicReference<Long>();
        var versaoRecebida = new AtomicReference<Integer>();
        var client = mock(ParametrizacaoChecklistClient.class);
        when(client.consultarPorIdentificadorNegocialEVersao(any(), any()))
                .thenAnswer(invocacao -> {
                    identificadorRecebido.set(invocacao.getArgument(0));
                    versaoRecebida.set(invocacao.getArgument(1));
                    return Uni.createFrom().item(resposta);
                });
        var adapter = new ChecklistMtrAdapter(client, new ChecklistMtrMapper());

        var resultado = adapter.obter(new ComandoConsultaChecklist(101L, 3))
                .await().indefinitely();

        assertEquals(101L, identificadorRecebido.get());
        assertEquals(3, versaoRecebida.get());
        assertEquals(101L, resultado.identificadorNegocial());
        assertEquals("Checklist documental", resultado.nome());
        assertEquals(201L, resultado.apontamentos().getFirst().identificadorNegocial());
    }

    @Test
    void traduzErroDeNegocioSemPerderCamposOuElementosNulos() {
        var erro = new ChecklistMtrException.Erro(
                404,
                SERVICO_MTR,
                "checklist-404",
                "MTR-CHECKLIST-404",
                Arrays.asList(new ChecklistMtrException.Mensagem("nao localizado"), null),
                "negocio",
                "stacktrace externo"
        );
        var origem = new ChecklistMtrException.Negocio(404, erro);

        var falha = assertThrows(
                FalhaConsultaChecklist.class,
                () -> adapterComFalha(origem).obter(new ComandoConsultaChecklist(101L, 3))
                        .await().indefinitely()
        );

        assertEquals(FalhaConsultaChecklist.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals("checklist-404", falha.idErro());
        assertEquals("MTR-CHECKLIST-404", falha.codigoErro());
        assertEquals(Arrays.asList("nao localizado", null), falha.mensagens());
        assertEquals("negocio", falha.detalhe());
        assertEquals("stacktrace externo", falha.stacktraceExterno());
        assertSame(origem, falha.getCause());
    }

    @Test
    void traduzFalhasTecnicaClienteEServidorParaClassificacoesInternas() {
        var erro = new ChecklistMtrException.Erro(
                429, null, null, null, null, null, null
        );
        var tecnica = assertThrows(
                FalhaConsultaChecklist.class,
                () -> adapterComFalha(new ChecklistMtrException.TecnicaCliente(429, erro))
                        .obter(new ComandoConsultaChecklist(101L, 3)).await().indefinitely()
        );
        var servidor = assertThrows(
                FalhaConsultaChecklist.class,
                () -> adapterComFalha(new ChecklistMtrException.Servidor(503, erro))
                        .obter(new ComandoConsultaChecklist(101L, 3)).await().indefinitely()
        );

        assertEquals(FalhaConsultaChecklist.Tipo.TECNICA_CLIENTE, tecnica.tipo());
        assertEquals(FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL, servidor.tipo());
    }

    @Test
    void traduzTimeoutSomenteDepoisDaFalhaDoClient() {
        var origem = new TimeoutException("tempo esgotado");

        var falha = assertThrows(
                FalhaConsultaChecklist.class,
                () -> adapterComFalha(origem).obter(new ComandoConsultaChecklist(101L, 3))
                        .await().indefinitely()
        );

        assertEquals(FalhaConsultaChecklist.Tipo.TIMEOUT, falha.tipo());
        assertSame(origem, falha.getCause());
    }

    @Test
    void preservaAusenciaDeComandoEResposta() {
        var client = mock(ParametrizacaoChecklistClient.class);
        when(client.consultarPorIdentificadorNegocialEVersao(isNull(), isNull()))
                .thenReturn(Uni.createFrom().nullItem());
        var adapter = new ChecklistMtrAdapter(client, new ChecklistMtrMapper());

        var resultado = adapter.obter(new ComandoConsultaChecklist(null, null))
                .await().indefinitely();

        assertNull(resultado);
        verify(client).consultarPorIdentificadorNegocialEVersao(isNull(), isNull());
    }

    @Test
    void preservaCamposOpcionaisNulosNaRespostaParcial() {
        var resposta = new ChecklistMtrResponse(
                null, null, null, null, null, null, null, null
        );
        var client = mock(ParametrizacaoChecklistClient.class);
        when(client.consultarPorIdentificadorNegocialEVersao(101L, 3))
                .thenReturn(Uni.createFrom().item(resposta));
        var adapter = new ChecklistMtrAdapter(client, new ChecklistMtrMapper());

        var resultado = adapter.obter(new ComandoConsultaChecklist(101L, 3))
                .await().indefinitely();

        assertNull(resultado.identificadorNegocial());
        assertNull(resultado.nome());
        assertNull(resultado.versao());
        assertNull(resultado.apontamentos());
    }

    @Test
    void traduzErroTecnicoSemCorpo() {
        var origem = new ChecklistMtrException.TecnicaCliente(422, null);
        var espera = adapterComFalha(origem).obter(new ComandoConsultaChecklist(101L, 3))
                .await();

        var falha = assertThrows(
                FalhaConsultaChecklist.class,
                espera::indefinitely
        );

        assertEquals(FalhaConsultaChecklist.Tipo.TECNICA_CLIENTE, falha.tipo());
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
        var erro = new ChecklistMtrException.Erro(
                503, SERVICO_MTR, "checklist-503", "MTR-CHECKLIST-503",
                null, "indisponivel", null
        );
        var origem = new ChecklistMtrException.Servidor(503, erro);
        var espera = adapterComFalha(origem).obter(new ComandoConsultaChecklist(101L, 3))
                .await();

        var falha = assertThrows(
                FalhaConsultaChecklist.class,
                espera::indefinitely
        );

        assertEquals(FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL, falha.tipo());
        assertEquals(503, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertNull(falha.mensagens());
        assertSame(origem, falha.getCause());
    }

    @Test
    void classificaSubtipoMtrDesconhecidoEPreservaMensagemNula() {
        var erro = new ChecklistMtrException.Erro(
                500, SERVICO_MTR, "checklist-500", "MTR-CHECKLIST-500",
                Collections.singletonList(null), null, null
        );
        var origem = mock(ChecklistMtrException.class);
        when(origem.status()).thenReturn(500);
        when(origem.erro()).thenReturn(erro);
        var espera = adapterComFalha(origem).obter(new ComandoConsultaChecklist(101L, 3))
                .await();

        var falha = assertThrows(
                FalhaConsultaChecklist.class,
                espera::indefinitely
        );

        assertEquals(FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL, falha.tipo());
        assertEquals(Collections.singletonList(null), falha.mensagens());
        assertSame(origem, falha.getCause());
    }

    @Test
    void traduzFalhaInesperadaComoDependenciaIndisponivel() {
        var origem = new IllegalStateException("falha inesperada");
        var espera = adapterComFalha(origem).obter(new ComandoConsultaChecklist(101L, 3))
                .await();

        var falha = assertThrows(
                FalhaConsultaChecklist.class,
                espera::indefinitely
        );

        assertEquals(FalhaConsultaChecklist.Tipo.DEPENDENCIA_INDISPONIVEL, falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertSame(origem, falha.getCause());
    }

    private static ChecklistMtrAdapter adapterComFalha(Throwable falha) {
        var client = mock(ParametrizacaoChecklistClient.class);
        when(client.consultarPorIdentificadorNegocialEVersao(any(), any()))
                .thenReturn(Uni.createFrom().failure(falha));
        return new ChecklistMtrAdapter(client, new ChecklistMtrMapper());
    }
}
