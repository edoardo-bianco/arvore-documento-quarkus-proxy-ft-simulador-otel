package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.FeedEventosAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.suporte.CouchDbQuarkusTestResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = CouchDbQuarkusTestResource.class,
        restrictToAnnotatedClass = true)
@TestProfile(CouchDbChangesHttpClientIntegrationTest.Perfil.class)
class CouchDbChangesHttpClientIntegrationTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "conformidade.couchdb.host")
    String host;

    @ConfigProperty(name = "conformidade.couchdb.port")
    int port;

    @ConfigProperty(name = "conformidade.couchdb.database")
    String database;

    @ConfigProperty(name = "conformidade.couchdb.username")
    String username;

    @ConfigProperty(name = "conformidade.couchdb.password")
    String password;

    private URI baseUri;

    @BeforeEach
    void prepararEndpoint() {
        baseUri = URI.create("http://" + host + ":" + port);
    }

    @Test
    void cursorLocalSobreviveAReconstrucaoDoAdapter() throws Exception {
        var revisao = revisao();
        String correlationId = UUID.randomUUID().toString();
        var referencia = new ReferenciasDocumentoAnaliseConformidade(objectMapper)
                .revisao(correlationId, revisao);
        var documento = objectMapper.createObjectNode()
                .put("id", referencia.documentoRef())
                .put("tipo", "revisao-humana")
                .put("instanceId", "instancia-restart-couchdb")
                .put("correlationId", correlationId)
                .put("versaoSchema", referencia.versaoSchema())
                .put("hashConteudo", referencia.hashConteudo())
                .set("revisao", objectMapper.valueToTree(revisao));
        enviar(
                baseUri.resolve("/" + database + "/" + referencia.documentoRef()),
                "PUT",
                objectMapper.writeValueAsString(documento),
                201);

        var primeiroClient = novoClient();
        var primeiroFeed = new CouchDbChangesFeed(
                primeiroClient,
                new CloudEventMapper(objectMapper));
        var eventos = new ArrayList<String>();
        primeiroFeed.processarUmaVez(evento -> eventos.add(evento.getId()))
                .await()
                .indefinitely();
        primeiroFeed.close();

        var clientAposReinicio = novoClient();
        String cursorRestaurado = clientAposReinicio.carregarCursor()
                .await()
                .indefinitely();
        List<CouchDbMudanca> depoisDoCursor = clientAposReinicio
                .lerMudancas(cursorRestaurado)
                .await()
                .indefinitely();

        assertEquals(List.of("conformidade:" + referencia.documentoRef()), eventos);
        assertNotEquals("0", cursorRestaurado);
        assertEquals(List.of(), depoisDoCursor);
    }

    private CouchDbChangesHttpClient novoClient() {
        return new CouchDbChangesHttpClient(
                baseUri,
                objectMapper,
                username,
                password,
                database,
                100);
    }

    private static RevisaoHumanaConformidade revisao() {
        return new RevisaoHumanaConformidade(
                "Aprovado pelo revisor",
                List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.CONFORME,
                        "Confirmado",
                        "Trecho",
                        0.9d)));
    }

    private String enviar(
            URI uri,
            String metodo,
            String corpo,
            int statusEsperado) throws Exception {
        var publisher = corpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo);
        var request = HttpRequest.newBuilder(uri)
                .header("Authorization", autorizacao())
                .header("Content-Type", "application/json")
                .method(metodo, publisher)
                .build();
        var response = HTTP_CLIENT.send(request, ofString());
        assertEquals(statusEsperado, response.statusCode(), response.body());
        return response.body();
    }

    private String autorizacao() {
        String credenciais = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
                credenciais.getBytes(StandardCharsets.UTF_8));
    }

    public static final class Perfil implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(FeedRuntimeDesabilitado.class);
        }
    }

    @Alternative
    @ApplicationScoped
    public static class FeedRuntimeDesabilitado
            implements FeedEventosAnaliseConformidade {

        @Override
        public void iniciar(Consumer<CloudEvent> consumidor) {
            // O teste exerce diretamente outra instância do feed.
        }

        @Override
        public void parar() {
            // Nada foi iniciado pelo runtime deste perfil.
        }

        @Override
        public void close() {
            // Nada foi iniciado pelo runtime deste perfil.
        }
    }
}
