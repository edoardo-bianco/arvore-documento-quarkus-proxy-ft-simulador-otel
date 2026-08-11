package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.AlteracaoProdutoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;

import java.util.ArrayList;
import java.util.List;

final class ProdutoDossieProdutoRestMapper {

    private ProdutoDossieProdutoRestMapper() {
    }

    static ComandoAlteracaoProdutosContratadosDossieProduto paraComando(
            Long identificadorDossieProduto,
            List<AlteracaoProdutoDossieProdutoRequest> request
    ) {
        return new ComandoAlteracaoProdutosContratadosDossieProduto(
                identificadorDossieProduto,
                produtos(request));
    }

    static Throwable paraExcecaoRest(FalhaAlteracaoProdutosContratadosDossieProduto falha) {
        int status = falha.status() != null ? falha.status() : 500;
        ErroPadraoDto erro = new ErroPadraoDto(
                falha.status(),
                falha.recurso(),
                falha.idErro(),
                falha.codigoErro(),
                mensagensErro(falha.mensagens()),
                falha.detalhe(),
                falha.stacktraceExterno());

        return switch (falha.tipo()) {
            case NEGOCIO -> new MtrBusinessErrorException(status, erro);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(status, erro);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT ->
                    new MtrServerErrorException(status, erro);
        };
    }

    @SuppressWarnings("java:S1168") // Null distingue corpo ausente de lista vazia na borda REST.
    private static List<ProdutoContratadoDossieProduto> produtos(
            List<AlteracaoProdutoDossieProdutoRequest> request
    ) {
        if (request == null) {
            return null;
        }
        List<ProdutoContratadoDossieProduto> resultado = new ArrayList<>(request.size());
        for (AlteracaoProdutoDossieProdutoRequest produto : request) {
            resultado.add(produto != null
                    ? new ProdutoContratadoDossieProduto(
                            produto.codigoOperacao(),
                            produto.codigoModalidade(),
                            produto.excluir())
                    : null);
        }
        return resultado;
    }

    @SuppressWarnings("java:S1168") // Null preserva a ausência da coleção recebida do MTR.
    private static List<ErroMensagemDto> mensagensErro(List<String> mensagens) {
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
