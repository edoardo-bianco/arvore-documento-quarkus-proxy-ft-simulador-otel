package br.gov.caixa.simtr.arvoredocumento.api;

import br.gov.caixa.simtr.arvoredocumento.TestFixtures;
import br.gov.caixa.simtr.arvoredocumento.api.dossieproduto.DossieProdutoResource;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.parametrizacao.ChecklistResource;
import br.gov.caixa.simtr.arvoredocumento.api.parametrizacao.ProcessoResource;
import br.gov.caixa.simtr.arvoredocumento.application.dossieproduto.DossieProdutoService;
import br.gov.caixa.simtr.arvoredocumento.application.parametrizacao.ChecklistService;
import br.gov.caixa.simtr.arvoredocumento.application.parametrizacao.ProcessoService;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.arvoredocumento.mapper.dossieproduto.DossieProdutoMapper;
import br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao.ChecklistMapper;
import br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao.ProcessoMapper;
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
    ProcessoMapper processoMapper;

    @Inject
    ChecklistMapper checklistMapper;

    @Inject
    DossieProdutoMapper dossieProdutoMapper;

    @InjectMock
    ProcessoService processoService;

    @InjectMock
    ChecklistService checklistService;

    @InjectMock
    DossieProdutoService dossieProdutoService;

    @Test
    void processoResourceCobreSucessoEFalhaDoBeanCdi() {
        when(processoService.consultarPorIdentificadorNegocial(1L))
                .thenReturn(Uni.createFrom().item(processoMapper.toVo(TestFixtures.processoDto())));
        when(processoService.consultarPorIdentificadorNegocial(2L))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha processo")));

        var resposta = processoResource.consultarPorIdentificadorNegocial(1L)
                .await().indefinitely();

        assertEquals(TestFixtures.processoDto().nome(), resposta.nome());
        assertThrows(IllegalStateException.class, () -> processoResource.consultarPorIdentificadorNegocial(2L)
                .await().indefinitely());
    }

    @Test
    void checklistResourceCobreSucessoEFalhaDoBeanCdi() {
        when(checklistService.consultarPorIdentificadorNegocialEVersao(1L, 1))
                .thenReturn(Uni.createFrom().item(checklistMapper.toVo(TestFixtures.checklistDto())));
        when(checklistService.consultarPorIdentificadorNegocialEVersao(2L, 1))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha checklist")));

        var resposta = checklistResource.consultarPorIdentificadorNegocialEVersao(1L, 1)
                .await().indefinitely();

        assertEquals(TestFixtures.checklistDto().nome(), resposta.nome());
        assertThrows(IllegalStateException.class, () -> checklistResource.consultarPorIdentificadorNegocialEVersao(2L, 1)
                .await().indefinitely());
    }

    @Test
    void dossieProdutoResourceCobrePostSucessoEFalhaDoBeanCdi() {
        when(dossieProdutoService.criarDossieProduto(any()))
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
        when(dossieProdutoService.atualizarFormularioDossieProduto(eq(123L), any()))
                .thenReturn(Uni.createFrom().item(new DossieProdutoCriadoVo(123L)));
        when(dossieProdutoService.atualizarFormularioDossieProduto(eq(124L), any()))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha formulario")));

        Response response = dossieProdutoResource.atualizarFormularioDossieProduto(123L, TestFixtures.formularioDto())
                .await().indefinitely();

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(123L, ((DossieProdutoCriadoDto) response.getEntity()).id());
        assertThrows(IllegalStateException.class,
                () -> dossieProdutoResource.atualizarFormularioDossieProduto(124L, TestFixtures.formularioDto())
                        .await().indefinitely());
    }
}
