package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumental;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

@QuarkusTest
class CosmosDbRepositorioDocumentalTest {

    private static final String DOCUMENT_ID = "documento-1";
    private static final String CORRELATION_ID = "correlation-1";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CosmosAsyncContainer container = mock(CosmosAsyncContainer.class);
    private final CosmosDbRepositorioDocumental repositorio =
            new CosmosDbRepositorioDocumental(container);

    @Test
    void criaItemNaParticaoDaCorrelacao() {
        ObjectNode documento = documento();
        CosmosItemResponse<ObjectNode> resposta = resposta(documento, "etag-1");
        when(container.createItem(
                        eq(documento),
                        eq(new PartitionKey(CORRELATION_ID)),
                        any(CosmosItemRequestOptions.class)))
                .thenReturn(Mono.just(resposta));

        var resultado = repositorio.criar(DOCUMENT_ID, documento)
                .await()
                .indefinitely();

        assertEquals(RepositorioDocumental.ResultadoGravacao.GRAVADO, resultado);
    }

    @Test
    void traduzConflitosDeCriacaoESubstituicao() {
        ObjectNode documento = documento();
        CosmosException conflitoCriacao = falhaCosmos(409);
        CosmosException conflitoSubstituicao = falhaCosmos(412);
        when(container.createItem(
                        eq(documento),
                        eq(new PartitionKey(CORRELATION_ID)),
                        any(CosmosItemRequestOptions.class)))
                .thenReturn(Mono.error(conflitoCriacao));
        when(container.replaceItem(
                        eq(documento),
                        eq(DOCUMENT_ID),
                        eq(new PartitionKey(CORRELATION_ID)),
                        any(CosmosItemRequestOptions.class)))
                .thenReturn(Mono.error(conflitoSubstituicao));

        assertEquals(
                RepositorioDocumental.ResultadoGravacao.CONFLITO,
                repositorio.criar(DOCUMENT_ID, documento).await().indefinitely());
        assertEquals(
                RepositorioDocumental.ResultadoGravacao.CONFLITO,
                repositorio.substituir(DOCUMENT_ID, "etag-antigo", documento)
                        .await()
                        .indefinitely());
    }

    @Test
    void substituiItemSomenteComEtagAtual() {
        ObjectNode documento = documento();
        CosmosItemResponse<ObjectNode> resposta = resposta(
                documento,
                "etag-novo");
        when(container.replaceItem(
                        eq(documento),
                        eq(DOCUMENT_ID),
                        eq(new PartitionKey(CORRELATION_ID)),
                        any(CosmosItemRequestOptions.class)))
                .thenReturn(Mono.just(resposta));

        var resultado = repositorio.substituir(
                        DOCUMENT_ID,
                        "etag-atual",
                        documento)
                .await()
                .indefinitely();

        var opcoes = ArgumentCaptor.forClass(CosmosItemRequestOptions.class);
        verify(container).replaceItem(
                eq(documento),
                eq(DOCUMENT_ID),
                eq(new PartitionKey(CORRELATION_ID)),
                opcoes.capture());
        assertEquals("etag-atual", opcoes.getValue().getIfMatchETag());
        assertEquals(RepositorioDocumental.ResultadoGravacao.GRAVADO, resultado);
    }

    @Test
    void fazLeituraPontualQuandoConheceACorrelacao() {
        ObjectNode documento = documento();
        CosmosItemResponse<ObjectNode> resposta = resposta(
                documento,
                "etag-lido");
        when(container.readItem(
                        DOCUMENT_ID,
                        new PartitionKey(CORRELATION_ID),
                        ObjectNode.class))
                .thenReturn(Mono.just(resposta));

        var encontrado = repositorio.consultar(DOCUMENT_ID, CORRELATION_ID)
                .await()
                .indefinitely()
                .orElseThrow();

        assertEquals("etag-lido", encontrado.versao());
        assertEquals(DOCUMENT_ID, encontrado.documento().path("id").asText());
        assertTrue(encontrado.documento().path("_etag").isMissingNode());
    }

    @Test
    void consultaProjecaoPorIdQuandoAindaNaoConheceACorrelacao() {
        ObjectNode documento = documento().put("_etag", "etag-consulta");
        @SuppressWarnings("unchecked")
        CosmosPagedFlux<ObjectNode> consulta = mock(CosmosPagedFlux.class);
        when(container.queryItems(
                        any(SqlQuerySpec.class),
                        any(CosmosQueryRequestOptions.class),
                        eq(ObjectNode.class)))
                .thenReturn(consulta);
        when(consulta.next()).thenReturn(Mono.just(documento));

        var encontrado = repositorio.consultar(DOCUMENT_ID, null)
                .await()
                .indefinitely()
                .orElseThrow();

        var especificacao = ArgumentCaptor.forClass(SqlQuerySpec.class);
        verify(container).queryItems(
                especificacao.capture(),
                any(CosmosQueryRequestOptions.class),
                eq(ObjectNode.class));
        assertEquals("SELECT * FROM c WHERE c.id = @id", especificacao.getValue()
                .getQueryText());
        assertEquals(DOCUMENT_ID, especificacao.getValue()
                .getParameters()
                .getFirst()
                .getValue(String.class));
        assertEquals("etag-consulta", encontrado.versao());
        assertEquals(documento(), encontrado.documento());
    }

    @Test
    void ausenciaRetornaOptionalVazio() {
        CosmosException ausencia = falhaCosmos(404);
        when(container.readItem(
                        DOCUMENT_ID,
                        new PartitionKey(CORRELATION_ID),
                        ObjectNode.class))
                .thenReturn(Mono.error(ausencia));

        var resultado = repositorio.consultar(DOCUMENT_ID, CORRELATION_ID)
                .await()
                .indefinitely();

        assertTrue(resultado.isEmpty());
    }

    private ObjectNode documento() {
        return objectMapper.createObjectNode()
                .put("id", DOCUMENT_ID)
                .put("correlationId", CORRELATION_ID);
    }

    @SuppressWarnings("unchecked")
    private static CosmosItemResponse<ObjectNode> resposta(
            ObjectNode documento,
            String etag) {
        CosmosItemResponse<ObjectNode> resposta = mock(CosmosItemResponse.class);
        when(resposta.getItem()).thenReturn(documento);
        when(resposta.getETag()).thenReturn(etag);
        return resposta;
    }

    private static CosmosException falhaCosmos(int status) {
        CosmosException falha = mock(CosmosException.class);
        when(falha.getStatusCode()).thenReturn(status);
        return falha;
    }
}
