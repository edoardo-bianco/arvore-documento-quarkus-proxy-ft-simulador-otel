package br.gov.caixa.simtr.hub.gestaodocumento.servico;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoGateway;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.mock.GestaoDocumentoMockFactory;
import br.gov.caixa.simtr.hub.gestaodocumento.mapeamento.GestaoDocumentoMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GestaoDocumentoServiceTest {

    @Inject
    GestaoDocumentoMapper mapper;

    @Test
    void credencialContainerComSimuladorHabilitadoUsaMockFactory() {
        FakeGestaoDocumentoGateway gateway = new FakeGestaoDocumentoGateway();
        FakeGestaoDocumentoMockFactory mockFactory = new FakeGestaoDocumentoMockFactory();
        GestaoDocumentoService service = new GestaoDocumentoService(gateway, mockFactory, mapper, true);

        var resposta = service.gerarCredencialContainer()
                .await().indefinitely();

        assertEquals("pre-validacao", resposta.nomeContainer());
        assertTrue(mockFactory.credencialContainerChamada);
        assertFalse(gateway.credencialContainerChamada);
    }

    @Test
    void credencialContainerComSimuladorDesabilitadoUsaGateway() {
        FakeGestaoDocumentoGateway gateway = new FakeGestaoDocumentoGateway();
        FakeGestaoDocumentoMockFactory mockFactory = new FakeGestaoDocumentoMockFactory();
        GestaoDocumentoService service = new GestaoDocumentoService(gateway, mockFactory, mapper, false);

        var resposta = service.gerarCredencialContainer()
                .await().indefinitely();

        assertEquals("container-gateway", resposta.nomeContainer());
        assertTrue(gateway.credencialContainerChamada);
        assertFalse(mockFactory.credencialContainerChamada);
    }

    @Test
    void credencialContainerPropagaFalhaDaOrigemSelecionada() {
        FakeGestaoDocumentoGateway gateway = new FakeGestaoDocumentoGateway();
        FakeGestaoDocumentoMockFactory mockFactory = new FakeGestaoDocumentoMockFactory();

        mockFactory.falha = new IllegalStateException("falha mock gestao documento");
        GestaoDocumentoService serviceComMock = new GestaoDocumentoService(gateway, mockFactory, mapper, true);
        assertSame(mockFactory.falha, assertThrows(IllegalStateException.class,
                () -> serviceComMock.gerarCredencialContainer().await().indefinitely()));

        gateway.falha = new IllegalStateException("falha gateway gestao documento");
        GestaoDocumentoService serviceComGateway = new GestaoDocumentoService(gateway, mockFactory, mapper, false);
        assertSame(gateway.falha, assertThrows(IllegalStateException.class,
                () -> serviceComGateway.gerarCredencialContainer().await().indefinitely()));
    }

    private static class FakeGestaoDocumentoGateway extends GestaoDocumentoGateway {
        private boolean credencialContainerChamada;
        private RuntimeException falha;

        private FakeGestaoDocumentoGateway() {
            super(null);
        }

        @Override
        public Uni<GestaoDocumentoCredencialContainerDto> gerarCredencialContainer() {
            credencialContainerChamada = true;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(new GestaoDocumentoCredencialContainerDto(
                    "sas-gateway",
                    "10/07/2026 18:00:00",
                    "https://storage-gateway.example",
                    "container-gateway"
            ));
        }
    }

    private static class FakeGestaoDocumentoMockFactory extends GestaoDocumentoMockFactory {
        private boolean credencialContainerChamada;
        private RuntimeException falha;

        private FakeGestaoDocumentoMockFactory() {
            super(null);
        }

        @Override
        public GestaoDocumentoCredencialContainerDto gerarCredencialContainerMock() {
            credencialContainerChamada = true;
            if (falha != null) {
                throw falha;
            }
            return TestFixtures.gestaoDocumentoCredencialContainerDto();
        }
    }
}
