package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ProdutoDossieProdutoSimuladorResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProdutoDossieProdutoSimuladorMapper {

    @SuppressWarnings("java:S1172") // O parâmetro explicita a tradução DTO do simulador -> Void.
    public Void paraResultado(ProdutoDossieProdutoSimuladorResponse resposta) {
        return null;
    }
}
