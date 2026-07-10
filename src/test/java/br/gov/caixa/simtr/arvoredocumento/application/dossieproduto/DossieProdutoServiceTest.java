package br.gov.caixa.simtr.arvoredocumento.application.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.TestFixtures;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto.DossieProdutoGateway;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto.mock.DossieProdutoMockFactory;
import br.gov.caixa.simtr.arvoredocumento.mapper.dossieproduto.DossieProdutoMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoServiceTest {

    @Inject
    DossieProdutoMapper mapper;

    @Test
    void criacaoComSimuladorHabilitadoUsaMockFactory() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, true);

        var resposta = service.criarDossieProduto(mapper.toVo(TestFixtures.dossieCriacaoDto()))
                .await().indefinitely();

        assertEquals(1L, resposta.id());
        assertTrue(mockFactory.criacaoChamada);
        assertFalse(gateway.criacaoChamada);
    }

    @Test
    void criacaoComSimuladorDesabilitadoUsaGateway() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, false);

        var resposta = service.criarDossieProduto(mapper.toVo(TestFixtures.dossieCriacaoDto()))
                .await().indefinitely();

        assertEquals(2L, resposta.id());
        assertTrue(gateway.criacaoChamada);
        assertFalse(mockFactory.criacaoChamada);
    }

    @Test
    void formularioComSimuladorHabilitadoUsaMockFactoryERetornaIdDoPath() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, true);

        var resposta = service.atualizarFormularioDossieProduto(
                        123L,
                        mapper.toFormularioVo(TestFixtures.formularioDto())
                )
                .await().indefinitely();

        assertEquals(123L, resposta.id());
        assertTrue(mockFactory.formularioChamada);
        assertFalse(gateway.formularioChamada);
    }

    @Test
    void formularioComSimuladorDesabilitadoUsaGateway() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, false);

        var resposta = service.atualizarFormularioDossieProduto(
                        123L,
                        mapper.toFormularioVo(TestFixtures.formularioDto())
                )
                .await().indefinitely();

        assertEquals(4L, resposta.id());
        assertTrue(gateway.formularioChamada);
        assertFalse(mockFactory.formularioChamada);
    }

    @Test
    void documentoComSimuladorHabilitadoUsaMockFactory() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, true);

        var resposta = service.incluirDocumentoDossieProduto(
                        123L,
                        mapper.toVo(TestFixtures.documentoInclusaoDto())
                )
                .await().indefinitely();

        assertEquals(5L, resposta.idDocumento());
        assertEquals(6L, resposta.idInstanciaDocumento());
        assertTrue(mockFactory.documentoChamada);
        assertFalse(gateway.documentoChamada);
    }

    @Test
    void documentoComSimuladorDesabilitadoUsaGateway() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, false);

        var resposta = service.incluirDocumentoDossieProduto(
                        123L,
                        mapper.toVo(TestFixtures.documentoInclusaoDto())
                )
                .await().indefinitely();

        assertEquals(7L, resposta.idDocumento());
        assertEquals(8L, resposta.idInstanciaDocumento());
        assertTrue(gateway.documentoChamada);
        assertFalse(mockFactory.documentoChamada);
    }

    @Test
    void validacaoNegocialComSimuladorHabilitadoUsaMockFactory() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, true);

        service.registrarValidacaoNegocialDossieProduto(
                        123L,
                        mapper.toVo(TestFixtures.validacaoNegocialDto())
                )
                .await().indefinitely();

        assertTrue(mockFactory.validacaoNegocialChamada);
        assertFalse(gateway.validacaoNegocialChamada);
    }

    @Test
    void validacaoNegocialComSimuladorDesabilitadoUsaGateway() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, false);

        service.registrarValidacaoNegocialDossieProduto(
                        123L,
                        mapper.toVo(TestFixtures.validacaoNegocialDto())
                )
                .await().indefinitely();

        assertTrue(gateway.validacaoNegocialChamada);
        assertFalse(mockFactory.validacaoNegocialChamada);
    }

    @Test
    void workflowComSimuladorHabilitadoUsaMockFactory() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, true);

        var resposta = service.iniciarOuAvancarWorkflowDossieProduto(123L)
                .await().indefinitely();

        assertEquals(123L, resposta.id());
        assertTrue(mockFactory.workflowChamada);
        assertFalse(gateway.workflowChamada);
    }

    @Test
    void workflowComSimuladorDesabilitadoUsaGateway() {
        FakeDossieGateway gateway = new FakeDossieGateway();
        FakeDossieMockFactory mockFactory = new FakeDossieMockFactory();
        DossieProdutoService service = new DossieProdutoService(gateway, mockFactory, mapper, false);

        var resposta = service.iniciarOuAvancarWorkflowDossieProduto(123L)
                .await().indefinitely();

        assertEquals(9L, resposta.id());
        assertTrue(gateway.workflowChamada);
        assertFalse(mockFactory.workflowChamada);
    }

    private static class FakeDossieGateway extends DossieProdutoGateway {
        private boolean criacaoChamada;
        private boolean formularioChamada;
        private boolean documentoChamada;
        private boolean validacaoNegocialChamada;
        private boolean workflowChamada;

        private FakeDossieGateway() {
            super(null);
        }

        @Override
        public Uni<DossieProdutoCriadoDto> criarDossieProduto(DossieProdutoCriacaoDto requisicao) {
            criacaoChamada = true;
            return Uni.createFrom().item(new DossieProdutoCriadoDto(2L));
        }

        @Override
        public Uni<DossieProdutoCriadoDto> atualizarFormularioDossieProduto(
                Long id,
                List<DossieProdutoFormularioDto> requisicao
        ) {
            formularioChamada = true;
            return Uni.createFrom().item(new DossieProdutoCriadoDto(4L));
        }

        @Override
        public Uni<DossieProdutoDocumentoCriadoDto> incluirDocumentoDossieProduto(
                Long id,
                DossieProdutoDocumentoInclusaoDto requisicao
        ) {
            documentoChamada = true;
            return Uni.createFrom().item(new DossieProdutoDocumentoCriadoDto(7L, 8L));
        }

        @Override
        public Uni<Void> registrarValidacaoNegocialDossieProduto(
                Long id,
                DossieProdutoValidacaoNegocialDto requisicao
        ) {
            validacaoNegocialChamada = true;
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<DossieProdutoCriadoDto> iniciarOuAvancarWorkflowDossieProduto(Long id) {
            workflowChamada = true;
            return Uni.createFrom().item(new DossieProdutoCriadoDto(9L));
        }
    }

    private static class FakeDossieMockFactory extends DossieProdutoMockFactory {
        private boolean criacaoChamada;
        private boolean formularioChamada;
        private boolean documentoChamada;
        private boolean validacaoNegocialChamada;
        private boolean workflowChamada;

        private FakeDossieMockFactory() {
            super(null);
        }

        @Override
        public DossieProdutoCriadoDto criarDossieProdutoMock(DossieProdutoCriacaoDto requisicao) {
            criacaoChamada = true;
            return new DossieProdutoCriadoDto(1L);
        }

        @Override
        public DossieProdutoCriadoDto atualizarFormularioDossieProdutoMock(
                Long id,
                List<DossieProdutoFormularioDto> requisicao
        ) {
            formularioChamada = true;
            return new DossieProdutoCriadoDto(id);
        }

        @Override
        public DossieProdutoDocumentoCriadoDto incluirDocumentoDossieProdutoMock(
                Long id,
                DossieProdutoDocumentoInclusaoDto requisicao
        ) {
            documentoChamada = true;
            return new DossieProdutoDocumentoCriadoDto(5L, 6L);
        }

        @Override
        public void registrarValidacaoNegocialDossieProdutoMock(
                Long id,
                DossieProdutoValidacaoNegocialDto requisicao
        ) {
            validacaoNegocialChamada = true;
        }

        @Override
        public DossieProdutoCriadoDto iniciarOuAvancarWorkflowDossieProdutoMock(Long id) {
            workflowChamada = true;
            return new DossieProdutoCriadoDto(id);
        }
    }
}
