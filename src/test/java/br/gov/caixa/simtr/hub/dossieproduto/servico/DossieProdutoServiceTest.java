package br.gov.caixa.simtr.hub.dossieproduto.servico;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.DossieProdutoGateway;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.mock.DossieProdutoMockFactory;
import br.gov.caixa.simtr.hub.dossieproduto.mapeamento.DossieProdutoMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoServiceTest {

    @Inject
    DossieProdutoMapper mapper;

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
        private boolean validacaoNegocialChamada;

        private FakeDossieGateway() {
            super(null);
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
        private boolean validacaoNegocialChamada;

        private FakeDossieMockFactory() {
            super(null);
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
