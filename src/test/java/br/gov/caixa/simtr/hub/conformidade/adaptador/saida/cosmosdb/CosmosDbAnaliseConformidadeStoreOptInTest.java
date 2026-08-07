package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.IdsDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EmissaoReferencialAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.TipoEmissaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Tag("cosmos-opt-in")
@EnabledIfEnvironmentVariable(named = "COSMOS_INTEGRATION_ENABLED", matches = "(?i)true")
class CosmosDbAnaliseConformidadeStoreOptInTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static CosmosAsyncClient client;
    private static com.azure.cosmos.CosmosAsyncContainer container;

    @BeforeAll
    static void conectar() {
        String endpoint = variavelObrigatoria("COSMOS_ENDPOINT");
        String database = variavelObrigatoria("COSMOS_DATABASE");
        String containerName = variavelObrigatoria("COSMOS_CONTAINER");
        client = new CosmosDbClienteFactory().criar(endpoint);
        container = client.getDatabase(database).getContainer(containerName);
    }

    @AfterAll
    static void fechar() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    @Timeout(120)
    void executaSequenciaReferencialContraCosmosReal() {
        String sufixo = UUID.randomUUID().toString();
        String correlationId = "cosmos-opt-in-" + sufixo;
        String instanceId = "instance-cosmos-opt-in-" + sufixo;
        var store = new CosmosDbAnaliseConformidadeStore(container, OBJECT_MAPPER);
        var referencias = new ReferenciasDocumentoAnaliseConformidade(OBJECT_MAPPER);
        var preliminar = resultado(OrigemResultado.AGENTE);
        var finalizado = resultado(OrigemResultado.REVISAO_HUMANA);
        var referenciaPreliminar = referencias.resultadoPreliminar(
                correlationId, preliminar);
        var referenciaFinal = referencias.resultadoFinal(correlationId, finalizado);
        String eventoPreliminar = "conformidade:" + referenciaPreliminar.documentoRef();
        String eventoFinal = "conformidade:" + referenciaFinal.documentoRef();
        List<String> documentos = List.of(
                IdsDocumentoAnaliseConformidade.emissao(eventoFinal),
                IdsDocumentoAnaliseConformidade.resultadoFinal(correlationId),
                IdsDocumentoAnaliseConformidade.revisao(correlationId),
                IdsDocumentoAnaliseConformidade.emissao(eventoPreliminar),
                IdsDocumentoAnaliseConformidade.resultadoPreliminar(correlationId),
                IdsDocumentoAnaliseConformidade.checklist(correlationId),
                IdsDocumentoAnaliseConformidade.projecao(instanceId),
                IdsDocumentoAnaliseConformidade.entrada(correlationId));

        try {
            aguardar(store.iniciar(
                    instanceId,
                    new SolicitacaoAnaliseConformidade(
                            correlationId,
                            "DOC-COSMOS-OPT-IN",
                            "Texto sintético do teste opt-in",
                            1000012583L,
                            1)));
            aguardar(store.registrarChecklist(instanceId, checklist()));
            aguardar(store.prepararResultadoPreliminar(
                    instanceId, preliminar, referenciaPreliminar));
            aguardar(store.registrarEmissao(emissao(
                    eventoPreliminar,
                    TipoEmissaoAnaliseConformidade.REVISAO_SOLICITADA,
                    instanceId,
                    correlationId,
                    referenciaPreliminar)));
            aguardar(store.reservarRevisao(
                    instanceId,
                    new RevisaoHumanaConformidade(
                            "Revisão sintética",
                            finalizado.apontamentos())));
            aguardar(store.prepararResultadoFinal(
                    instanceId, finalizado, referenciaFinal));
            aguardar(store.registrarEmissao(emissao(
                    eventoFinal,
                    TipoEmissaoAnaliseConformidade.ANALISE_CONCLUIDA,
                    instanceId,
                    correlationId,
                    referenciaFinal)));

            var concluida = aguardar(store.consultar(instanceId)).orElseThrow();
            assertEquals(StatusAnaliseConformidade.CONCLUIDA, concluida.status());
            assertEquals(finalizado, concluida.resultadoFinal());
        } finally {
            removerDocumentos(documentos, correlationId);
        }
    }

    private static EmissaoReferencialAnaliseConformidade emissao(
            String id,
            TipoEmissaoAnaliseConformidade tipo,
            String instanceId,
            String correlationId,
            br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida
                    .ReferenciaDocumentoAnaliseConformidade referencia) {
        return new EmissaoReferencialAnaliseConformidade(
                id,
                URI.create("urn:simtr-hub:conformidade"),
                tipo,
                OffsetDateTime.now(),
                instanceId,
                correlationId,
                null,
                referencia);
    }

    private static Checklist checklist() {
        return new Checklist(
                "Checklist Cosmos opt-in",
                1000012583L,
                1,
                "2026-08-02T00:00:00Z",
                "2026-08-02T00:00:00Z",
                false,
                "Orientação sintética",
                List.of(new ApontamentoChecklist(
                        1L,
                        "Apontamento sintético",
                        "Descrição sintética",
                        "Orientação sintética",
                        false,
                        1)));
    }

    private static ResultadoAnaliseConformidade resultado(OrigemResultado origem) {
        return new ResultadoAnaliseConformidade(
                1000012583L,
                1,
                "Checklist Cosmos opt-in",
                "Resumo sintético",
                List.of(new ResultadoApontamentoConformidade(
                        1L,
                        "Apontamento sintético",
                        ParecerConformidade.CONFORME,
                        "Justificativa sintética",
                        "Evidência sintética",
                        0.9d)),
                origem);
    }

    private static void removerDocumentos(List<String> documentos, String correlationId) {
        for (String documento : documentos) {
            try {
                container.deleteItem(documento, new PartitionKey(correlationId))
                        .block(Duration.ofSeconds(20));
            } catch (CosmosException falha) {
                if (falha.getStatusCode() != 404) {
                    throw falha;
                }
            }
        }
    }

    private static String variavelObrigatoria(String nome) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Variável obrigatória ausente: " + nome);
        }
        return valor;
    }

    private static <T> T aguardar(io.smallrye.mutiny.Uni<T> operacao) {
        return operacao.await().atMost(Duration.ofSeconds(30));
    }
}
