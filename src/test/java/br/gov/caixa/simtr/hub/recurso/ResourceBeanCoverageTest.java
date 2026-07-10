package br.gov.caixa.simtr.hub.recurso;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.DossieProdutoResource;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.GestaoDocumentoResource;
import br.gov.caixa.simtr.hub.parametrizacao.fachada.ParametrizacaoFachada;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.ChecklistResource;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.ProcessoResource;
import br.gov.caixa.simtr.hub.dossieproduto.fachada.DossieProdutoFachada;
import br.gov.caixa.simtr.hub.gestaodocumento.fachada.GestaoDocumentoFachada;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoCriadoVo;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.GestaoDocumentoCredencialContainerVo;
import br.gov.caixa.simtr.hub.dossieproduto.mapeamento.DossieProdutoMapper;
import br.gov.caixa.simtr.hub.gestaodocumento.mapeamento.GestaoDocumentoMapper;
import br.gov.caixa.simtr.hub.parametrizacao.mapeamento.ChecklistMapper;
import br.gov.caixa.simtr.hub.parametrizacao.mapeamento.ProcessoMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class ResourceBeanCoverageTest {

    @Inject
    ProcessoResource processoResource;

    @Inject
    ChecklistResource checklistResource;

    @Inject
    DossieProdutoResource dossieProdutoResource;

    @Inject
    GestaoDocumentoResource gestaoDocumentoResource;

    @Inject
    ProcessoMapper processoMapper;

    @Inject
    ChecklistMapper checklistMapper;

    @Inject
    DossieProdutoMapper dossieProdutoMapper;

    @Inject
    GestaoDocumentoMapper gestaoDocumentoMapper;

    @InjectMock
    ParametrizacaoFachada parametrizacaoFachada;

    @InjectMock
    DossieProdutoFachada dossieProdutoFachada;

    @InjectMock
    GestaoDocumentoFachada gestaoDocumentoFachada;

    @Test
    void processoResourceCobreSucessoEFalhaDoBeanCdi() {
        when(parametrizacaoFachada.consultarProcessoPorIdentificadorNegocial(1L))
                .thenReturn(Uni.createFrom().item(processoMapper.toVo(TestFixtures.processoDto())));
        when(parametrizacaoFachada.consultarProcessoPorIdentificadorNegocial(2L))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha processo")));

        var resposta = processoResource.consultarPorIdentificadorNegocial(1L)
                .await().indefinitely();

        assertEquals(TestFixtures.processoDto().nome(), resposta.nome());
        assertThrows(IllegalStateException.class, () -> processoResource.consultarPorIdentificadorNegocial(2L)
                .await().indefinitely());
    }

    @Test
    void checklistResourceCobreSucessoEFalhaDoBeanCdi() {
        when(parametrizacaoFachada.consultarChecklistPorIdentificadorNegocialEVersao(1L, 1))
                .thenReturn(Uni.createFrom().item(checklistMapper.toVo(TestFixtures.checklistDto())));
        when(parametrizacaoFachada.consultarChecklistPorIdentificadorNegocialEVersao(2L, 1))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha checklist")));

        var resposta = checklistResource.consultarPorIdentificadorNegocialEVersao(1L, 1)
                .await().indefinitely();

        assertEquals(TestFixtures.checklistDto().nome(), resposta.nome());
        assertThrows(IllegalStateException.class, () -> checklistResource.consultarPorIdentificadorNegocialEVersao(2L, 1)
                .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobrePostSucessoEFalhaDoBeanCdi() {
        when(dossieProdutoFachada.criarDossieProduto(any()))
                .thenReturn(Uni.createFrom().item(new DossieProdutoCriadoVo(77L)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha dossie")));

        Response response = dossieProdutoResource.criarDossieProduto(TestFixtures.dossieCriacaoDto())
                .await().indefinitely();

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(77L, ((DossieProdutoCriadoDto) response.getEntity()).id());
        assertThrows(IllegalStateException.class, () -> dossieProdutoResource.criarDossieProduto(TestFixtures.dossieCriacaoDto())
                .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobrePatchSucessoEFalhaDoBeanCdi() {
        when(dossieProdutoFachada.atualizarFormularioDossieProduto(eq(123L), any()))
                .thenReturn(Uni.createFrom().item(new DossieProdutoCriadoVo(123L)));
        when(dossieProdutoFachada.atualizarFormularioDossieProduto(eq(124L), any()))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha formulario")));

        Response response = dossieProdutoResource.atualizarFormularioDossieProduto(123L, TestFixtures.formularioDto())
                .await().indefinitely();

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(123L, ((DossieProdutoCriadoDto) response.getEntity()).id());
        assertThrows(IllegalStateException.class,
                () -> dossieProdutoResource.atualizarFormularioDossieProduto(124L, TestFixtures.formularioDto())
                        .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobreDocumentoSucessoEFalhaDoBeanCdi() {
        when(dossieProdutoFachada.incluirDocumentoDossieProduto(eq(123L), any()))
                .thenReturn(Uni.createFrom().item(new DossieProdutoDocumentoCriadoVo(456L, 789L)));
        when(dossieProdutoFachada.incluirDocumentoDossieProduto(eq(124L), any()))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha documento")));

        Response response = dossieProdutoResource.incluirDocumentoDossieProduto(123L, TestFixtures.documentoInclusaoDto())
                .await().indefinitely();

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(456L, ((DossieProdutoDocumentoCriadoDto) response.getEntity()).idDocumento());
        assertEquals(789L, ((DossieProdutoDocumentoCriadoDto) response.getEntity()).idInstanciaDocumento());
        assertThrows(IllegalStateException.class,
                () -> dossieProdutoResource.incluirDocumentoDossieProduto(124L, TestFixtures.documentoInclusaoDto())
                        .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobreValidacaoNegocialSucessoEFalhaDoBeanCdi() {
        when(dossieProdutoFachada.registrarValidacaoNegocialDossieProduto(eq(123L), any()))
                .thenReturn(Uni.createFrom().voidItem());
        when(dossieProdutoFachada.registrarValidacaoNegocialDossieProduto(eq(124L), any()))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha validacao negocial")));

        Response response = dossieProdutoResource.registrarValidacaoNegocialDossieProduto(
                        123L,
                        TestFixtures.validacaoNegocialDto()
                )
                .await().indefinitely();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(null, response.getEntity());
        assertThrows(IllegalStateException.class,
                () -> dossieProdutoResource.registrarValidacaoNegocialDossieProduto(
                                124L,
                                TestFixtures.validacaoNegocialDto()
                        )
                        .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobreWorkflowSucessoEFalhaDoBeanCdi() {
        when(dossieProdutoFachada.iniciarOuAvancarWorkflowDossieProduto(123L))
                .thenReturn(Uni.createFrom().item(new DossieProdutoCriadoVo(123L)));
        when(dossieProdutoFachada.iniciarOuAvancarWorkflowDossieProduto(124L))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha workflow")));

        Response response = dossieProdutoResource.iniciarOuAvancarWorkflowDossieProduto(123L)
                .await().indefinitely();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(123L, ((DossieProdutoCriadoDto) response.getEntity()).id());
        assertThrows(IllegalStateException.class,
                () -> dossieProdutoResource.iniciarOuAvancarWorkflowDossieProduto(124L)
                        .await().indefinitely());
    }

    @Test
    void gestaoDocumentoResourceCobreCredencialContainerSucessoEFalhaDoBeanCdi() {
        when(gestaoDocumentoFachada.gerarCredencialContainer())
                .thenReturn(Uni.createFrom().item(new GestaoDocumentoCredencialContainerVo(
                        "sas-sensivel",
                        "10/07/2026 18:00:00",
                        "https://storage.example",
                        "pre-validacao"
                )))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha gestao documento")));

        Response response = gestaoDocumentoResource.gerarCredencialContainer()
                .await().indefinitely();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        GestaoDocumentoCredencialContainerDto entity = (GestaoDocumentoCredencialContainerDto) response.getEntity();
        assertEquals("pre-validacao", entity.nomeContainer());
        assertEquals("https://storage.example", entity.urlStorage());
        assertThrows(IllegalStateException.class,
                () -> gestaoDocumentoResource.gerarCredencialContainer().await().indefinitely());
    }
}
