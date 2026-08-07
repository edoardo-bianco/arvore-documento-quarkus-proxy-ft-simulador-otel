package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow.ContextoAnaliseConformidadeFlow;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MetadadosCloudEventAnaliseConformidadeDecoratorTest {

    @Test
    void adicionaIdDeterministicoSourceTimeECorrelacaoMinuscula() {
        var referencia = new ReferenciaDocumentoAnaliseConformidade(
                "resultado-preliminar-123",
                "a".repeat(64),
                (short) 1);
        var solicitacao = new SolicitacaoAnaliseConformidade(
                "correlacao-123",
                "DOC-123",
                "Texto protegido",
                1000012583L,
                1);
        var contexto = ContextoAnaliseConformidadeFlow.inicial(solicitacao);
        WorkflowContext workflowContext = mock(WorkflowContext.class, RETURNS_DEEP_STUBS);
        when(workflowContext.definition().workflow().getDocument().getName())
                .thenReturn("analise-conformidade");
        when(workflowContext.instance().id()).thenReturn("instancia-123");
        WorkflowModel entradaInstancia = mock(WorkflowModel.class);
        when(workflowContext.instanceData().input()).thenReturn(entradaInstancia);
        when(entradaInstancia.as(ContextoAnaliseConformidadeFlow.class))
                .thenReturn(Optional.of(contexto));
        TaskContext taskContext = mock(TaskContext.class);
        when(taskContext.taskName()).thenReturn("emitirSolicitacaoRevisao");
        WorkflowModel entradaTarefa = mock(WorkflowModel.class);
        when(taskContext.rawInput()).thenReturn(entradaTarefa);
        when(entradaTarefa.as(ReferenciaDocumentoAnaliseConformidade.class))
                .thenReturn(Optional.of(referencia));
        var primeiroBuilder = CloudEventBuilder.v1()
                .withType(CloudEventMapper.EVENTO_REVISAO_SOLICITADA);
        var segundoBuilder = CloudEventBuilder.v1()
                .withType(CloudEventMapper.EVENTO_REVISAO_SOLICITADA);
        var decorator = new MetadadosCloudEventAnaliseConformidadeDecorator();

        decorator.decorate(primeiroBuilder, workflowContext, taskContext);
        decorator.decorate(segundoBuilder, workflowContext, taskContext);
        var primeiro = primeiroBuilder.build();
        var segundo = segundoBuilder.build();

        assertEquals(primeiro.getId(), segundo.getId());
        assertEquals("conformidade:resultado-preliminar-123", primeiro.getId());
        assertEquals(CloudEventMapper.SOURCE, primeiro.getSource());
        assertEquals("instancia-123", primeiro.getExtension("flowinstanceid"));
        assertEquals(
                "emitirSolicitacaoRevisao",
                primeiro.getExtension("flowtaskid"));
        assertEquals("correlacao-123", primeiro.getExtension("correlationid"));
        assertNotNull(primeiro.getTime());
    }
}
