package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class ConsultaDossieProdutoResourceQuarkusTest {

    private static final String PATH = "/simtr-hub/v1/dossie-produto/{id}";
    private static final long IDENTIFICADOR = 4324680L;
    private static final String RECURSO = "simtr-dossie-produto";
    private static final String ID_ERRO = "dossie-404";
    private static final String CODIGO_ERRO = "MTR-DOSSIE-404";
    private static final String MENSAGEM_ERRO = "dossie nao localizado";
    private static final String DETALHE_ERRO = "falha controlada";
    private static final String STACKTRACE_ERRO = "stack-remota";

    @InjectMock
    ConsultarDossieProduto consultarDossieProduto;

    @Test
    void resourceUsaSomentePortaEntradaEMapeiaRespostaPublica() {
        when(consultarDossieProduto.executar(any())).thenReturn(
                Uni.createFrom().item(dossie(IDENTIFICADOR))
        );

        given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .get(PATH, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("id", equalTo(4324680))
                .body("clientes.size()", equalTo(0))
                .body("unidades_tratamento.size()", equalTo(0))
                .body("produtos_contratados.size()", equalTo(0));

        var captor = ArgumentCaptor.forClass(IdentificadorDossieProduto.class);
        verify(consultarDossieProduto).executar(captor.capture());
        assertEquals(IDENTIFICADOR, captor.getValue().valor());
    }

    @Test
    void resourceTraduzFalhaInternaParaErroPublicoCompleto() {
        when(consultarDossieProduto.executar(any())).thenReturn(Uni.createFrom().failure(
                new FalhaConsultaDossieProduto(
                        FalhaConsultaDossieProduto.Tipo.NEGOCIO,
                        404,
                        RECURSO,
                        ID_ERRO,
                        CODIGO_ERRO,
                        List.of(MENSAGEM_ERRO),
                        DETALHE_ERRO,
                        STACKTRACE_ERRO,
                        null
                )
        ));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .get(PATH, 987654L)
                .then()
                .statusCode(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("codigo_http", equalTo(404))
                .body("recurso", equalTo(RECURSO))
                .body("id_erro", equalTo(ID_ERRO))
                .body("codigo_erro", equalTo(CODIGO_ERRO))
                .body("erros[0].mensagem", equalTo(MENSAGEM_ERRO))
                .body("detalhe", equalTo(DETALHE_ERRO))
                .body("stacktrace", equalTo(STACKTRACE_ERRO));
    }

    @Test
    void declaraGetSemCorpoESpanServerAprovado() throws NoSuchMethodException {
        var metodo = DossieProdutoResource.class.getMethod(
                "consultarDossieProduto", Long.class
        );

        assertNotNull(metodo.getAnnotation(GET.class));
        assertEquals("/{id}", metodo.getAnnotation(Path.class).value());
        assertEquals(MediaType.WILDCARD, metodo.getAnnotation(Consumes.class).value()[0]);
        WithSpan span = metodo.getAnnotation(WithSpan.class);
        assertEquals("simtr-hub.api.dossie-produto.consultar", span.value());
        assertEquals(SpanKind.SERVER, span.kind());
    }

    private static DossieProdutoConsultado dossie(Long id) {
        return new DossieProdutoConsultado(
                id, null, null, null, "SIMTRAPI", 5402, null,
                List.of(), null, null, null, List.of(), List.of()
        );
    }
}
