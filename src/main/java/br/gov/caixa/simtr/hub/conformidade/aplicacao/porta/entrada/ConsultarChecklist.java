package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.smallrye.mutiny.Uni;

public interface ConsultarChecklist {

    Uni<Checklist> executar(ComandoConsultaChecklist comando);
}
