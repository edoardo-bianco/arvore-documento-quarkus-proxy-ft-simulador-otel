package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus;

import br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.dto.MonitorarDossieMtrV1;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
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

import static br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.LogErroEntradaMonitoramento.Tipo;

@ApplicationScoped
public class MonitoramentoEntradaServiceBusMapper {

    private static final String OPERACAO_MAPEAMENTO = "paraTentativa";
    private static final String OPERACAO_VALIDACAO_TIPOS = "validarTipos";
    private static final List<String> CAMPOS_TEXTO = List.of("monitoramentoId", "orquestracaoId",
            "idDossiePreValidacao", "idDossieMtr", "iniciadoEm", "limiteEm", "politicaMonitoramentoVersao");
    private static final List<String> CAMPOS_INTEIROS = List.of("schemaVersion", "tentativaAtual");

    private final ObjectMapper json;
    private final ObjectReader leitor;
    private final Validator validator;
    private final String recurso;

    @Inject
    public MonitoramentoEntradaServiceBusMapper(ObjectMapper json, Validator validator,
            @ConfigProperty(name = "monitoramento.service-bus.input-queue") String recurso) {
        this.json = json;
        this.leitor = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = validator;
        this.recurso = recurso;
    }

    public TentativaMonitoramento paraTentativa(String corpo, String messageId, String correlationId,
            String subject, String contentType) {
        if (!"MONITORAR_DOSSIE_MTR".equals(subject) || !"application/json".equals(contentType)) {
            throw falha(Tipo.ENVELOPE_INVALIDO, OPERACAO_MAPEAMENTO);
        }
        var contrato = lerContrato(corpo);
        if (!validator.validate(contrato).isEmpty()) {
            throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_MAPEAMENTO);
        }
        if (!contrato.limiteEm().isAfter(contrato.iniciadoEm())) {
            throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_MAPEAMENTO);
        }
        if (!contrato.orquestracaoId().equals(correlationId)
                || !(contrato.monitoramentoId() + ":tentativa:" + contrato.tentativaAtual()).equals(messageId)) {
            throw falha(Tipo.ENVELOPE_INVALIDO, OPERACAO_MAPEAMENTO);
        }
        return new TentativaMonitoramento(contrato.monitoramentoId(), contrato.orquestracaoId(),
                contrato.idDossiePreValidacao(), contrato.idDossieMtr(), contrato.tentativaAtual(),
                contrato.iniciadoEm(), contrato.limiteEm(), contrato.politicaMonitoramentoVersao());
    }

    private MonitorarDossieMtrV1 lerContrato(String corpo) {
        if (corpo == null) {
            throw falha(Tipo.CONTRATO_INVALIDO, "lerContrato");
        }
        try {
            JsonNode arvore = leitor.readTree(corpo);
            validarTipos(arvore);
            return json.treeToValue(arvore, MonitorarDossieMtrV1.class);
        } catch (JsonProcessingException causa) {
            throw LogErroEntradaMonitoramento.registrar(Tipo.JSON_INVALIDO, "lerContrato", recurso, causa);
        }
    }

    private ContratoMonitoramentoInvalidoException falha(Tipo tipo, String operacao) {
        return LogErroEntradaMonitoramento.registrar(tipo, operacao, recurso, null);
    }

    private void validarTipos(JsonNode arvore) {
        if (arvore == null || !arvore.isObject()) {
            throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
        }
        for (String campo : CAMPOS_TEXTO) {
            if (!arvore.path(campo).isTextual()) {
                throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
            }
        }
        for (String campo : CAMPOS_INTEIROS) {
            var valor = arvore.path(campo);
            if (!valor.isIntegralNumber() || !valor.canConvertToInt()) {
                throw falha(Tipo.CONTRATO_INVALIDO, OPERACAO_VALIDACAO_TIPOS);
            }
        }
    }
}
