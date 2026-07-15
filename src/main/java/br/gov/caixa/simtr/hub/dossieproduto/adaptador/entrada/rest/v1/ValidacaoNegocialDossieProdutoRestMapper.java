package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ParecerApontamentoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VerificacaoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoValidacaoNegocialClienteAvalistaDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoValidacaoNegocialGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoValidacaoNegocialParecerApontamentoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoValidacaoNegocialProdutoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoValidacaoNegocialRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoValidacaoNegocialVerificacaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ValidacaoNegocialDossieProdutoRequest;

import java.util.ArrayList;
import java.util.List;

final class ValidacaoNegocialDossieProdutoRestMapper {

    private ValidacaoNegocialDossieProdutoRestMapper() {
    }

    static ComandoRegistroValidacaoNegocialDossieProduto paraComando(
            Long identificadorDossieProduto,
            ValidacaoNegocialDossieProdutoRequest request
    ) {
        return new ComandoRegistroValidacaoNegocialDossieProduto(
                identificadorDossieProduto,
                request != null ? verificacoes(request.verificacoes()) : null,
                request != null ? respostas(request.respostasFormulario()) : null);
    }

    static Throwable paraExcecaoRest(FalhaRegistroValidacaoNegocialDossieProduto falha) {
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

    private static List<VerificacaoValidacaoNegocialDossieProduto> verificacoes(
            List<DossieProdutoValidacaoNegocialVerificacaoDto> verificacoes
    ) {
        if (verificacoes == null) {
            return null;
        }
        List<VerificacaoValidacaoNegocialDossieProduto> resultado =
                new ArrayList<>(verificacoes.size());
        for (DossieProdutoValidacaoNegocialVerificacaoDto verificacao : verificacoes) {
            resultado.add(verificacao(verificacao));
        }
        return resultado;
    }

    private static VerificacaoValidacaoNegocialDossieProduto verificacao(
            DossieProdutoValidacaoNegocialVerificacaoDto verificacao
    ) {
        if (verificacao == null) {
            return null;
        }
        return new VerificacaoValidacaoNegocialDossieProduto(
                verificacao.identificadorInstanciaDocumento(),
                verificacao.identificadorChecklist(),
                verificacao.versaoChecklist(),
                verificacao.analiseRealizada(),
                pareceres(verificacao.parecerApontamentos()),
                garantia(verificacao.garantia()),
                produto(verificacao.produto()),
                verificacao.previo());
    }

    private static List<ParecerApontamentoValidacaoNegocialDossieProduto> pareceres(
            List<DossieProdutoValidacaoNegocialParecerApontamentoDto> pareceres
    ) {
        if (pareceres == null) {
            return null;
        }
        List<ParecerApontamentoValidacaoNegocialDossieProduto> resultado =
                new ArrayList<>(pareceres.size());
        for (DossieProdutoValidacaoNegocialParecerApontamentoDto parecer : pareceres) {
            resultado.add(parecer != null
                    ? new ParecerApontamentoValidacaoNegocialDossieProduto(
                            parecer.identificadorApontamento(),
                            parecer.resultado(),
                            parecer.comentario(),
                            parecer.necessidadeReanalise(),
                            parecer.indiceIa())
                    : null);
        }
        return resultado;
    }

    private static GarantiaValidacaoNegocialDossieProduto garantia(
            DossieProdutoValidacaoNegocialGarantiaDto garantia
    ) {
        if (garantia == null) {
            return null;
        }
        return new GarantiaValidacaoNegocialDossieProduto(
                garantia.codigoBacen(), avalistas(garantia.clientesAvalistas()));
    }

    private static List<ClienteAvalistaValidacaoNegocialDossieProduto> avalistas(
            List<DossieProdutoValidacaoNegocialClienteAvalistaDto> avalistas
    ) {
        if (avalistas == null) {
            return null;
        }
        List<ClienteAvalistaValidacaoNegocialDossieProduto> resultado =
                new ArrayList<>(avalistas.size());
        for (DossieProdutoValidacaoNegocialClienteAvalistaDto avalista : avalistas) {
            resultado.add(avalista != null
                    ? new ClienteAvalistaValidacaoNegocialDossieProduto(
                            avalista.cpf(), avalista.cnpj())
                    : null);
        }
        return resultado;
    }

    private static ProdutoValidacaoNegocialDossieProduto produto(
            DossieProdutoValidacaoNegocialProdutoDto produto
    ) {
        return produto != null
                ? new ProdutoValidacaoNegocialDossieProduto(
                        produto.codigoOperacao(), produto.codigoModalidade())
                : null;
    }

    private static List<RespostaFormularioValidacaoNegocialDossieProduto> respostas(
            List<DossieProdutoValidacaoNegocialRespostaFormularioDto> respostas
    ) {
        if (respostas == null) {
            return null;
        }
        List<RespostaFormularioValidacaoNegocialDossieProduto> resultado =
                new ArrayList<>(respostas.size());
        for (DossieProdutoValidacaoNegocialRespostaFormularioDto resposta : respostas) {
            resultado.add(resposta != null
                    ? new RespostaFormularioValidacaoNegocialDossieProduto(
                            resposta.campoFormulario(),
                            resposta.resposta(),
                            resposta.opcoesSelecionadas())
                    : null);
        }
        return resultado;
    }

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
