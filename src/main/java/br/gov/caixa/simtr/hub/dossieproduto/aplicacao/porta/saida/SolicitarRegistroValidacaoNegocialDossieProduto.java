package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import io.smallrye.mutiny.Uni;

public interface SolicitarRegistroValidacaoNegocialDossieProduto {

    Uni<Void> registrar(ComandoRegistroValidacaoNegocialDossieProduto comando);
}
