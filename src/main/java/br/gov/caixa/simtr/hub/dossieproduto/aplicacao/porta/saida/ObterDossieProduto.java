package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.smallrye.mutiny.Uni;

public interface ObterDossieProduto {

    Uni<DossieProdutoConsultado> obter(IdentificadorDossieProduto identificador);
}
