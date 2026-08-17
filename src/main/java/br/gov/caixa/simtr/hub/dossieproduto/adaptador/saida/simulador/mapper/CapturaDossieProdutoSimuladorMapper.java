package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.CapturaDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CapturaDossieProdutoSimuladorMapper {

    public ResultadoCapturaDossieProduto paraResultado(
            CapturaDossieProdutoSimuladorResponse resposta
    ) {
        if (resposta == null) {
            return null;
        }
        return new ResultadoCapturaDossieProduto(resposta.id());
    }
}
