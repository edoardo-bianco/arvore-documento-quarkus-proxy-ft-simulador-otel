package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumental;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import java.util.Optional;

final class CouchDbRepositorioDocumental implements RepositorioDocumental {

    private final CouchDbClient client;
    private final String database;

    CouchDbRepositorioDocumental(CouchDbClient client, String database) {
        this.client = java.util.Objects.requireNonNull(client, "client");
        this.database = validarDatabase(database);
    }

    @Override
    public Uni<ResultadoGravacao> criar(
            String documentId,
            ObjectNode documento) {
        return client.gravar(database, documentId, documento)
                .map(CouchDbRepositorioDocumental::resultadoGravacao);
    }

    @Override
    public Uni<ResultadoGravacao> substituir(
            String documentId,
            String versaoEsperada,
            ObjectNode documento) {
        ObjectNode documentoCouchDb = documento.deepCopy()
                .put("_rev", validarVersao(versaoEsperada));
        return client.gravar(database, documentId, documentoCouchDb)
                .map(CouchDbRepositorioDocumental::resultadoGravacao);
    }

    @Override
    public Uni<Optional<DocumentoPersistido>> consultar(
            String documentId,
            String correlationId) {
        return client.consultar(database, documentId)
                .map(resposta -> {
                    if (resposta.status() == 404) {
                        return Optional.empty();
                    }
                    if (resposta.status() == 200
                            && resposta.body() instanceof ObjectNode documento) {
                        String versao = texto(documento, "_rev");
                        ObjectNode conteudo = documento.deepCopy();
                        conteudo.remove("_id");
                        conteudo.remove("_rev");
                        return Optional.of(new DocumentoPersistido(conteudo, versao));
                    }
                    throw new IllegalStateException("Resposta CouchDB inválida");
                });
    }

    private static ResultadoGravacao resultadoGravacao(
            CouchDbClient.Resposta resposta) {
        return switch (resposta.status()) {
            case 201 -> ResultadoGravacao.GRAVADO;
            case 409 -> ResultadoGravacao.CONFLITO;
            default -> throw new IllegalStateException("Gravação CouchDB inválida");
        };
    }

    private static String validarDatabase(String database) {
        if (database == null || !database.matches("[a-z][a-z0-9_$()+-]{0,237}")) {
            throw new IllegalArgumentException("Nome de database CouchDB inválido");
        }
        return database;
    }

    private static String validarVersao(String versao) {
        if (versao == null || versao.isBlank()) {
            throw new IllegalArgumentException("Versão CouchDB inválida");
        }
        return versao;
    }

    private static String texto(ObjectNode documento, String campo) {
        var valor = documento.path(campo);
        if (!valor.isTextual() || valor.textValue().isBlank()) {
            throw new IllegalStateException("Resposta CouchDB inválida");
        }
        return valor.textValue();
    }
}
