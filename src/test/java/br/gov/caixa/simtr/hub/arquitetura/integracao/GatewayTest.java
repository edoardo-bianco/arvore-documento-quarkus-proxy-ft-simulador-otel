package br.gov.caixa.simtr.hub.arquitetura.integracao;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.DossieProdutoClient;
import br.gov.caixa.simtr.hub.dossieproduto.integracao.DossieProdutoGateway;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoClient;
import br.gov.caixa.simtr.hub.gestaodocumento.integracao.GestaoDocumentoGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoChecklistClient;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoChecklistGateway;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoProcessoClient;
import br.gov.caixa.simtr.hub.parametrizacao.integracao.ParametrizacaoProcessoGateway;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class GatewayTest {

    @Test
    void dossieGatewayEncaminhaCriacaoFormularioDocumentoValidacaoNegocialEWorkflowParaClient() {
        FakeDossieProdutoClient client = new FakeDossieProdutoClient();
        DossieProdutoGateway gateway = new DossieProdutoGateway(client);

        DossieProdutoCriadoDto criacao = gateway.criarDossieProduto(TestFixtures.dossieCriacaoDto())
                .await().indefinitely();
        DossieProdutoCriadoDto formulario = gateway.atualizarFormularioDossieProduto(
                        123L,
                        formularioComItensNulos()
                )
                .await().indefinitely();
        DossieProdutoDocumentoCriadoDto documento = gateway.incluirDocumentoDossieProduto(
                        321L,
                        TestFixtures.documentoInclusaoDto()
                )
                .await().indefinitely();
        gateway.registrarValidacaoNegocialDossieProduto(
                        432L,
                        TestFixtures.validacaoNegocialDto()
                )
                .await().indefinitely();
        DossieProdutoCriadoDto workflow = gateway.iniciarOuAvancarWorkflowDossieProduto(654L)
                .await().indefinitely();

        assertEquals(99L, criacao.id());
        assertEquals(123L, formulario.id());
        assertEquals(456L, documento.idDocumento());
        assertEquals(789L, documento.idInstanciaDocumento());
        assertEquals(654L, workflow.id());
        assertEquals(TestFixtures.dossieCriacaoDto().processo(), client.criacaoRecebida.processo());
        assertEquals(123L, client.idFormularioRecebido);
        assertEquals(3, client.formularioRecebido.size());
        assertEquals(321L, client.idDocumentoRecebido);
        assertEquals("RG", client.documentoRecebido.tipoDocumento());
        assertEquals(432L, client.idValidacaoNegocialRecebido);
        assertEquals(6592L, client.validacaoNegocialRecebida.verificacoes().getFirst().identificadorChecklist());
        assertEquals(654L, client.idWorkflowRecebido);
    }

    @Test
    void dossieGatewayPropagaFalhaDoClient() {
        FakeDossieProdutoClient client = new FakeDossieProdutoClient();
        client.falha = new IllegalStateException("falha dossie");
        DossieProdutoGateway gateway = new DossieProdutoGateway(client);

        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.criarDossieProduto(null).await().indefinitely()));
        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.atualizarFormularioDossieProduto(123L, null).await().indefinitely()));
        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.incluirDocumentoDossieProduto(123L, null).await().indefinitely()));
        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.registrarValidacaoNegocialDossieProduto(123L, null).await().indefinitely()));
        assertSame(client.falha, assertThrows(IllegalStateException.class,
                () -> gateway.iniciarOuAvancarWorkflowDossieProduto(123L).await().indefinitely()));
    }

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

    private static List<DossieProdutoFormularioDto> formularioComItensNulos() {
        List<DossieProdutoFormularioDto> formulario = new ArrayList<>();
        formulario.add(null);
        formulario.add(new DossieProdutoFormularioDto(null));
        formulario.add(TestFixtures.formularioDto().getFirst());
        return formulario;
    }

    static class FakeDossieProdutoClient implements DossieProdutoClient {
        private DossieProdutoCriacaoDto criacaoRecebida;
        private Long idFormularioRecebido;
        private List<DossieProdutoFormularioDto> formularioRecebido;
        private Long idDocumentoRecebido;
        private DossieProdutoDocumentoInclusaoDto documentoRecebido;
        private Long idValidacaoNegocialRecebido;
        private DossieProdutoValidacaoNegocialDto validacaoNegocialRecebida;
        private Long idWorkflowRecebido;
        private RuntimeException falha;

        @Override
        public Uni<DossieProdutoCriadoDto> criarDossieProduto(DossieProdutoCriacaoDto requisicao) {
            criacaoRecebida = requisicao;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(new DossieProdutoCriadoDto(99L));
        }

        @Override
        public Uni<DossieProdutoCriadoDto> atualizarFormularioDossieProduto(
                Long id,
                List<DossieProdutoFormularioDto> requisicao
        ) {
            idFormularioRecebido = id;
            formularioRecebido = requisicao;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(new DossieProdutoCriadoDto(id));
        }

        @Override
        public Uni<DossieProdutoDocumentoCriadoDto> incluirDocumentoDossieProduto(
                Long id,
                DossieProdutoDocumentoInclusaoDto requisicao
        ) {
            idDocumentoRecebido = id;
            documentoRecebido = requisicao;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(new DossieProdutoDocumentoCriadoDto(456L, 789L));
        }

        @Override
        public Uni<Void> registrarValidacaoNegocialDossieProduto(
                Long id,
                DossieProdutoValidacaoNegocialDto requisicao
        ) {
            idValidacaoNegocialRecebido = id;
            validacaoNegocialRecebida = requisicao;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<DossieProdutoCriadoDto> iniciarOuAvancarWorkflowDossieProduto(Long id) {
            idWorkflowRecebido = id;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(new DossieProdutoCriadoDto(id));
        }
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
