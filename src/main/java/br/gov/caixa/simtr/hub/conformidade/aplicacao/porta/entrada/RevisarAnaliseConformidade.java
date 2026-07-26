package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import io.smallrye.mutiny.Uni;

public interface RevisarAnaliseConformidade {

    Uni<Void> executar(String instanceId, RevisaoHumanaConformidade revisao);
}
