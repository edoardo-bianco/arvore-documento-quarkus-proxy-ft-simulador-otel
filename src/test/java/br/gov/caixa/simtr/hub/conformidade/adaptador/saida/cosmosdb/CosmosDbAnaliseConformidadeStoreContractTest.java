package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.contrato.ArmazenarEstadoAnaliseConformidadeContractTest;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumental;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class CosmosDbAnaliseConformidadeStoreContractTest
        extends ArmazenarEstadoAnaliseConformidadeContractTest {

    @Override
    protected ArmazenarEstadoAnaliseConformidade novoStore() {
        return new CosmosDbAnaliseConformidadeStore(
                new RepositorioCosmosFake(),
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private static final class RepositorioCosmosFake
            implements RepositorioDocumental {

        private final ConcurrentMap<String, ObjectNode> documentos =
                new ConcurrentHashMap<>();
        private final AtomicLong proximoEtag = new AtomicLong();

        @Override
        public Uni<ResultadoGravacao> criar(
                String documentId,
                ObjectNode documento) {
            return Uni.createFrom().item(() -> {
                ObjectNode novo = comEtag(documento);
                return documentos.putIfAbsent(chave(documentId, documento), novo) == null
                        ? ResultadoGravacao.GRAVADO
                        : ResultadoGravacao.CONFLITO;
            });
        }

        @Override
        public Uni<ResultadoGravacao> substituir(
                String documentId,
                String versaoEsperada,
                ObjectNode documento) {
            return Uni.createFrom().item(() -> {
                AtomicReference<ResultadoGravacao> resultado = new AtomicReference<>();
                documentos.compute(chave(documentId, documento), (chave, atual) -> {
                    if (atual == null
                            || !texto(atual, "_etag").equals(versaoEsperada)) {
                        resultado.set(ResultadoGravacao.CONFLITO);
                        return atual;
                    }
                    resultado.set(ResultadoGravacao.GRAVADO);
                    return comEtag(documento);
                });
                return resultado.get();
            });
        }

        @Override
        public Uni<Optional<DocumentoPersistido>> consultar(
                String documentId,
                String correlationId) {
            return Uni.createFrom().item(() -> {
                if (correlationId != null) {
                    return Optional.ofNullable(documentos.get(chave(
                            documentId,
                                    correlationId)))
                            .map(RepositorioCosmosFake::persistido);
                }
                return documentos.values().stream()
                        .filter(documento -> documentId.equals(texto(documento, "id")))
                        .findFirst()
                        .map(RepositorioCosmosFake::persistido);
            });
        }

        private ObjectNode comEtag(ObjectNode documento) {
            return documento.deepCopy()
                    .put("_etag", Long.toString(proximoEtag.incrementAndGet()));
        }

        private static DocumentoPersistido persistido(ObjectNode documento) {
            ObjectNode conteudo = documento.deepCopy();
            String etag = texto(conteudo, "_etag");
            conteudo.remove("_etag");
            return new DocumentoPersistido(conteudo, etag);
        }

        private static String chave(String documentId, ObjectNode documento) {
            return chave(documentId, texto(documento, "correlationId"));
        }

        private static String chave(String documentId, String correlationId) {
            return correlationId + '\0' + documentId;
        }

        private static String texto(ObjectNode documento, String campo) {
            return documento.path(campo).asText();
        }
    }
}
