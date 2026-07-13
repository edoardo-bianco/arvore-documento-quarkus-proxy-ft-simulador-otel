package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.smallrye.mutiny.Uni;

public interface IniciarOuAvancarWorkflowDossieProduto {

    Uni<ResultadoWorkflowDossieProduto> executar(IdentificadorDossieProduto identificador);
}
