package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CloudEventMapper {

    static final String EVENTO_REVISAO_SOLICITADA =
            "br.gov.caixa.simtr.conformidade.revisao.solicitada.v1";
    static final String EVENTO_REVISAO_CONCLUIDA =
            "br.gov.caixa.simtr.conformidade.revisao.concluida.v1";
    static final String EVENTO_ANALISE_CONCLUIDA =
            "br.gov.caixa.simtr.conformidade.analise.concluida.v1";
    static final String EXTENSAO_FLOW_INSTANCE_ID = "flowinstanceid";
    static final String EXTENSAO_FLOW_TASK_ID = "flowtaskid";
    static final URI SOURCE = URI.create("urn:simtr-hub:conformidade");

    private static final String APPLICATION_JSON = "application/json";
    private static final Set<String> TIPOS_CONHECIDOS = Set.of(
            EVENTO_REVISAO_SOLICITADA,
            EVENTO_REVISAO_CONCLUIDA,
            EVENTO_ANALISE_CONCLUIDA);
    private static final Set<String> TIPOS_FLOW_OUT = Set.of(
            EVENTO_REVISAO_SOLICITADA,
            EVENTO_ANALISE_CONCLUIDA);
    private static final JsonFormat FORMATO = new JsonFormat();

    private final ObjectMapper objectMapper;

    @Inject
    public CloudEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    byte[] serializarRevisaoConcluida(String instanceId, JsonNode revisao) {
        String correlacao = textoObrigatorio(instanceId, "flowinstanceid ausente");
        if (revisao == null || !revisao.isObject()) {
            throw new CloudEventInvalidoException("Payload da revisão deve ser um objeto JSON");
        }

        try {
            byte[] dados = objectMapper.writeValueAsBytes(revisao);
            CloudEvent evento = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(SOURCE)
                    .withType(EVENTO_REVISAO_CONCLUIDA)
                    .withTime(OffsetDateTime.now(ZoneOffset.UTC))
                    .withDataContentType(APPLICATION_JSON)
                    .withData(dados)
                    .withExtension(EXTENSAO_FLOW_INSTANCE_ID, correlacao)
                    .build();
            return FORMATO.serialize(evento);
        } catch (JsonProcessingException | RuntimeException falha) {
            if (falha instanceof CloudEventInvalidoException cloudEventInvalido) {
                throw cloudEventInvalido;
            }
            throw new CloudEventInvalidoException("Não foi possível serializar o CloudEvent", falha);
        }
    }

    EventoFlowOutRecebido lerEventoSaida(byte[] envelope) {
        CloudEvent evento = desserializar(envelope);
        if (!TIPOS_FLOW_OUT.contains(evento.getType())) {
            throw new CloudEventInvalidoException("Tipo de CloudEvent inválido para flow-out");
        }

        JsonNode dados = lerDados(evento);
        return new EventoFlowOutRecebido(
                evento.getId(),
                evento.getSource(),
                evento.getType(),
                evento.getTime(),
                extensaoObrigatoria(evento, EXTENSAO_FLOW_INSTANCE_ID),
                extensaoOpcional(evento, EXTENSAO_FLOW_TASK_ID),
                dados);
    }

    private CloudEvent desserializar(byte[] envelope) {
        if (envelope == null || envelope.length == 0) {
            throw new CloudEventInvalidoException("Envelope CloudEvent ausente");
        }

        try {
            CloudEvent evento = FORMATO.deserialize(envelope);
            validarEnvelope(evento);
            return evento;
        } catch (CloudEventInvalidoException falha) {
            throw falha;
        } catch (RuntimeException falha) {
            throw new CloudEventInvalidoException("Envelope CloudEvent inválido", falha);
        }
    }

    private static void validarEnvelope(CloudEvent evento) {
        if (evento.getSpecVersion() != SpecVersion.V1) {
            throw new CloudEventInvalidoException("Somente CloudEvent 1.0 é aceito");
        }
        textoObrigatorio(evento.getId(), "CloudEvent sem id");
        if (evento.getSource() == null) {
            throw new CloudEventInvalidoException("CloudEvent sem source");
        }
        if (!TIPOS_CONHECIDOS.contains(evento.getType())) {
            throw new CloudEventInvalidoException("Tipo de CloudEvent desconhecido");
        }
        if (evento.getTime() == null) {
            throw new CloudEventInvalidoException("CloudEvent sem time");
        }
        if (!APPLICATION_JSON.equalsIgnoreCase(evento.getDataContentType())) {
            throw new CloudEventInvalidoException("CloudEvent sem datacontenttype JSON");
        }
        extensaoObrigatoria(evento, EXTENSAO_FLOW_INSTANCE_ID);
        extensaoOpcional(evento, EXTENSAO_FLOW_TASK_ID);
        if (evento.getData() == null) {
            throw new CloudEventInvalidoException("CloudEvent sem data");
        }
    }

    private JsonNode lerDados(CloudEvent evento) {
        try {
            JsonNode dados = objectMapper.readTree(evento.getData().toBytes());
            if (dados == null || !dados.isObject()) {
                throw new CloudEventInvalidoException("Data do CloudEvent deve ser um objeto JSON");
            }
            return dados;
        } catch (IOException falha) {
            throw new CloudEventInvalidoException("Data do CloudEvent contém JSON inválido", falha);
        }
    }

    private static String extensaoObrigatoria(CloudEvent evento, String nome) {
        Object valor = evento.getExtension(nome);
        if (!(valor instanceof String texto)) {
            throw new CloudEventInvalidoException("CloudEvent sem extensão obrigatória " + nome);
        }
        return textoObrigatorio(texto, "CloudEvent sem extensão obrigatória " + nome);
    }

    private static String extensaoOpcional(CloudEvent evento, String nome) {
        Object valor = evento.getExtension(nome);
        if (valor == null) {
            return null;
        }
        if (!(valor instanceof String texto)) {
            throw new CloudEventInvalidoException("Extensão " + nome + " inválida");
        }
        return textoObrigatorio(texto, "Extensão " + nome + " inválida");
    }

    private static String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new CloudEventInvalidoException(mensagem);
        }
        return valor;
    }
}
