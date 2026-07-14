package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.client.ParametrizacaoChecklistClient;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.dto.v1.checklist.ChecklistMtrResponse;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.erro.ChecklistMtrException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.mapper.ChecklistMtrMapper;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.smallrye.mutiny.Uni;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

class ChecklistMtrAdapterTest {

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
                "simtr-parametrizacao",
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
        assertEquals("simtr-parametrizacao", falha.recurso());
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

    private static ChecklistMtrAdapter adapterComFalha(Throwable falha) {
        var client = mock(ParametrizacaoChecklistClient.class);
        when(client.consultarPorIdentificadorNegocialEVersao(any(), any()))
                .thenReturn(Uni.createFrom().failure(falha));
        return new ChecklistMtrAdapter(client, new ChecklistMtrMapper());
    }
}
