package br.gov.caixa.simtr.orquestrador.adaptador.saida.acl.monitoramento.falso;

public final class AcessosMonitoramento {
    private AcessosMonitoramento() {
    }

    public interface Permitido {
        br.gov.caixa.simtr.monitoramento.dominio.modelo.ParametrosMonitoramento executar(
                br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.PrepararMonitoramento porta);
    }

    public interface CasoDeUso {
        br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso.PrepararMonitoramentoUseCase acessar();
    }

    public interface PortaSaida {
        br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarPreValidacao acessar();
    }

    public interface Dto {
        br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto.ResultadoMonitoramentoDossieMtrV1 acessar();
    }
}
