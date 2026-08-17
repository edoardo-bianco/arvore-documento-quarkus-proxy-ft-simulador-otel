package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.CapturaDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;

final class CapturaDossieProdutoRestMapper {

    private static final int STATUS_ERRO_INTERNO = 500;
    private static final String RECURSO_HUB = "simtr-hub";
    private static final String CODIGO_ERRO_INTERNO = "ARVDOCP9999";
    private static final String MENSAGEM_ERRO_INTERNO =
            "Erro interno ao processar a requisição.";
    private static final String MENSAGEM_OBSERVAVEL =
            "Falha ao capturar dossie produto";

    private CapturaDossieProdutoRestMapper() {
    }

    static CapturaDossieProdutoResponse paraResposta(ResultadoCapturaDossieProduto resultado) {
        if (resultado == null) {
            return null;
        }
        return new CapturaDossieProdutoResponse(resultado.identificadorDossieProduto());
    }

    static Throwable paraExcecaoRest(FalhaCapturaDossieProduto falha) {
        if (falha.status() == null) {
            return erroInternoSeguro();
        }

        int status = falha.status();
        ErroPadraoDto erro = new ErroPadraoDto(
                falha.status(),
                falha.recurso(),
                falha.idErro(),
                falha.codigoErro(),
                mensagens(falha.mensagens()),
                falha.detalhe(),
                falha.stacktraceExterno());

        return switch (falha.tipo()) {
            case NEGOCIO -> new MtrBusinessErrorException(
                    status, erro, MENSAGEM_OBSERVAVEL);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(
                    status, erro, MENSAGEM_OBSERVAVEL);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT ->
                    new MtrServerErrorException(status, erro, MENSAGEM_OBSERVAVEL);
        };
    }

    private static MtrServerErrorException erroInternoSeguro() {
        ErroPadraoDto erro = new ErroPadraoDto(
                STATUS_ERRO_INTERNO,
                RECURSO_HUB,
                UUID.randomUUID().toString(),
                CODIGO_ERRO_INTERNO,
                List.of(new ErroMensagemDto(MENSAGEM_ERRO_INTERNO)),
                null,
                null);
        return new MtrServerErrorException(STATUS_ERRO_INTERNO, erro);
    }

    @SuppressWarnings("java:S1168") // Null preserva a ausência da lista no contrato MTR válido.
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
