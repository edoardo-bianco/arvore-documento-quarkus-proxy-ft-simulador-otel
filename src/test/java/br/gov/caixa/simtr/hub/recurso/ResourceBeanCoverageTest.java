package br.gov.caixa.simtr.hub.recurso;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.ProcessoResource;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.entrada.ConsultarProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.ChecklistResource;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.DossieProdutoResource;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.CriacaoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.CriacaoDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.InclusaoDocumentoDossieProdutoResponse;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.entrada.rest.v1.GestaoDocumentoResource;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.entrada.rest.v1.dto.GestaoDocumentoCredencialContainerResponse;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.entrada.ObterCredencialContainer;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IniciarOuAvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CriarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.AtualizarFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IncluirDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.RegistrarValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMock
    ConsultarProcessoParametrizado consultarProcessoParametrizado;

    @InjectMock
    ConsultarChecklist consultarChecklist;

    @InjectMock
    CriarDossieProduto criarDossieProduto;

    @InjectMock
    AtualizarFormularioDossieProduto atualizarFormularioDossieProduto;

    @InjectMock
    IncluirDocumentoDossieProduto incluirDocumentoDossieProduto;

    @InjectMock
    IniciarOuAvancarWorkflowDossieProduto iniciarOuAvancarWorkflow;

    @InjectMock
    RegistrarValidacaoNegocialDossieProduto registrarValidacaoNegocial;

    @InjectMock
    ObterCredencialContainer obterCredencialContainer;

    @Test
    void processoResourceCobreSucessoEFalhaDoBeanCdi() {
        when(consultarProcessoParametrizado.executar(new IdentificadorNegocialProcesso(1L)))
                .thenReturn(Uni.createFrom().item(new ProcessoParametrizado(
                        1L, "Processo", true, null, false,
                        null, null, null, null, null, null)));
        when(consultarProcessoParametrizado.executar(new IdentificadorNegocialProcesso(2L)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha processo")));

        var resposta = processoResource.consultarPorIdentificadorNegocial(1L)
                .await().indefinitely();

        assertEquals("Processo", resposta.nome());
        assertThrows(IllegalStateException.class, () -> processoResource.consultarPorIdentificadorNegocial(2L)
                .await().indefinitely());
    }

    @Test
    void checklistResourceCobreSucessoEFalhaDoBeanCdi() {
        when(consultarChecklist.executar(new ComandoConsultaChecklist(1L, 1)))
                .thenReturn(Uni.createFrom().item(new Checklist(
                        "Checklist", 1L, 1, null, null, false, null, null)));
        when(consultarChecklist.executar(new ComandoConsultaChecklist(2L, 1)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha checklist")));

        var resposta = checklistResource.consultarPorIdentificadorNegocialEVersao(1L, 1)
                .await().indefinitely();

        assertEquals("Checklist", resposta.nome());
        assertThrows(IllegalStateException.class, () -> checklistResource.consultarPorIdentificadorNegocialEVersao(2L, 1)
                .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobrePostSucessoEFalhaDoBeanCdi() {
        when(criarDossieProduto.executar(any()))
                .thenReturn(Uni.createFrom().item(new ResultadoCriacaoDossieProduto(77L)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha dossie")));

        var fixture = TestFixtures.criacaoDossieProdutoRequest();
        var request = new CriacaoDossieProdutoRequest(
                fixture.processo(), fixture.chaveCorrelacaoCanal(), fixture.numeroNegocio(), fixture.clientes());

        Response response = dossieProdutoResource.criarDossieProduto(request)
                .await().indefinitely();

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(77L, ((CriacaoDossieProdutoResponse) response.getEntity()).id());
        assertThrows(IllegalStateException.class, () -> dossieProdutoResource.criarDossieProduto(request)
                .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobrePatchSucessoEFalhaDoBeanCdi() {
        when(atualizarFormularioDossieProduto.executar(any()))
                .thenReturn(Uni.createFrom().item(
                        new ResultadoAtualizacaoFormularioDossieProduto(123L)))
                .thenReturn(Uni.createFrom().failure(
                        new IllegalStateException("falha formulario")));

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
        when(incluirDocumentoDossieProduto.executar(any()))
                .thenReturn(Uni.createFrom().item(
                        new ResultadoInclusaoDocumentoDossieProduto(456L, 789L)))
                .thenReturn(Uni.createFrom().failure(
                        new IllegalStateException("falha documento")));

        Response response = dossieProdutoResource.incluirDocumentoDossieProduto(123L, TestFixtures.documentoInclusaoDto())
                .await().indefinitely();

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(456L, ((InclusaoDocumentoDossieProdutoResponse)
                response.getEntity()).idDocumento());
        assertEquals(789L, ((InclusaoDocumentoDossieProdutoResponse)
                response.getEntity()).idInstanciaDocumento());
        assertThrows(IllegalStateException.class,
                () -> dossieProdutoResource.incluirDocumentoDossieProduto(124L, TestFixtures.documentoInclusaoDto())
                        .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobreValidacaoNegocialSucessoEFalhaDoBeanCdi() {
        when(registrarValidacaoNegocial.executar(any()))
                .thenReturn(Uni.createFrom().voidItem())
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha validacao negocial")));

        Response response = dossieProdutoResource.registrarValidacaoNegocialDossieProduto(
                        123L,
                        TestFixtures.validacaoNegocialRequest()
                )
                .await().indefinitely();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(null, response.getEntity());
        assertThrows(IllegalStateException.class,
                () -> dossieProdutoResource.registrarValidacaoNegocialDossieProduto(
                                124L,
                                TestFixtures.validacaoNegocialRequest()
                        )
                        .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobreWorkflowSucessoEFalhaDoBeanCdi() {
        when(iniciarOuAvancarWorkflow.executar(new IdentificadorDossieProduto(123L)))
                .thenReturn(Uni.createFrom().item(new ResultadoWorkflowDossieProduto(123L)));
        when(iniciarOuAvancarWorkflow.executar(new IdentificadorDossieProduto(124L)))
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
        when(obterCredencialContainer.executar())
                .thenReturn(Uni.createFrom().item(new CredencialContainer(
                        "sas-opaca-teste",
                        "10/07/2026 18:00:00",
                        "https://storage.example",
                        "pre-validacao"
                )))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha gestao documento")));

        Response response = gestaoDocumentoResource.gerarCredencialContainer()
                .await().indefinitely();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        GestaoDocumentoCredencialContainerResponse entity =
                (GestaoDocumentoCredencialContainerResponse) response.getEntity();
        assertEquals("pre-validacao", entity.nomeContainer());
        assertEquals("https://storage.example", entity.urlStorage());
        assertThrows(IllegalStateException.class,
                () -> gestaoDocumentoResource.gerarCredencialContainer().await().indefinitely());
    }
}
