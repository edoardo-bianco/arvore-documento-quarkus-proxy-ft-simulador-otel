package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarChecklist;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.smallrye.mutiny.Uni;

public final class ConsultarChecklistCasoDeUso implements ConsultarChecklist {

    private final ObterChecklist portaSaida;

    public ConsultarChecklistCasoDeUso(ObterChecklist portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<Checklist> executar(ComandoConsultaChecklist comando) {
        return portaSaida.obter(comando);
    }
}
