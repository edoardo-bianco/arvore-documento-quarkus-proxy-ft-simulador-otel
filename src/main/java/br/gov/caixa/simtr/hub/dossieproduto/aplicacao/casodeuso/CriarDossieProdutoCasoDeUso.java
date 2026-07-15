package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CriarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import io.smallrye.mutiny.Uni;

public final class CriarDossieProdutoCasoDeUso implements CriarDossieProduto {

    private final SolicitarCriacaoDossieProduto portaSaida;

    public CriarDossieProdutoCasoDeUso(SolicitarCriacaoDossieProduto portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<ResultadoCriacaoDossieProduto> executar(ComandoCriacaoDossieProduto comando) {
        return portaSaida.criar(comando);
    }
}
