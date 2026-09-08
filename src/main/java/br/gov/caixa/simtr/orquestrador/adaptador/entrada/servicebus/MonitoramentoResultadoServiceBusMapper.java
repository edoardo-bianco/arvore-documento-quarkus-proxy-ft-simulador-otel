package br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus;

import br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.dto.ResultadoMonitoramentoDossieMtrV1;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.LogErroResultado.Tipo;

/**
 * Valida envelope e JSON de resultado antes de produzir o modelo proprio do orquestrador.
 *
 * <p>Preserva a situacao original do MTR separada da pre-validacao calculada.
 * Nao executa listener, settlement, persistencia ou continuidade de workflow.
 */
@ApplicationScoped
public class MonitoramentoResultadoServiceBusMapper {

    private static final String OPERACAO_MAPEAMENTO = "paraResultado";
    private static final String OPERACAO_VALIDACAO_TIPOS = "validarTipos";
    private static final List<String> TEXTOS = List.of("monitoramentoId", "orquestracaoId",
            "idDossiePreValidacao", "idDossieMtr", "resultadoMonitoramento",
            "situacaoPreValidacao", "motivo", "iniciadoEm", "concluidoEm");
    private static final List<String> INTEIROS = List.of("schemaVersion", "tentativasRealizadas");
    private final ObjectMapper json;
    private final ObjectReader leitor;
    private final Validator validator;
    private final String recurso;

    /**
     * @param json mapeador configurado pelo Quarkus
     * @param validator validador do DTO proprio do consumidor
     * @param recurso fila de saida configurada, usada no log
     */
    @Inject
    public MonitoramentoResultadoServiceBusMapper(ObjectMapper json, Validator validator,
            @ConfigProperty(name = "monitoramento.service-bus.output-queue") String recurso) {
        this.json = json;
        this.leitor = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = validator;
        this.recurso = recurso;
    }

    /**
     * @param corpo JSON v1 recebido da fila de saida
     * @param messageId identidade deterministica do resultado
     * @param correlationId identidade da orquestracao
     * @param subject assunto da mensagem de resultado
     * @param contentType tipo de conteudo JSON
     * @return resultado semanticamente proprio do orquestrador
     * @throws MapeamentoResultadoException se parsing, envelope ou contrato forem invalidos
     */
    public ResultadoMonitoramento paraResultado(String corpo, String messageId, String correlationId,
            String subject, String contentType) {
        if (!"RESULTADO_MONITORAMENTO_DOSSIE_MTR".equals(subject) || !"application/json".equals(contentType)) {
            throw falha(Tipo.ENVELOPE_INVALIDO, OPERACAO_MAPEAMENTO);
        }
        var contrato = lerContrato(corpo);
        if (!validator.validate(contrato).isEmpty()
                || contrato.concluidoEm().isBefore(contrato.iniciadoEm())) {
            throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_MAPEAMENTO);
        }
        if (!contrato.orquestracaoId().equals(correlationId)
                || !(contrato.monitoramentoId() + ":resultado:v1").equals(messageId)) {
            throw falha(Tipo.ENVELOPE_INVALIDO, OPERACAO_MAPEAMENTO);
        }
        return new ResultadoMonitoramento(contrato.monitoramentoId(), contrato.orquestracaoId(),
                contrato.idDossiePreValidacao(), contrato.idDossieMtr(), contrato.resultadoMonitoramento(),
                contrato.situacaoMtr(), contrato.situacaoPreValidacao(), contrato.motivo(),
                contrato.tentativasRealizadas(), contrato.iniciadoEm(), contrato.concluidoEm(),
                contrato.inputSequenceNumber());
    }

    private ResultadoMonitoramentoDossieMtrV1 lerContrato(String corpo) {
        if (corpo == null) {
            throw falha(Tipo.CONTRATO_INVALIDO, "lerContrato");
        }
        try {
            JsonNode arvore = leitor.readTree(corpo);
            validarTipos(arvore);
            return json.treeToValue(arvore, ResultadoMonitoramentoDossieMtrV1.class);
        } catch (JsonProcessingException causa) {
            throw LogErroResultado.registrar(Tipo.JSON_INVALIDO, "lerContrato", recurso, causa);
        }
    }

    private void validarTipos(JsonNode arvore) {
        if (arvore == null || !arvore.isObject()) {
            throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
        }
        for (String campo : TEXTOS) {
            if (!arvore.path(campo).isTextual()) {
                throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
            }
        }
        for (String campo : INTEIROS) {
            var valor = arvore.path(campo);
            if (!valor.isIntegralNumber() || !valor.canConvertToInt()) {
                throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
            }
        }
        var situacaoMtr = arvore.path("situacaoMtr");
        if (!situacaoMtr.isMissingNode() && !situacaoMtr.isNull() && !situacaoMtr.isTextual()) {
            throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
        }
        var sequencia = arvore.path("inputSequenceNumber");
        if (!sequencia.isIntegralNumber() || !sequencia.canConvertToLong()) {
            throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
        }
    }

    private MapeamentoResultadoException falha(Tipo tipo, String operacao) {
        return LogErroResultado.registrar(tipo, operacao, recurso, null);
    }
}
