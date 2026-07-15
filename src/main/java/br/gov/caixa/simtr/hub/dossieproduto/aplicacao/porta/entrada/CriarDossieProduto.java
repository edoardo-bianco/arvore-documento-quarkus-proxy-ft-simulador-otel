package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import io.smallrye.mutiny.Uni;

public interface CriarDossieProduto {

    Uni<ResultadoCriacaoDossieProduto> executar(ComandoCriacaoDossieProduto comando);
}
