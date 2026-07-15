package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario.FormularioDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario.FormularioDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.FormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoFormularioDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class FormularioDossieProdutoMtrMapper {

    public List<FormularioDossieProdutoMtrRequest> paraMtr(
            ComandoAtualizacaoFormularioDossieProduto comando
    ) {
        if (comando == null || comando.formularios() == null) {
            return null;
        }
        return comando.formularios().stream()
                .map(FormularioDossieProdutoMtrMapper::formulario)
                .toList();
    }

    public ResultadoAtualizacaoFormularioDossieProduto paraResultado(
            FormularioDossieProdutoMtrResponse resposta
    ) {
        if (resposta == null) {
            return null;
        }
        return new ResultadoAtualizacaoFormularioDossieProduto(resposta.id());
    }

    private static FormularioDossieProdutoMtrRequest formulario(
            FormularioDossieProduto formulario
    ) {
        if (formulario == null) {
            return null;
        }
        return new FormularioDossieProdutoMtrRequest(vinculo(formulario.vinculoDossie()));
    }

    private static FormularioDossieProdutoMtrRequest.VinculoDossie vinculo(
            VinculoFormularioDossieProduto vinculo
    ) {
        if (vinculo == null) {
            return null;
        }
        return new FormularioDossieProdutoMtrRequest.VinculoDossie(
                vinculo.fase(),
                cliente(vinculo.cliente()),
                produto(vinculo.produto()),
                garantia(vinculo.garantia()),
                respostas(vinculo.respostasFormulario()));
    }

    private static FormularioDossieProdutoMtrRequest.Cliente cliente(
            ClienteFormularioDossieProduto cliente
    ) {
        if (cliente == null) {
            return null;
        }
        return new FormularioDossieProdutoMtrRequest.Cliente(
                cliente.cpf(), cliente.cnpj(), cliente.tipoVinculo());
    }

    private static FormularioDossieProdutoMtrRequest.Produto produto(
            ProdutoFormularioDossieProduto produto
    ) {
        if (produto == null) {
            return null;
        }
        return new FormularioDossieProdutoMtrRequest.Produto(
                produto.codigoOperacao(), produto.codigoModalidade());
    }

    private static FormularioDossieProdutoMtrRequest.Garantia garantia(
            GarantiaFormularioDossieProduto garantia
    ) {
        if (garantia == null) {
            return null;
        }
        return new FormularioDossieProdutoMtrRequest.Garantia(
                garantia.codigoBacen(),
                garantia.produtoOperacao(),
                garantia.produtoModalidade(),
                avalistas(garantia.clientesAvalistas()));
    }

    private static List<FormularioDossieProdutoMtrRequest.ClienteAvalista> avalistas(
            List<ClienteAvalistaFormularioDossieProduto> avalistas
    ) {
        if (avalistas == null) {
            return null;
        }
        return avalistas.stream()
                .map(FormularioDossieProdutoMtrMapper::avalista)
                .toList();
    }

    private static FormularioDossieProdutoMtrRequest.ClienteAvalista avalista(
            ClienteAvalistaFormularioDossieProduto avalista
    ) {
        if (avalista == null) {
            return null;
        }
        return new FormularioDossieProdutoMtrRequest.ClienteAvalista(
                avalista.cpf(), avalista.cnpj());
    }

    private static List<FormularioDossieProdutoMtrRequest.Resposta> respostas(
            List<RespostaFormularioDossieProduto> respostas
    ) {
        if (respostas == null) {
            return null;
        }
        return respostas.stream()
                .map(FormularioDossieProdutoMtrMapper::resposta)
                .toList();
    }

    private static FormularioDossieProdutoMtrRequest.Resposta resposta(
            RespostaFormularioDossieProduto resposta
    ) {
        if (resposta == null) {
            return null;
        }
        return new FormularioDossieProdutoMtrRequest.Resposta(
                resposta.campoFormulario(),
                resposta.resposta(),
                resposta.opcoesSelecionadas(),
                resposta.excluir());
    }
}
