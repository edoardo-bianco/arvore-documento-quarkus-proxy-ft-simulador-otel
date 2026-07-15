package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import io.smallrye.mutiny.Uni;

public interface SolicitarCriacaoDossieProduto {

    Uni<ResultadoCriacaoDossieProduto> criar(ComandoCriacaoDossieProduto comando);
}
