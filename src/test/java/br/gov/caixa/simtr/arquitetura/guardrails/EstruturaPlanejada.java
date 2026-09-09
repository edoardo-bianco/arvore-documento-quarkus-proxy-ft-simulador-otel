package br.gov.caixa.simtr.arquitetura.guardrails;

import java.util.List;

/** Inventario temporario: retirar cada classe somente quando sua fatia funcional for verificada. */
final class EstruturaPlanejada {

    static final List<String> ESQUELETOS = List.of(
            "br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento",
            "br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento",
            "br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso.ReceberResultadoMonitoramentoUseCase",
            "br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoListener",
            "br.gov.caixa.simtr.orquestrador.adaptador.saida.log.ResultadoMonitoramentoLogAdapter",
            "br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso.ProcessarTentativaMonitoramentoUseCase",
            "br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.MonitoramentoEntradaListener",
            "br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.MonitoramentoReagendamentoAdapter"
    );

    private EstruturaPlanejada() {
    }
}
