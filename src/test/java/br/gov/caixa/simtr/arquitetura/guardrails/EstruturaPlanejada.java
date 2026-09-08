package br.gov.caixa.simtr.arquitetura.guardrails;

import java.util.List;

/** Inventario temporario: retirar cada classe somente quando sua fatia funcional for verificada. */
final class EstruturaPlanejada {

    static final List<String> ESQUELETOS = List.of(
            "br.gov.caixa.simtr.monitoramento.dominio.modelo.PreValidacaoConsultada",
            "br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada",
            "br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento",
            "br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento",
            "br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso.IniciarMonitoramentoUseCase",
            "br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso.ReceberResultadoMonitoramentoUseCase",
            "br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.MonitoramentoDossieResource",
            "br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.MonitoramentoResultadoListener",
            "br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.MonitoramentoEntradaPublisher",
            "br.gov.caixa.simtr.orquestrador.adaptador.saida.log.ResultadoMonitoramentoLogAdapter",
            "br.gov.caixa.simtr.orquestrador.adaptador.saida.acl.monitoramento.ParametrosMonitoramentoAcl",
            "br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso.PrepararMonitoramentoUseCase",
            "br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso.ProcessarTentativaMonitoramentoUseCase",
            "br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.MonitoramentoEntradaListener",
            "br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao.PreValidacaoSimuladaAdapter",
            "br.gov.caixa.simtr.monitoramento.adaptador.saida.acl.simtrhub.SituacaoDossieHubAcl",
            "br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.MonitoramentoResultadoPublisher",
            "br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.MonitoramentoReagendamentoAdapter",
            "br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ClientesServiceBus"
    );

    private EstruturaPlanejada() {
    }
}
