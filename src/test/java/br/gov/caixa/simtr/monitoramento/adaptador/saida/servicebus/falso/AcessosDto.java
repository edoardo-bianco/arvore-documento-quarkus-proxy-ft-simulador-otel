package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.falso;

public final class AcessosDto {
    private AcessosDto() {
    }

    public interface Permitido {
        br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto.ResultadoMonitoramentoDossieMtrV1 acessar();
    }

    public interface OutraBorda {
        br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.dto.MonitorarDossieMtrV1 acessar();
    }

    public interface OutroComponente {
        br.gov.caixa.simtr.orquestrador.adaptador.entrada.servicebus.dto.ResultadoMonitoramentoDossieMtrV1 acessar();
    }
}
