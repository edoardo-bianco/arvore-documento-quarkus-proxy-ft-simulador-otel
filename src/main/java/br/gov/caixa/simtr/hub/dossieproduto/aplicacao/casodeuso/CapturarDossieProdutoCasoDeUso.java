package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CapturarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.smallrye.mutiny.Uni;

public final class CapturarDossieProdutoCasoDeUso implements CapturarDossieProduto {

    private final SolicitarCapturaDossieProduto portaSaida;

    public CapturarDossieProdutoCasoDeUso(SolicitarCapturaDossieProduto portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<ResultadoCapturaDossieProduto> executar(IdentificadorDossieProduto identificador) {
        return portaSaida.capturar(identificador);
    }
}
