package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieRequest;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieResponse;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;

final class MonitoramentoDossieRestMapper {

    private MonitoramentoDossieRestMapper() {
    }

    static SolicitacaoMonitoramento paraSolicitacao(IniciarMonitoramentoDossieRequest request) {
        return new SolicitacaoMonitoramento(request.idDossiePreValidacao(), request.idDossieMtr());
    }

    static IniciarMonitoramentoDossieResponse paraResposta(MonitoramentoIniciado iniciado) {
        return new IniciarMonitoramentoDossieResponse(iniciado.monitoramentoId(), iniciado.orquestracaoId());
    }
}
