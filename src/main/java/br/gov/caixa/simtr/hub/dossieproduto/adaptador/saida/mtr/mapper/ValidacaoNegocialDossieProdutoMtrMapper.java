package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.validacaonegocial.ValidacaoNegocialDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ParecerApontamentoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VerificacaoValidacaoNegocialDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ValidacaoNegocialDossieProdutoMtrMapper {

    public ValidacaoNegocialDossieProdutoMtrRequest paraMtr(
            ComandoRegistroValidacaoNegocialDossieProduto comando
    ) {
        if (comando == null) {
            return null;
        }
        return new ValidacaoNegocialDossieProdutoMtrRequest(
                verificacoes(comando.verificacoes()),
                respostas(comando.respostasFormulario()));
    }

    private static List<ValidacaoNegocialDossieProdutoMtrRequest.Verificacao> verificacoes(
            List<VerificacaoValidacaoNegocialDossieProduto> verificacoes
    ) {
        if (verificacoes == null) {
            return null;
        }
        return verificacoes.stream()
                .map(ValidacaoNegocialDossieProdutoMtrMapper::verificacao)
                .toList();
    }

    private static ValidacaoNegocialDossieProdutoMtrRequest.Verificacao verificacao(
            VerificacaoValidacaoNegocialDossieProduto verificacao
    ) {
        if (verificacao == null) {
            return null;
        }
        return new ValidacaoNegocialDossieProdutoMtrRequest.Verificacao(
                verificacao.identificadorInstanciaDocumento(),
                verificacao.identificadorChecklist(),
                verificacao.versaoChecklist(),
                verificacao.analiseRealizada(),
                pareceres(verificacao.parecerApontamentos()),
                garantia(verificacao.garantia()),
                produto(verificacao.produto()),
                verificacao.previo());
    }

    private static List<ValidacaoNegocialDossieProdutoMtrRequest.ParecerApontamento> pareceres(
            List<ParecerApontamentoValidacaoNegocialDossieProduto> pareceres
    ) {
        if (pareceres == null) {
            return null;
        }
        return pareceres.stream()
                .map(ValidacaoNegocialDossieProdutoMtrMapper::parecer)
                .toList();
    }

    private static ValidacaoNegocialDossieProdutoMtrRequest.ParecerApontamento parecer(
            ParecerApontamentoValidacaoNegocialDossieProduto parecer
    ) {
        if (parecer == null) {
            return null;
        }
        return new ValidacaoNegocialDossieProdutoMtrRequest.ParecerApontamento(
                parecer.identificadorApontamento(),
                parecer.resultado(),
                parecer.comentario(),
                parecer.necessidadeReanalise(),
                parecer.indiceIa());
    }

    private static ValidacaoNegocialDossieProdutoMtrRequest.Garantia garantia(
            GarantiaValidacaoNegocialDossieProduto garantia
    ) {
        if (garantia == null) {
            return null;
        }
        return new ValidacaoNegocialDossieProdutoMtrRequest.Garantia(
                garantia.codigoBacen(), avalistas(garantia.clientesAvalistas()));
    }

    private static List<ValidacaoNegocialDossieProdutoMtrRequest.ClienteAvalista> avalistas(
            List<ClienteAvalistaValidacaoNegocialDossieProduto> avalistas
    ) {
        if (avalistas == null) {
            return null;
        }
        return avalistas.stream()
                .map(ValidacaoNegocialDossieProdutoMtrMapper::avalista)
                .toList();
    }

    private static ValidacaoNegocialDossieProdutoMtrRequest.ClienteAvalista avalista(
            ClienteAvalistaValidacaoNegocialDossieProduto avalista
    ) {
        if (avalista == null) {
            return null;
        }
        return new ValidacaoNegocialDossieProdutoMtrRequest.ClienteAvalista(
                avalista.cpf(), avalista.cnpj());
    }

    private static ValidacaoNegocialDossieProdutoMtrRequest.Produto produto(
            ProdutoValidacaoNegocialDossieProduto produto
    ) {
        if (produto == null) {
            return null;
        }
        return new ValidacaoNegocialDossieProdutoMtrRequest.Produto(
                produto.codigoOperacao(), produto.codigoModalidade());
    }

    private static List<ValidacaoNegocialDossieProdutoMtrRequest.RespostaFormulario> respostas(
            List<RespostaFormularioValidacaoNegocialDossieProduto> respostas
    ) {
        if (respostas == null) {
            return null;
        }
        return respostas.stream()
                .map(ValidacaoNegocialDossieProdutoMtrMapper::resposta)
                .toList();
    }

    private static ValidacaoNegocialDossieProdutoMtrRequest.RespostaFormulario resposta(
            RespostaFormularioValidacaoNegocialDossieProduto resposta
    ) {
        if (resposta == null) {
            return null;
        }
        return new ValidacaoNegocialDossieProdutoMtrRequest.RespostaFormulario(
                resposta.campoFormulario(),
                resposta.resposta(),
                resposta.opcoesSelecionadas());
    }
}
