package br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Recebe da fila de saída o resultado para encerramento do recorte por log.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 9.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Ser acionada pelo MonitoramentoResultadoListener após validação/mapeamento próprio. O caso de uso solicita RegistrarResultadoMonitoramento e conclui após o registro. O listener então conclui a mensagem da saída.
 *
 * <p><strong>Verificação prevista:</strong> Provar delegação, conclusão e propagação de falha sem log duplicado. Não retomar workflow durável nem alterar dossie/Hub neste recorte.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface ReceberResultadoMonitoramento {

    /**
     * Recebe da fila de saída o resultado para encerramento do recorte por log.
     *
     * <p>Comportamento a implementar no item 9.1: Ser acionada pelo MonitoramentoResultadoListener após validação/mapeamento próprio. O caso de uso solicita RegistrarResultadoMonitoramento e conclui após o registro. O listener então conclui a mensagem da saída.
     *
     * @param resultado resultado mapeado e validado pela borda consumidora; campos ainda serão completados em 4.1
     * @return conclusão sem item de dados após o registro; falha se o registro não concluir
     */
    Uni<Void> executar(ResultadoMonitoramento resultado);
}
