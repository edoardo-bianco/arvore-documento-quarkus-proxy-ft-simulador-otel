package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento.DocumentoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento.DocumentoDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.AtributoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.PropriedadeDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoDocumentoDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class DocumentoDossieProdutoMtrMapper {

    public DocumentoDossieProdutoMtrRequest paraMtr(
            ComandoInclusaoDocumentoDossieProduto comando
    ) {
        if (comando == null) {
            return null;
        }
        return new DocumentoDossieProdutoMtrRequest(
                comando.identificadorDocumento(),
                comando.caminhoArmazenamento(),
                comando.codigoGed(),
                comando.repositorioGed(),
                comando.tipoDocumento(),
                vinculo(comando.vinculoDossie()),
                atributos(comando.atributos()),
                propriedades(comando.propriedades()));
    }

    public ResultadoInclusaoDocumentoDossieProduto paraResultado(
            DocumentoDossieProdutoMtrResponse resposta
    ) {
        if (resposta == null) {
            return null;
        }
        return new ResultadoInclusaoDocumentoDossieProduto(
                resposta.idDocumento(), resposta.idInstanciaDocumento());
    }

    private static DocumentoDossieProdutoMtrRequest.VinculoDossie vinculo(
            VinculoDocumentoDossieProduto vinculo
    ) {
        if (vinculo == null) {
            return null;
        }
        return new DocumentoDossieProdutoMtrRequest.VinculoDossie(
                cliente(vinculo.cliente()),
                vinculo.elementoConteudo(),
                garantia(vinculo.garantia()));
    }

    private static DocumentoDossieProdutoMtrRequest.Cliente cliente(
            ClienteDocumentoDossieProduto cliente
    ) {
        if (cliente == null) {
            return null;
        }
        return new DocumentoDossieProdutoMtrRequest.Cliente(
                cliente.cpf(), cliente.cnpj(), cliente.tipoVinculo());
    }

    private static DocumentoDossieProdutoMtrRequest.Garantia garantia(
            GarantiaDocumentoDossieProduto garantia
    ) {
        if (garantia == null) {
            return null;
        }
        return new DocumentoDossieProdutoMtrRequest.Garantia(
                garantia.codigoBacen(),
                garantia.produtoOperacao(),
                garantia.produtoModalidade(),
                clientes(garantia.clientesAvalistas()));
    }

    private static List<DocumentoDossieProdutoMtrRequest.Cliente> clientes(
            List<ClienteDocumentoDossieProduto> clientes
    ) {
        if (clientes == null) {
            return null;
        }
        return clientes.stream()
                .map(DocumentoDossieProdutoMtrMapper::cliente)
                .toList();
    }

    private static List<DocumentoDossieProdutoMtrRequest.Atributo> atributos(
            List<AtributoDocumentoDossieProduto> atributos
    ) {
        if (atributos == null) {
            return null;
        }
        return atributos.stream()
                .map(DocumentoDossieProdutoMtrMapper::atributo)
                .toList();
    }

    private static DocumentoDossieProdutoMtrRequest.Atributo atributo(
            AtributoDocumentoDossieProduto atributo
    ) {
        if (atributo == null) {
            return null;
        }
        return new DocumentoDossieProdutoMtrRequest.Atributo(
                atributo.chave(),
                atributo.valor(),
                atributo.objeto(),
                atributo.opcoesSelecionadas());
    }

    private static List<DocumentoDossieProdutoMtrRequest.Propriedade> propriedades(
            List<PropriedadeDocumentoDossieProduto> propriedades
    ) {
        if (propriedades == null) {
            return null;
        }
        return propriedades.stream()
                .map(DocumentoDossieProdutoMtrMapper::propriedade)
                .toList();
    }

    private static DocumentoDossieProdutoMtrRequest.Propriedade propriedade(
            PropriedadeDocumentoDossieProduto propriedade
    ) {
        if (propriedade == null) {
            return null;
        }
        return new DocumentoDossieProdutoMtrRequest.Propriedade(
                propriedade.chave(), propriedade.valor(), propriedade.objeto());
    }
}
