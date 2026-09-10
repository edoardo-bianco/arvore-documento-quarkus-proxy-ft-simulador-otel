package br.gov.caixa.simtr.arquitetura.guardrails;

import java.util.List;

/** Componentes que concluem o fluxo de resultado; todos devem resolver por CDI. */
final class ComponentesResultadoMonitoramento {
    static final List<String> CLASSES = List.of(
            "br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoListener",
            "br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso.ReceberResultadoMonitoramentoUseCase",
            "br.gov.caixa.simtr.orquestrador.adaptador.saida.log.ResultadoMonitoramentoLogAdapter");

    private ComponentesResultadoMonitoramento() {
    }
}
