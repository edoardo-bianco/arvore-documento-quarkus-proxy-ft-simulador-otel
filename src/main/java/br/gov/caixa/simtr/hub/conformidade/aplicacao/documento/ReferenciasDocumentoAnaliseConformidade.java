package br.gov.caixa.simtr.hub.conformidade.aplicacao.documento;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@ApplicationScoped
public class ReferenciasDocumentoAnaliseConformidade {

    private final ObjectMapper objectMapper;

    @Inject
    public ReferenciasDocumentoAnaliseConformidade(ObjectMapper objectMapper) {
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public ReferenciaDocumentoAnaliseConformidade resultadoPreliminar(
            String correlationId,
            ResultadoAnaliseConformidade resultado) {
        return referencia(
                IdsDocumentoAnaliseConformidade.resultadoPreliminar(correlationId),
                resultado);
    }

    public ReferenciaDocumentoAnaliseConformidade resultadoFinal(
            String correlationId,
            ResultadoAnaliseConformidade resultado) {
        return referencia(
                IdsDocumentoAnaliseConformidade.resultadoFinal(correlationId),
                resultado);
    }

    public ReferenciaDocumentoAnaliseConformidade checklist(
            String correlationId,
            Checklist checklist) {
        if (checklist == null) {
            throw FalhaAnaliseConformidade.checklistInvalido(
                    "Checklist obrigatório");
        }
        return referencia(
                IdsDocumentoAnaliseConformidade.checklist(correlationId),
                checklist);
    }

    public ReferenciaDocumentoAnaliseConformidade revisao(
            String correlationId,
            RevisaoHumanaConformidade revisao) {
        if (revisao == null) {
            throw FalhaAnaliseConformidade.revisaoInconsistente(
                    "Revisão humana obrigatória");
        }
        return referencia(
                IdsDocumentoAnaliseConformidade.revisao(correlationId),
                revisao);
    }

    private ReferenciaDocumentoAnaliseConformidade referencia(
            String documentoRef,
            Object conteudo) {
        if (conteudo == null) {
            throw FalhaAnaliseConformidade.resultadoInvalido("Resultado obrigatório");
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(conteudo);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return new ReferenciaDocumentoAnaliseConformidade(
                    documentoRef,
                    HexFormat.of().formatHex(digest),
                    ReferenciaDocumentoAnaliseConformidade.VERSAO_INICIAL);
        } catch (JsonProcessingException | NoSuchAlgorithmException impossivel) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }
}
