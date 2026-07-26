package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.events.EmittedEventDecorator;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Completa os atributos do contrato CloudEvent que não são preenchidos pelo
 * {@code emitJson} do SDK Serverless Workflow 7.22.2.
 */
public final class MetadadosCloudEventAnaliseConformidadeDecorator
        implements EmittedEventDecorator {

    private static final String WORKFLOW_ANALISE_CONFORMIDADE = "analise-conformidade";

    @Override
    public void decorate(
            CloudEventBuilder builder,
            WorkflowContext workflowContext,
            TaskContext taskContext) {
        String nomeWorkflow = workflowContext.definition()
                .workflow()
                .getDocument()
                .getName();
        if (WORKFLOW_ANALISE_CONFORMIDADE.equals(nomeWorkflow)) {
            builder.withSource(CloudEventMapper.SOURCE)
                    .withTime(OffsetDateTime.now(ZoneOffset.UTC));
        }
    }
}
