package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;

public interface ConsultarAnaliseConformidade {

    Uni<VisaoAnaliseConformidade> executar(String instanceId);
}
