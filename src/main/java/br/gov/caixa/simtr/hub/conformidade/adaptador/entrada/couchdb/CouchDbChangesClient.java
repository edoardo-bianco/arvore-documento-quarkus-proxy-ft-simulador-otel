package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb;

import io.smallrye.mutiny.Uni;
import java.util.List;

interface CouchDbChangesClient {

    Uni<String> carregarCursor();

    Uni<List<CouchDbMudanca>> lerMudancas(String desde);

    Uni<Void> salvarCursor(String sequencia);
}
