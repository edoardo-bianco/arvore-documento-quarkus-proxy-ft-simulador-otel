package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.mock;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.gestaodocumento.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto.mock.DossieProdutoMockFactory;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.gestaodocumento.mock.GestaoDocumentoMockFactory;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.mock.ChecklistMockFactory;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.mock.ProcessoMockFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class MockFactoryTest {

    private final MarkdownJsonMockReader reader = new MarkdownJsonMockReader(new ObjectMapper());

    @Test
    void deveLerMockDeProcessoComFallback() {
        ProcessoMockFactory factory = new ProcessoMockFactory(reader);

        ProcessoDto processo = factory.criarProcessoMock(999L);

        assertNotNull(processo);
        assertEquals(1000016487L, processo.identificadorNegocial());
    }

    @Test
    void deveLerMockDeChecklistComFallback() {
        ChecklistMockFactory factory = new ChecklistMockFactory(reader);

        ChecklistDto checklist = factory.criarChecklistMock(999L, 9);

        assertNotNull(checklist);
        assertEquals(1000012583L, checklist.identificadorNegocial());
        assertEquals(1, checklist.versao());
    }

    @Test
    void deveLerMockDeCriacaoDossieProduto() {
        DossieProdutoMockFactory factory = new DossieProdutoMockFactory(reader);

        DossieProdutoCriadoDto resposta = factory.criarDossieProdutoMock(null);

        assertEquals(1L, resposta.id());
    }

    @Test
    void deveLerMockDeFormularioDossieProdutoERetornarIdDoPath() {
        DossieProdutoMockFactory factory = new DossieProdutoMockFactory(reader);

        DossieProdutoCriadoDto resposta = factory.atualizarFormularioDossieProdutoMock(123L, null);

        assertEquals(123L, resposta.id());
    }

    @Test
    void deveLerMockDeDocumentoDossieProduto() {
        DossieProdutoMockFactory factory = new DossieProdutoMockFactory(reader);

        DossieProdutoDocumentoCriadoDto resposta = factory.incluirDocumentoDossieProdutoMock(123L, null);

        assertEquals(456L, resposta.idDocumento());
        assertEquals(789L, resposta.idInstanciaDocumento());
    }

    @Test
    void deveLerMockDeValidacaoNegocialDossieProduto() {
        DossieProdutoMockFactory factory = new DossieProdutoMockFactory(reader);

        factory.registrarValidacaoNegocialDossieProdutoMock(123L, null);
    }

    @Test
    void deveLerMockDeWorkflowDossieProdutoERetornarIdDoPath() {
        DossieProdutoMockFactory factory = new DossieProdutoMockFactory(reader);

        DossieProdutoCriadoDto resposta = factory.iniciarOuAvancarWorkflowDossieProdutoMock(123L);

        assertEquals(123L, resposta.id());
    }

    @Test
    void deveLerMockDeCredencialContainerGestaoDocumento() {
        GestaoDocumentoMockFactory factory = new GestaoDocumentoMockFactory(reader);

        GestaoDocumentoCredencialContainerDto resposta = factory.gerarCredencialContainerMock();

        assertNotNull(resposta.sas());
        assertEquals("10/07/2026 18:00:00", resposta.validade());
        assertEquals("https://dossiedigitaldes.blob.core.windows.net", resposta.urlStorage());
        assertEquals("pre-validacao", resposta.nomeContainer());
    }
}
