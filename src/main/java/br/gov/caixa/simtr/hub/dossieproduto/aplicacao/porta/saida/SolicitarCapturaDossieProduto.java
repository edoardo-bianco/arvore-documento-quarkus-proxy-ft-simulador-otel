package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.smallrye.mutiny.Uni;

public interface SolicitarCapturaDossieProduto {

    Uni<ResultadoCapturaDossieProduto> capturar(IdentificadorDossieProduto identificador);
}
