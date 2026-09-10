package br.gov.caixa.simtr.orquestrador.adaptador.saida.log;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.RegistrarResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/**
 * Submete o evento final ao logging padrao com contexto proprio por registro.
 * Best-effort: retorno normal nao confirma gravacao nem detecta falhas internas dos handlers.
 */
@ApplicationScoped
public class ResultadoMonitoramentoLogAdapter implements RegistrarResultadoMonitoramento {
    private static final String EVENTO = "orquestrador.monitoramento-dossie.resultado.registrado";
    private static final Logger LOG = Logger.getLogger(ResultadoMonitoramentoLogAdapter.class.getName());

    /** Emite na primeira assinatura; o Uni compartilha a conclusao somente desta invocacao. */
    @Override
    public Uni<Void> executar(ResultadoMonitoramento resultado) {
        return Uni.createFrom().voidItem().invoke(() -> registrar(resultado)).memoize().indefinitely();
    }

    private void registrar(ResultadoMonitoramento resultado) {
        Objects.requireNonNull(resultado, "Resultado de monitoramento obrigatorio.");
        var monitoramentoId = Objects.requireNonNull(resultado.monitoramentoId(),
                "Identidade de monitoramento obrigatoria.");
        var orquestracaoId = Objects.requireNonNull(resultado.orquestracaoId(),
                "Identidade de orquestracao obrigatoria.");
        var registro = new ExtLogRecord(Level.INFO, EVENTO, ResultadoMonitoramentoLogAdapter.class.getName());
        registro.copyMdc();
        registro.putMdc("evento", EVENTO);
        registro.putMdc("camada", "adaptador");
        registro.putMdc("componente", "ResultadoMonitoramentoLogAdapter");
        registro.putMdc("operacao", "registrar");
        registro.putMdc("monitoramento_id", monitoramentoId);
        registro.putMdc("orquestracao_id", orquestracaoId);
        LOG.log(registro);
    }
}
