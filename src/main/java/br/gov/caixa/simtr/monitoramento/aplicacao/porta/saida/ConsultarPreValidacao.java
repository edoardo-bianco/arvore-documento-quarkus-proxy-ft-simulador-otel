package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.PreValidacaoConsultada;
import io.smallrye.mutiny.Uni;

/**
 * Consulta a situação de pré-validação usada nos critérios do monitoramento.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 5.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Implementar pelo adapter simulado próprio e determinístico. Fornecer a situação que permite decidir se ainda há EM_ANALISE_ENVIO_MTR; este recorte não acessa banco nem persiste transição.
 *
 * <p><strong>Verificação prevista:</strong> Provar cenários, tradução e ausência/indisponibilidade conforme contrato a detalhar; não confundir esse simulador de negócio com o emulador Service Bus.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface ConsultarPreValidacao {

    /**
     * Consulta a situação de pré-validação usada nos critérios do monitoramento.
     *
     * <p>Comportamento a implementar no item 5.1: Implementar pelo adapter simulado próprio e determinístico. Fornecer a situação que permite decidir se ainda há EM_ANALISE_ENVIO_MTR; este recorte não acessa banco nem persiste transição.
     *
     * @param idDossiePreValidacao identificador textual da pré-validação, sem conversão arbitrária para UUID
     * @return visão própria da pré-validação; ausência e falhas devem ser detalhadas no item 5.1
     */
    Uni<PreValidacaoConsultada> executar(String idDossiePreValidacao);
}
