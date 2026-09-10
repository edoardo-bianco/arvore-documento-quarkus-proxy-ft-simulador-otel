package br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.ReceberResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.RegistrarResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;

/** Aguarda a porta de registro; nao executa settlement, persistencia ou continuidade de workflow. */
@ApplicationScoped
public class ReceberResultadoMonitoramentoUseCase implements ReceberResultadoMonitoramento {
    private final RegistrarResultadoMonitoramento registrar;

    @Inject
    public ReceberResultadoMonitoramentoUseCase(RegistrarResultadoMonitoramento registrar) {
        this.registrar = registrar;
    }

    /** Uma submissao por invocacao; novas entregas podem registrar o mesmo resultado novamente. */
    @Override
    public Uni<Void> executar(ResultadoMonitoramento resultado) {
        return Uni.createFrom().deferred(() -> registrar.executar(
                Objects.requireNonNull(resultado, "Resultado de monitoramento obrigatorio.")))
                .memoize().indefinitely();
    }
}
