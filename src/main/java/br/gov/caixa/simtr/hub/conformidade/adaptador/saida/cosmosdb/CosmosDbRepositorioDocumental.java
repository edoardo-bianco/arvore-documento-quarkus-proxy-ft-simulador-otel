package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumental;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import reactor.core.publisher.Mono;

final class CosmosDbRepositorioDocumental implements RepositorioDocumental {

    private static final String CAMPO_CORRELATION_ID = "correlationId";
    private static final String CAMPO_ETAG = "_etag";
    private final CosmosAsyncContainer container;

    CosmosDbRepositorioDocumental(CosmosAsyncContainer container) {
        this.container = java.util.Objects.requireNonNull(container, "container");
    }

    @Override
    public Uni<ResultadoGravacao> criar(
            String documentId,
            ObjectNode documento) {
        return Uni.createFrom().deferred(() -> {
            validarDocumento(documentId, documento);
            var partitionKey = new PartitionKey(texto(documento, CAMPO_CORRELATION_ID));
            return gravar(container.createItem(
                    documento,
                    partitionKey,
                    new CosmosItemRequestOptions()));
        });
    }

    @Override
    public Uni<ResultadoGravacao> substituir(
            String documentId,
            String versaoEsperada,
            ObjectNode documento) {
        return Uni.createFrom().deferred(() -> {
            validarDocumento(documentId, documento);
            var partitionKey = new PartitionKey(texto(documento, CAMPO_CORRELATION_ID));
            var opcoes = new CosmosItemRequestOptions()
                    .setIfMatchETag(validarVersao(versaoEsperada));
            return gravar(container.replaceItem(
                    documento,
                    documentId,
                    partitionKey,
                    opcoes));
        });
    }

    @Override
    public Uni<Optional<DocumentoPersistido>> consultar(
            String documentId,
            String correlationId) {
        if (correlationId == null) {
            return consultarPorId(documentId);
        }
        return Uni.createFrom().deferred(() -> uni(container.readItem(
                                documentId,
                                new PartitionKey(correlationId),
                                ObjectNode.class))
                        .map(CosmosDbRepositorioDocumental::documentoDaResposta)
                        .map(Optional::of))
                .onFailure(CosmosDbRepositorioDocumental::naoEncontrado)
                .recoverWithItem(Optional.empty());
    }

    private Uni<Optional<DocumentoPersistido>> consultarPorId(String documentId) {
        return Uni.createFrom().deferred(() -> {
            var consulta = new SqlQuerySpec(
                    "SELECT * FROM c WHERE c.id = @id",
                    new SqlParameter("@id", documentId));
            return uni(container.queryItems(
                                    consulta,
                                    new CosmosQueryRequestOptions(),
                                    ObjectNode.class)
                            .next())
                    .map(documento -> documento == null
                            ? Optional.empty()
                            : Optional.of(documentoPersistido(documento, null)));
        });
    }

    private static Uni<ResultadoGravacao> gravar(Mono<?> operacao) {
        return uni(operacao)
                .map(ignorado -> ResultadoGravacao.GRAVADO)
                .onFailure(CosmosDbRepositorioDocumental::conflito)
                .recoverWithItem(ResultadoGravacao.CONFLITO);
    }

    private static <T> Uni<T> uni(Mono<T> mono) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(mono));
    }

    private static DocumentoPersistido documentoDaResposta(
            CosmosItemResponse<ObjectNode> resposta) {
        ObjectNode item = resposta.getItem();
        if (item == null) {
            throw new IllegalStateException("Resposta Cosmos sem item");
        }
        return documentoPersistido(item, resposta.getETag());
    }

    private static DocumentoPersistido documentoPersistido(
            ObjectNode item,
            String etagResposta) {
        String versao = etagResposta == null || etagResposta.isBlank()
                ? texto(item, CAMPO_ETAG)
                : etagResposta;
        ObjectNode documento = item.deepCopy();
        documento.remove(java.util.List.of(
                "_rid",
                CAMPO_ETAG,
                "_ts",
                "_self",
                "_attachments"));
        return new DocumentoPersistido(documento, versao);
    }

    private static boolean conflito(Throwable falha) {
        return falha instanceof CosmosException cosmos
                && (cosmos.getStatusCode() == 409
                        || cosmos.getStatusCode() == 412);
    }

    private static boolean naoEncontrado(Throwable falha) {
        return falha instanceof CosmosException cosmos
                && cosmos.getStatusCode() == 404;
    }

    private static void validarDocumento(
            String documentId,
            ObjectNode documento) {
        if (documentId == null
                || documentId.isBlank()
                || documento == null
                || !documentId.equals(texto(documento, "id"))) {
            throw new IllegalArgumentException("Documento Cosmos inválido");
        }
    }

    private static String validarVersao(String versao) {
        if (versao == null || versao.isBlank()) {
            throw new IllegalArgumentException("Versão Cosmos inválida");
        }
        return versao;
    }

    private static String texto(ObjectNode documento, String campo) {
        var valor = documento.path(campo);
        if (!valor.isTextual() || valor.textValue().isBlank()) {
            throw new IllegalArgumentException("Documento Cosmos inválido");
        }
        return valor.textValue();
    }
}
