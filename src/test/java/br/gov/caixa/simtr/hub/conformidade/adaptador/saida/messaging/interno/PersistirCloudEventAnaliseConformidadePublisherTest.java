package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_SOLICITADA;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EmissaoReferencialAnaliseConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PersistirCloudEventAnaliseConformidadePublisherTest {

    private static final String HASH = "a".repeat(64);

    private final ArmazenarEstadoAnaliseConformidade estados =
            mock(ArmazenarEstadoAnaliseConformidade.class);
    private final CloudEventMapper mapper = new CloudEventMapper(new ObjectMapper());
    private final PersistirCloudEventAnaliseConformidadePublisher publisher =
            new PersistirCloudEventAnaliseConformidadePublisher(estados, mapper);

    @Test
    void persisteEmissaoReferencialPelaPortaNeutra() {
        when(estados.registrarEmissao(any())).thenReturn(Uni.createFrom().voidItem());
        var evento = CloudEventMapperTest.evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-publisher",
                "correlacao-publisher",
                "tarefa-publisher",
                dados("resultado-preliminar-publisher"));

        publisher.publish(evento).join();

        var captor = ArgumentCaptor.forClass(EmissaoReferencialAnaliseConformidade.class);
        verify(estados).registrarEmissao(captor.capture());
        var emissao = captor.getValue();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals(
                        "instancia-publisher", emissao.instanceId()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(
                        "correlacao-publisher", emissao.correlationId()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(
                        "resultado-preliminar-publisher",
                        emissao.documento().documentoRef()));
    }

    @Test
    void ignoraEventoDeOutroWorkflow() {
        var evento = CloudEventMapperTest.evento(
                "br.gov.caixa.simtr.outro.evento.v1",
                "outra-instancia",
                "outra-correlacao",
                null,
                dados("outro-documento"));

        publisher.publish(evento).join();

        verifyNoInteractions(estados);
    }

    @Test
    void propagaFalhaDaPortaSemBloquearThread() {
        when(estados.registrarEmissao(any())).thenReturn(
                Uni.createFrom().failure(new IllegalStateException("store indisponível")));
        var evento = CloudEventMapperTest.evento(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-falha",
                "correlacao-falha",
                null,
                dados("resultado-preliminar-falha"));

        CompletionException falha = assertThrows(
                CompletionException.class,
                () -> publisher.publish(evento).join());

        assertInstanceOf(IllegalStateException.class, falha.getCause());
    }

    private static String dados(String documentoRef) {
        return "{\"documentoRef\":\"" + documentoRef
                + "\",\"hashConteudo\":\"" + HASH
                + "\",\"versaoSchema\":1}";
    }
}
