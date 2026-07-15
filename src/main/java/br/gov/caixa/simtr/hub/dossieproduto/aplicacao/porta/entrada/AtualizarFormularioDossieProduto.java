package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import io.smallrye.mutiny.Uni;

public interface AtualizarFormularioDossieProduto {

    Uni<ResultadoAtualizacaoFormularioDossieProduto> executar(
            ComandoAtualizacaoFormularioDossieProduto comando);
}
