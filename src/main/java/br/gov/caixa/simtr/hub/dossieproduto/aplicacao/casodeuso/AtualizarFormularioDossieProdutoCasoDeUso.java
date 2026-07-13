package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.AtualizarFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import io.smallrye.mutiny.Uni;

public final class AtualizarFormularioDossieProdutoCasoDeUso
        implements AtualizarFormularioDossieProduto {

    private final SolicitarAtualizacaoFormularioDossieProduto portaSaida;

    public AtualizarFormularioDossieProdutoCasoDeUso(
            SolicitarAtualizacaoFormularioDossieProduto portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<ResultadoAtualizacaoFormularioDossieProduto> executar(
            ComandoAtualizacaoFormularioDossieProduto comando) {
        return portaSaida.atualizar(comando);
    }
}
