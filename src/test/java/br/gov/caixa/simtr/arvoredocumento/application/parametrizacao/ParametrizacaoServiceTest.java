package br.gov.caixa.simtr.arvoredocumento.application.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.TestFixtures;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.ParametrizacaoChecklistGateway;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.ParametrizacaoProcessoGateway;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.mock.ChecklistMockFactory;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao.mock.ProcessoMockFactory;
import br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao.ChecklistMapper;
import br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao.ProcessoMapper;
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
    void processoComSimuladorHabilitadoUsaMockFactory() {
        FakeProcessoGateway gateway = new FakeProcessoGateway();
        FakeProcessoMockFactory mockFactory = new FakeProcessoMockFactory();
        ProcessoService service = new ProcessoService(gateway, mockFactory, new ProcessoMapper(), true);

        var resposta = service.consultarPorIdentificadorNegocial(100L).await().indefinitely();

        assertEquals(TestFixtures.processoDto().nome(), resposta.nome());
        assertTrue(mockFactory.chamado);
        assertFalse(gateway.chamado);
    }

    @Test
    void processoComSimuladorDesabilitadoUsaGateway() {
        FakeProcessoGateway gateway = new FakeProcessoGateway();
        FakeProcessoMockFactory mockFactory = new FakeProcessoMockFactory();
        ProcessoService service = new ProcessoService(gateway, mockFactory, new ProcessoMapper(), false);

        var resposta = service.consultarPorIdentificadorNegocial(100L).await().indefinitely();

        assertEquals(TestFixtures.processoDto().nome(), resposta.nome());
        assertTrue(gateway.chamado);
        assertFalse(mockFactory.chamado);
    }

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

    private static class FakeProcessoGateway extends ParametrizacaoProcessoGateway {
        private boolean chamado;

        private FakeProcessoGateway() {
            super(null);
        }

        @Override
        public Uni<ProcessoDto> consultarPorIdentificadorNegocial(Long identificador) {
            chamado = true;
            return Uni.createFrom().item(TestFixtures.processoDto());
        }
    }

    private static class FakeProcessoMockFactory extends ProcessoMockFactory {
        private boolean chamado;

        private FakeProcessoMockFactory() {
            super(null);
        }

        @Override
        public ProcessoDto criarProcessoMock(Long identificador) {
            chamado = true;
            return TestFixtures.processoDto();
        }
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
