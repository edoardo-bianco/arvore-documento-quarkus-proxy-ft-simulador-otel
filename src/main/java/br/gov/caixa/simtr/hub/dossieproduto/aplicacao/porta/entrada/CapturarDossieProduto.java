package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.smallrye.mutiny.Uni;

public interface CapturarDossieProduto {

    Uni<ResultadoCapturaDossieProduto> executar(IdentificadorDossieProduto identificador);
}
