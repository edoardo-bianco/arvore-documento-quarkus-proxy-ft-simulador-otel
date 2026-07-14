package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.smallrye.mutiny.Uni;

public interface ObterChecklist {

    Uni<Checklist> obter(ComandoConsultaChecklist comando);
}
