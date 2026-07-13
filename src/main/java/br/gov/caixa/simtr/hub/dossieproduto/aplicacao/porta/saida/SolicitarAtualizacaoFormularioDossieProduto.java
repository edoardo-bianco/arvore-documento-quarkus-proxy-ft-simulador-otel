package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import io.smallrye.mutiny.Uni;

public interface SolicitarAtualizacaoFormularioDossieProduto {

    Uni<ResultadoAtualizacaoFormularioDossieProduto> atualizar(
            ComandoAtualizacaoFormularioDossieProduto comando);
}
