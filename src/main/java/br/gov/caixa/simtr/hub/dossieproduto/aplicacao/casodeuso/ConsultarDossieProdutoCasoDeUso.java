package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.smallrye.mutiny.Uni;

public final class ConsultarDossieProdutoCasoDeUso implements ConsultarDossieProduto {

    private final ObterDossieProduto portaSaida;

    public ConsultarDossieProdutoCasoDeUso(ObterDossieProduto portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<DossieProdutoConsultado> executar(IdentificadorDossieProduto identificador) {
        return portaSaida.obter(identificador);
    }
}
