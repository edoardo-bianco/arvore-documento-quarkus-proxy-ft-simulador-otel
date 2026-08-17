package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.captura.CapturaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CapturaDossieProdutoMtrMapper {

    public ResultadoCapturaDossieProduto paraResultado(
            CapturaDossieProdutoMtrResponse resposta
    ) {
        if (resposta == null) {
            return null;
        }
        return new ResultadoCapturaDossieProduto(resposta.id());
    }
}
