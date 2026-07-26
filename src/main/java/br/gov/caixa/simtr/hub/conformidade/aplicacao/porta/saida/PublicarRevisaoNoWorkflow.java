package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import io.smallrye.mutiny.Uni;

public interface PublicarRevisaoNoWorkflow {

    Uni<Void> publicar(
            String instanceId,
            RevisaoHumanaConformidade revisao);
}
