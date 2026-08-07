package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.ContextoAnaliseConformidadeFlow;
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
            ReferenciaDocumentoAnaliseConformidade referencia = taskContext.rawInput()
                    .as(ReferenciaDocumentoAnaliseConformidade.class)
                    .orElseThrow(() -> new CloudEventInvalidoException(
                            "A emissão do workflow deve ser referencial"));
            ContextoAnaliseConformidadeFlow contexto = workflowContext.instanceData()
                    .input()
                    .as(ContextoAnaliseConformidadeFlow.class)
                    .orElseThrow(() -> new CloudEventInvalidoException(
                            "A correlação original da análise está ausente"));
            builder.withId("conformidade:" + referencia.documentoRef())
                    .withSource(CloudEventMapper.SOURCE)
                    .withTime(OffsetDateTime.now(ZoneOffset.UTC));
            builder.withExtension(
                    CloudEventMapper.EXTENSAO_CORRELATION_ID,
                    contexto.correlationId());
            builder.withExtension(
                    CloudEventMapper.EXTENSAO_FLOW_INSTANCE_ID,
                    workflowContext.instance().id());
            builder.withExtension(
                    CloudEventMapper.EXTENSAO_FLOW_TASK_ID,
                    taskContext.taskName());
        }
    }
}
