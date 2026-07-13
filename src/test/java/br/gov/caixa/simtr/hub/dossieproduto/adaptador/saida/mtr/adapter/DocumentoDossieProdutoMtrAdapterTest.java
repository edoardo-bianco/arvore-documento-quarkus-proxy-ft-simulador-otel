package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.DocumentoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento.DocumentoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento.DocumentoDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.DocumentoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.DocumentoDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.AtributoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.PropriedadeDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoDocumentoDossieProduto;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoDossieProdutoMtrAdapterTest {

    private final DocumentoDossieProdutoMtrClient client =
            mock(DocumentoDossieProdutoMtrClient.class);
    private final DocumentoDossieProdutoMtrAdapter adapter =
            new DocumentoDossieProdutoMtrAdapter(
                    client, new DocumentoDossieProdutoMtrMapper());

    @Test
    void mapeiaComandoCompletoPreservandoNulosEConverteResposta() {
        when(client.incluir(eq(123L), any(DocumentoDossieProdutoMtrRequest.class)))
                .thenReturn(Uni.createFrom().item(
                        new DocumentoDossieProdutoMtrResponse(456L, 789L)));

        var resultado = adapter.incluir(comando()).await().indefinitely();

        ArgumentCaptor<DocumentoDossieProdutoMtrRequest> captor =
                ArgumentCaptor.forClass(DocumentoDossieProdutoMtrRequest.class);
        verify(client).incluir(eq(123L), captor.capture());
        DocumentoDossieProdutoMtrRequest request = captor.getValue();
        assertEquals(321L, request.id());
        assertEquals("dossie/123/documento.pdf", request.pathStorage());
        assertEquals("GED-321", request.codigoGed());
        assertEquals("GED-PRINCIPAL", request.objectStoreGed());
        assertEquals("CONTRATO", request.tipoDocumento());
        assertEquals("12345678901", request.vinculoDossie().cliente().cpf());
        assertEquals(10L, request.vinculoDossie().elementoConteudo());
        assertEquals(300, request.vinculoDossie().garantia().codigoBacen());
        assertNull(request.vinculoDossie().garantia().clienteAvalista().get(1));
        assertEquals(List.of("opcao-1"), request.atributos().getFirst().opcoesSelecionadas());
        assertNull(request.atributos().get(1));
        assertNull(request.propriedades().get(1));
        assertEquals(456L, resultado.identificadorDocumento());
        assertEquals(789L, resultado.identificadorInstanciaDocumento());
    }

    @Test
    void traduzErroMtrParaFalhaInternaLossless() {
        var erro = new DocumentoDossieProdutoMtrException.Erro(
                400, "simtr-dossie-produto", "documento-400", "MTR-DOC-400",
                List.of(new DocumentoDossieProdutoMtrException.Mensagem(
                        "documento nao permitido")),
                "negocio", "stack-remota");
        when(client.incluir(eq(123L), any(DocumentoDossieProdutoMtrRequest.class)))
                .thenReturn(Uni.createFrom().failure(
                        new DocumentoDossieProdutoMtrException.Negocio(400, erro)));

        FalhaInclusaoDocumentoDossieProduto falha = assertThrows(
                FalhaInclusaoDocumentoDossieProduto.class,
                () -> adapter.incluir(comando()).await().indefinitely());

        assertEquals(FalhaInclusaoDocumentoDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(400, falha.status());
        assertEquals("simtr-dossie-produto", falha.recurso());
        assertEquals("documento-400", falha.idErro());
        assertEquals("MTR-DOC-400", falha.codigoErro());
        assertEquals(List.of("documento nao permitido"), falha.mensagens());
        assertEquals("negocio", falha.detalhe());
        assertEquals("stack-remota", falha.stacktraceExterno());
    }

    @Test
    void classificaTimeoutDepoisDaPoliticaDoClient() {
        when(client.incluir(eq(123L), any(DocumentoDossieProdutoMtrRequest.class)))
                .thenReturn(Uni.createFrom().failure(new TimeoutException()));

        FalhaInclusaoDocumentoDossieProduto falha = assertThrows(
                FalhaInclusaoDocumentoDossieProduto.class,
                () -> adapter.incluir(comando()).await().indefinitely());

        assertEquals(FalhaInclusaoDocumentoDossieProduto.Tipo.TIMEOUT, falha.tipo());
        assertEquals("simtr-dossie-produto", falha.recurso());
    }

    private static ComandoInclusaoDocumentoDossieProduto comando() {
        List<ClienteDocumentoDossieProduto> avalistas = new ArrayList<>();
        avalistas.add(new ClienteDocumentoDossieProduto(
                "98765432100", "12345678000190", 2L));
        avalistas.add(null);

        List<AtributoDocumentoDossieProduto> atributos = new ArrayList<>();
        atributos.add(new AtributoDocumentoDossieProduto(
                "atributo", "valor", "objeto", List.of("opcao-1")));
        atributos.add(null);

        List<PropriedadeDocumentoDossieProduto> propriedades = new ArrayList<>();
        propriedades.add(new PropriedadeDocumentoDossieProduto(
                "propriedade", "valor", "objeto"));
        propriedades.add(null);

        return new ComandoInclusaoDocumentoDossieProduto(
                123L,
                321L,
                "dossie/123/documento.pdf",
                "GED-321",
                "GED-PRINCIPAL",
                "CONTRATO",
                new VinculoDocumentoDossieProduto(
                        new ClienteDocumentoDossieProduto("12345678901", null, 1L),
                        10L,
                        new GarantiaDocumentoDossieProduto(300, 400, 500, avalistas)),
                atributos,
                propriedades);
    }
}
