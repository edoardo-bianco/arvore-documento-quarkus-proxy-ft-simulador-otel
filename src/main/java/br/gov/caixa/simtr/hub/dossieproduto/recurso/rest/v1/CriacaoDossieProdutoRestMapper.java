package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteRelacionadoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.CriacaoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.CriacaoDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteRelacionadoDto;

import java.util.ArrayList;
import java.util.List;

final class CriacaoDossieProdutoRestMapper {

    private CriacaoDossieProdutoRestMapper() {
    }

    static ComandoCriacaoDossieProduto paraComando(CriacaoDossieProdutoRequest request) {
        if (request == null) {
            return null;
        }
        return new ComandoCriacaoDossieProduto(
                request.processo(),
                request.chaveCorrelacaoCanal(),
                request.numeroNegocio(),
                clientes(request.clientes()));
    }

    static CriacaoDossieProdutoResponse paraResposta(ResultadoCriacaoDossieProduto resultado) {
        if (resultado == null) {
            return null;
        }
        return new CriacaoDossieProdutoResponse(resultado.identificadorDossieProduto());
    }

    static Throwable paraExcecaoRest(FalhaCriacaoDossieProduto falha) {
        int status = falha.status() != null ? falha.status() : 500;
        ErroPadraoDto erro = new ErroPadraoDto(
                falha.status(),
                falha.recurso(),
                falha.idErro(),
                falha.codigoErro(),
                mensagens(falha.mensagens()),
                falha.detalhe(),
                falha.stacktraceExterno());

        return switch (falha.tipo()) {
            case NEGOCIO -> new MtrBusinessErrorException(status, erro);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(status, erro);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT -> new MtrServerErrorException(status, erro);
        };
    }

    private static List<ClienteCriacaoDossieProduto> clientes(List<DossieProdutoClienteDto> clientes) {
        if (clientes == null) {
            return null;
        }
        List<ClienteCriacaoDossieProduto> resultado = new ArrayList<>(clientes.size());
        for (DossieProdutoClienteDto cliente : clientes) {
            resultado.add(cliente != null ? cliente(cliente) : null);
        }
        return resultado;
    }

    private static ClienteCriacaoDossieProduto cliente(DossieProdutoClienteDto cliente) {
        return new ClienteCriacaoDossieProduto(
                cliente.cpf(),
                cliente.cnpj(),
                cliente.tipoVinculo(),
                clienteRelacionado(cliente.clienteRelacionado()),
                cliente.sequenciaTitularidade());
    }

    private static ClienteRelacionadoCriacaoDossieProduto clienteRelacionado(
            DossieProdutoClienteRelacionadoDto cliente) {
        return cliente != null
                ? new ClienteRelacionadoCriacaoDossieProduto(cliente.cpf(), cliente.cnpj())
                : null;
    }

    private static List<ErroMensagemDto> mensagens(List<String> mensagens) {
        if (mensagens == null) {
            return null;
        }
        List<ErroMensagemDto> resultado = new ArrayList<>(mensagens.size());
        for (String mensagem : mensagens) {
            resultado.add(mensagem != null ? new ErroMensagemDto(mensagem) : null);
        }
        return resultado;
    }
}
