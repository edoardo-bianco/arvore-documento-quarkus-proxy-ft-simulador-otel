package br.gov.caixa.simtr.hub.arquitetura.integracao;

import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoClient;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoGateway;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
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
