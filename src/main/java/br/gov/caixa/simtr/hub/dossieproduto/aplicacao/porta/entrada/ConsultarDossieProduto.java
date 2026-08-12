package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.smallrye.mutiny.Uni;

public interface ConsultarDossieProduto {

    Uni<DossieProdutoConsultado> executar(IdentificadorDossieProduto identificador);
}
