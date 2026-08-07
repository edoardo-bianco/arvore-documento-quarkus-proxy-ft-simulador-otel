package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EmissaoReferencialAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.TipoEmissaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
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
    static final String EXTENSAO_CORRELATION_ID = "correlationid";
    static final URI SOURCE = URI.create("urn:simtr-hub:conformidade");

    private static final String APPLICATION_JSON = "application/json";
    private static final String CAMPO_HASH_CONTEUDO = "hashConteudo";
    private static final String CAMPO_VERSAO_SCHEMA = "versaoSchema";
    private static final Set<String> TIPOS_CONHECIDOS = Set.of(
            EVENTO_REVISAO_SOLICITADA,
            EVENTO_REVISAO_CONCLUIDA,
            EVENTO_ANALISE_CONCLUIDA);
    private static final Set<String> TIPOS_FLOW_OUT = Set.of(
            EVENTO_REVISAO_SOLICITADA,
            EVENTO_ANALISE_CONCLUIDA);
    private static final JsonFormat FORMATO = new JsonFormat();

    private final ObjectMapper objectMapper;
    private final ReferenciasDocumentoAnaliseConformidade referencias;

    @Inject
    public CloudEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.referencias = new ReferenciasDocumentoAnaliseConformidade(objectMapper);
    }

    public CloudEvent mapearRevisaoPersistida(JsonNode documento) {
        try {
            if (documento == null
                    || !documento.isObject()
                    || !"revisao-humana".equals(textoJsonObrigatorio(documento, "tipo"))) {
                throw new CloudEventInvalidoException(
                        "Documento não representa uma revisão humana");
            }
            String instanceId = textoJsonObrigatorio(documento, "instanceId");
            String correlationId = textoJsonObrigatorio(documento, "correlationId");
            JsonNode versao = documento.path(CAMPO_VERSAO_SCHEMA);
            if (!versao.isIntegralNumber()
                    || !versao.canConvertToInt()
                    || versao.intValue()
                            != ReferenciaDocumentoAnaliseConformidade.VERSAO_INICIAL.intValue()) {
                throw new CloudEventInvalidoException("versaoSchema inválida");
            }
            RevisaoHumanaConformidade revisao = objectMapper.treeToValue(
                    documento.path("revisao"),
                    RevisaoHumanaConformidade.class);
            var referencia = referencias.revisao(correlationId, revisao);
            if (!referencia.documentoRef().equals(textoJsonObrigatorio(documento, "id"))
                    || !referencia.hashConteudo().equals(
                            textoJsonObrigatorio(documento, CAMPO_HASH_CONTEUDO))) {
                throw new CloudEventInvalidoException(
                        "Identidade ou integridade da revisão inválida");
            }
            byte[] dados = objectMapper.writeValueAsBytes(referencia);
            return CloudEventBuilder.v1()
                    .withId("conformidade:" + referencia.documentoRef())
                    .withSource(SOURCE)
                    .withType(EVENTO_REVISAO_CONCLUIDA)
                    .withTime(OffsetDateTime.now(ZoneOffset.UTC))
                    .withDataContentType(APPLICATION_JSON)
                    .withData(dados)
                    .withExtension(EXTENSAO_FLOW_INSTANCE_ID, instanceId)
                    .withExtension(EXTENSAO_CORRELATION_ID, correlationId)
                    .build();
        } catch (CloudEventInvalidoException falha) {
            throw falha;
        } catch (JsonProcessingException | IllegalArgumentException | NullPointerException falha) {
            throw new CloudEventInvalidoException(
                    "Documento de revisão inválido",
                    falha);
        }
    }

    byte[] serializarRevisaoConcluida(String instanceId, JsonNode referencia) {
        String correlacao = textoObrigatorio(instanceId, "flowinstanceid ausente");
        validarReferencia(referencia);

        try {
            byte[] dados = objectMapper.writeValueAsBytes(referencia);
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

    EmissaoReferencialAnaliseConformidade lerEventoSaida(byte[] envelope) {
        CloudEvent evento = desserializar(envelope);
        return lerEmissaoReferencial(evento);
    }

    boolean ehEmissaoAnaliseConformidade(CloudEvent evento) {
        return evento != null && TIPOS_FLOW_OUT.contains(evento.getType());
    }

    EmissaoReferencialAnaliseConformidade lerEmissaoReferencial(CloudEvent evento) {
        try {
            validarEnvelope(evento);
            if (!TIPOS_FLOW_OUT.contains(evento.getType())) {
                throw new CloudEventInvalidoException(
                        "Tipo de CloudEvent inválido para emissão de conformidade");
            }
            if (!SOURCE.equals(evento.getSource())) {
                throw new CloudEventInvalidoException("Source do CloudEvent inválido");
            }
            var referencia = validarReferencia(lerDados(evento));
            return new EmissaoReferencialAnaliseConformidade(
                    evento.getId(),
                    evento.getSource(),
                    TipoEmissaoAnaliseConformidade.deCloudEventType(evento.getType()),
                    evento.getTime(),
                    extensaoObrigatoria(evento, EXTENSAO_FLOW_INSTANCE_ID),
                    extensaoObrigatoria(evento, EXTENSAO_CORRELATION_ID),
                    extensaoOpcional(evento, EXTENSAO_FLOW_TASK_ID),
                    referencia);
        } catch (CloudEventInvalidoException falha) {
            throw falha;
        } catch (IllegalArgumentException | NullPointerException falha) {
            throw new CloudEventInvalidoException("Emissão referencial inválida", falha);
        }
    }

    private static ReferenciaDocumentoAnaliseConformidade validarReferencia(
            JsonNode dados) {
        if (dados == null || !dados.isObject()) {
            throw new CloudEventInvalidoException(
                    "Data do CloudEvent deve ser um objeto JSON");
        }
        if (dados.size() != 3
                || !dados.has("documentoRef")
                || !dados.has(CAMPO_HASH_CONTEUDO)
                || !dados.has(CAMPO_VERSAO_SCHEMA)) {
            throw new CloudEventInvalidoException(
                    "Data do CloudEvent deve conter somente a referência documental");
        }
        JsonNode versao = dados.path(CAMPO_VERSAO_SCHEMA);
        if (!versao.isIntegralNumber()
                || !versao.canConvertToInt()
                || versao.intValue()
                        != ReferenciaDocumentoAnaliseConformidade.VERSAO_INICIAL.intValue()) {
            throw new CloudEventInvalidoException("versaoSchema inválida");
        }
        try {
            return new ReferenciaDocumentoAnaliseConformidade(
                    textoJsonObrigatorio(dados, "documentoRef"),
                    textoJsonObrigatorio(dados, CAMPO_HASH_CONTEUDO),
                    versao.shortValue());
        } catch (IllegalArgumentException falha) {
            throw new CloudEventInvalidoException(
                    "Referência documental inválida", falha);
        }
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

    private static String textoJsonObrigatorio(JsonNode dados, String campo) {
        JsonNode valor = dados.path(campo);
        if (!valor.isTextual()) {
            throw new CloudEventInvalidoException("Campo referencial inválido: " + campo);
        }
        return textoObrigatorio(valor.textValue(), "Campo referencial inválido: " + campo);
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
