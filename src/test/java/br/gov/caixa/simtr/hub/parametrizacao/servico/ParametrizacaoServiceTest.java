package br.gov.caixa.simtr.hub.parametrizacao.servico;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoChecklistGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.mock.ChecklistMockFactory;
import br.gov.caixa.simtr.hub.parametrizacao.mapeamento.ChecklistMapper;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ParametrizacaoServiceTest {

    @Inject
    ChecklistMapper checklistMapper;

    @Test
    void checklistComSimuladorHabilitadoUsaMockFactory() {
        FakeChecklistGateway gateway = new FakeChecklistGateway();
        FakeChecklistMockFactory mockFactory = new FakeChecklistMockFactory();
        ChecklistService service = new ChecklistService(gateway, mockFactory, checklistMapper, true);

        var resposta = service.consultarPorIdentificadorNegocialEVersao(100L, 1).await().indefinitely();

        assertEquals(TestFixtures.checklistDto().nome(), resposta.nome());
        assertTrue(mockFactory.chamado);
        assertFalse(gateway.chamado);
    }

    @Test
    void checklistComSimuladorDesabilitadoUsaGateway() {
        FakeChecklistGateway gateway = new FakeChecklistGateway();
        FakeChecklistMockFactory mockFactory = new FakeChecklistMockFactory();
        ChecklistService service = new ChecklistService(gateway, mockFactory, checklistMapper, false);

        var resposta = service.consultarPorIdentificadorNegocialEVersao(100L, 1).await().indefinitely();

        assertEquals(TestFixtures.checklistDto().nome(), resposta.nome());
        assertTrue(gateway.chamado);
        assertFalse(mockFactory.chamado);
    }

    private static class FakeChecklistGateway extends ParametrizacaoChecklistGateway {
        private boolean chamado;

        private FakeChecklistGateway() {
            super(null);
        }

        @Override
        public Uni<ChecklistDto> consultarPorIdentificadorNegocialEVersao(Long identificador, Integer versao) {
            chamado = true;
            return Uni.createFrom().item(TestFixtures.checklistDto());
        }
    }

    private static class FakeChecklistMockFactory extends ChecklistMockFactory {
        private boolean chamado;

        private FakeChecklistMockFactory() {
            super(null);
        }

        @Override
        public ChecklistDto criarChecklistMock(Long identificador, Integer versao) {
            chamado = true;
            return TestFixtures.checklistDto();
        }
    }
}
