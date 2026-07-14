package br.gov.caixa.simtr.hub.arquitetura.integracao;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoClient;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoChecklistClient;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoChecklistGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoProcessoClient;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoProcessoGateway;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class GatewayTest {

    @Test
    void gestaoDocumentoGatewayEncaminhaCredencialContainerParaClient() {
        FakeGestaoDocumentoClient client = new FakeGestaoDocumentoClient();
        GestaoDocumentoGateway gateway = new GestaoDocumentoGateway(client);

        GestaoDocumentoCredencialContainerDto resposta = gateway.gerarCredencialContainer()
                .await().indefinitely();

        assertEquals("pre-validacao", resposta.nomeContainer());
        assertEquals("https://storage.example", resposta.urlStorage());
        assertEquals(1, client.credencialContainerChamadas);
    }

    @Test
    void gestaoDocumentoGatewayPropagaFalhaDoClient() {
        FakeGestaoDocumentoClient client = new FakeGestaoDocumentoClient();
        client.falha = new IllegalStateException("falha gestao documento");
        GestaoDocumentoGateway gateway = new GestaoDocumentoGateway(client);

        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.gerarCredencialContainer().await().indefinitely()));
    }

    @Test
    void processoGatewayEncaminhaConsultaParaClient() {
        FakeProcessoClient client = new FakeProcessoClient();
        ParametrizacaoProcessoGateway gateway = new ParametrizacaoProcessoGateway(client);

        ProcessoDto resposta = gateway.consultarPorIdentificadorNegocial(1000016487L)
                .await().indefinitely();

        assertEquals(TestFixtures.processoDto().nome(), resposta.nome());
        assertEquals(1000016487L, client.identificadorRecebido);
    }

    @Test
    void processoGatewayPropagaFalhaDoClient() {
        FakeProcessoClient client = new FakeProcessoClient();
        client.falha = new IllegalStateException("falha processo");
        ParametrizacaoProcessoGateway gateway = new ParametrizacaoProcessoGateway(client);

        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.consultarPorIdentificadorNegocial(100L).await().indefinitely()));
    }

    @Test
    void checklistGatewayEncaminhaConsultaParaClient() {
        FakeChecklistClient client = new FakeChecklistClient();
        ParametrizacaoChecklistGateway gateway = new ParametrizacaoChecklistGateway(client);

        ChecklistDto resposta = gateway.consultarPorIdentificadorNegocialEVersao(1000012583L, 1)
                .await().indefinitely();

        assertEquals(TestFixtures.checklistDto().nome(), resposta.nome());
        assertEquals(1000012583L, client.identificadorRecebido);
        assertEquals(1, client.versaoRecebida);
    }

    @Test
    void checklistGatewayPropagaFalhaDoClient() {
        FakeChecklistClient client = new FakeChecklistClient();
        client.falha = new IllegalStateException("falha checklist");
        ParametrizacaoChecklistGateway gateway = new ParametrizacaoChecklistGateway(client);

        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.consultarPorIdentificadorNegocialEVersao(100L, 1).await().indefinitely()));
    }

    static class FakeProcessoClient implements ParametrizacaoProcessoClient {
        private Long identificadorRecebido;
        private RuntimeException falha;

        @Override
        public Uni<ProcessoDto> consultarPorIdentificadorNegocial(Long identificador) {
            identificadorRecebido = identificador;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(TestFixtures.processoDto());
        }
    }

    static class FakeChecklistClient implements ParametrizacaoChecklistClient {
        private Long identificadorRecebido;
        private Integer versaoRecebida;
        private RuntimeException falha;

        @Override
        public Uni<ChecklistDto> consultarPorIdentificadorNegocialEVersao(Long identificador, Integer versao) {
            identificadorRecebido = identificador;
            versaoRecebida = versao;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(TestFixtures.checklistDto());
        }
    }

    static class FakeGestaoDocumentoClient implements GestaoDocumentoClient {
        private int credencialContainerChamadas;
        private RuntimeException falha;

        @Override
        public Uni<GestaoDocumentoCredencialContainerDto> gerarCredencialContainer() {
            credencialContainerChamadas++;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(new GestaoDocumentoCredencialContainerDto(
                    "sas",
                    "10/07/2026 18:00:00",
                    "https://storage.example",
                    "pre-validacao"
            ));
        }
    }
}
