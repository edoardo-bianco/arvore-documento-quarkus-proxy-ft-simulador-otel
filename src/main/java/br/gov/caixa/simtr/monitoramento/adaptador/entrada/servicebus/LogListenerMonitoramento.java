package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade.CamposLogJson;
import io.quarkus.jsonp.JsonProviderHolder;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/** Eventos aprovados com valores locais; nunca recebe entrega, payload ou Throwable. */
final class LogListenerMonitoramento {
    private static final String COMPONENTE = MonitoramentoEntradaListener.class.getName();
    private static final Logger LOG = Logger.getLogger(COMPONENTE);

    private LogListenerMonitoramento() {
    }

    static void decisao(String decisao) {
        registrar(Level.INFO, "doctree.monitoramento-mtr.decisao.tomada", "decidir", "decisao", decisao);
    }

    static void falha(String operacao) {
        registrar(Level.ERROR, "doctree.monitoramento-mtr.processamento.falhou",
                operacao, "error_type", "FALHA_TECNICA");
    }

    static void settlement(String acao) {
        registrar(Level.INFO, "doctree.monitoramento-mtr.settlement.executado", "liquidar", "settlement", acao);
    }

    private static void registrar(Level nivel, String evento, String operacao, String campo, String valor) {
        var campos = JsonProviderHolder.jsonProvider().createObjectBuilder()
                .add("evento", evento).add("camada", "adaptador")
                .add("componente", "MonitoramentoEntradaListener").add("operacao", operacao)
                .add(campo, valor).build();
        var registro = new ExtLogRecord(nivel, evento, LogListenerMonitoramento.class.getName());
        registro.setLoggerName(COMPONENTE);
        registro.setMarker(new CamposLogJson(campos));
        LOG.log(registro);
    }
}
