package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.entrada.rest.v1.dto.GestaoDocumentoCredencialContainerResponse;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.erro.FalhaObtencaoCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import java.util.ArrayList;
import java.util.List;

final class GestaoDocumentoRestMapper {

    private GestaoDocumentoRestMapper() {
    }

    static GestaoDocumentoCredencialContainerResponse paraResposta(
            CredencialContainer credencial
    ) {
        if (credencial == null) {
            return null;
        }
        return new GestaoDocumentoCredencialContainerResponse(
                credencial.sas(),
                credencial.validade(),
                credencial.urlStorage(),
                credencial.nomeContainer()
        );
    }

    static Throwable paraExcecaoRest(FalhaObtencaoCredencialContainer falha) {
        int status = falha.status() != null ? falha.status() : 500;
        ErroPadraoDto erro = new ErroPadraoDto(
                falha.status(),
                falha.recurso(),
                falha.idErro(),
                falha.codigoErro(),
                mensagens(falha.mensagens()),
                falha.detalhe(),
                falha.stacktraceExterno()
        );

        return switch (falha.tipo()) {
            case NEGOCIO -> new MtrBusinessErrorException(status, erro);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(status, erro);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT ->
                    new MtrServerErrorException(status, erro);
        };
    }

    private static List<ErroMensagemDto> mensagens(List<String> mensagens) {
        if (mensagens == null) {
            return null;
        }
        List<ErroMensagemDto> resposta = new ArrayList<>(mensagens.size());
        for (String mensagem : mensagens) {
            resposta.add(mensagem != null ? new ErroMensagemDto(mensagem) : null);
        }
        return resposta;
    }
}
