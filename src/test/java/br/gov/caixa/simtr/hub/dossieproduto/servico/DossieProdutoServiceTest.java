package br.gov.caixa.simtr.hub.dossieproduto.servico;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.DossieProdutoGateway;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.mock.DossieProdutoMockFactory;
import br.gov.caixa.simtr.hub.dossieproduto.mapeamento.DossieProdutoMapper;
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

    private static class FakeDossieGateway extends DossieProdutoGateway {
        private boolean formularioChamada;
        private boolean documentoChamada;
        private boolean validacaoNegocialChamada;

        private FakeDossieGateway() {
            super(null);
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
    }

    private static class FakeDossieMockFactory extends DossieProdutoMockFactory {
        private boolean formularioChamada;
        private boolean documentoChamada;
        private boolean validacaoNegocialChamada;

        private FakeDossieMockFactory() {
            super(null);
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
    }
}
