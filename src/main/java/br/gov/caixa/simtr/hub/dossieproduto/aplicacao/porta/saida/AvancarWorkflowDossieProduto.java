package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.smallrye.mutiny.Uni;

public interface AvancarWorkflowDossieProduto {

    Uni<ResultadoWorkflowDossieProduto> avancar(IdentificadorDossieProduto identificador);
}
