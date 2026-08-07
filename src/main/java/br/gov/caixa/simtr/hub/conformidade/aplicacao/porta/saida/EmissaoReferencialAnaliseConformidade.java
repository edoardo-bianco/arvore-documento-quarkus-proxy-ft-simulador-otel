package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

public record EmissaoReferencialAnaliseConformidade(
        String id,
        URI source,
        TipoEmissaoAnaliseConformidade tipo,
        OffsetDateTime ocorridoEm,
        String instanceId,
        String correlationId,
        String taskId,
        ReferenciaDocumentoAnaliseConformidade documento) {

    public EmissaoReferencialAnaliseConformidade {
        exigirTexto(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm");
        exigirTexto(instanceId, "instanceId");
        exigirTexto(correlationId, "correlationId");
        if (taskId != null) {
            exigirTexto(taskId, "taskId");
        }
        Objects.requireNonNull(documento, "documento");
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatório");
        }
    }
}
