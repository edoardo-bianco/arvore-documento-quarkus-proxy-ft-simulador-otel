package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import io.smallrye.mutiny.Uni;

public interface RegistrarValidacaoNegocialDossieProduto {

    Uni<Void> executar(ComandoRegistroValidacaoNegocialDossieProduto comando);
}
