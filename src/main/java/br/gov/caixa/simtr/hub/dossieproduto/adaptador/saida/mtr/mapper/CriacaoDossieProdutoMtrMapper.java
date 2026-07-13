package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.criacao.CriacaoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.criacao.CriacaoDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteRelacionadoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CriacaoDossieProdutoMtrMapper {

    public CriacaoDossieProdutoMtrRequest paraMtr(ComandoCriacaoDossieProduto comando) {
        if (comando == null) {
            return null;
        }
        return new CriacaoDossieProdutoMtrRequest(
                comando.processo(),
                comando.chaveCorrelacaoCanal(),
                comando.numeroNegocio(),
                clientes(comando.clientes()));
    }

    public ResultadoCriacaoDossieProduto paraResultado(CriacaoDossieProdutoMtrResponse resposta) {
        if (resposta == null) {
            return null;
        }
        return new ResultadoCriacaoDossieProduto(resposta.id());
    }

    private static List<CriacaoDossieProdutoMtrRequest.Cliente> clientes(
            List<ClienteCriacaoDossieProduto> clientes
    ) {
        if (clientes == null) {
            return null;
        }
        return clientes.stream().map(CriacaoDossieProdutoMtrMapper::cliente).toList();
    }

    private static CriacaoDossieProdutoMtrRequest.Cliente cliente(
            ClienteCriacaoDossieProduto cliente
    ) {
        if (cliente == null) {
            return null;
        }
        return new CriacaoDossieProdutoMtrRequest.Cliente(
                cliente.cpf(),
                cliente.cnpj(),
                cliente.tipoVinculo(),
                clienteRelacionado(cliente.clienteRelacionado()),
                cliente.sequenciaTitularidade());
    }

    private static CriacaoDossieProdutoMtrRequest.ClienteRelacionado clienteRelacionado(
            ClienteRelacionadoCriacaoDossieProduto relacionado
    ) {
        if (relacionado == null) {
            return null;
        }
        return new CriacaoDossieProdutoMtrRequest.ClienteRelacionado(
                relacionado.cpf(), relacionado.cnpj());
    }
}
