package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.AtributoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.PropriedadeDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoDocumentoAtributoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoDocumentoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoDocumentoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoDocumentoPropriedadeDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.DossieProdutoDocumentoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.InclusaoDocumentoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.InclusaoDocumentoDossieProdutoResponse;

import java.util.ArrayList;
import java.util.List;

final class DocumentoDossieProdutoRestMapper {

    private DocumentoDossieProdutoRestMapper() {
    }

    static ComandoInclusaoDocumentoDossieProduto paraComando(
            Long identificadorDossieProduto,
            InclusaoDocumentoDossieProdutoRequest request
    ) {
        if (request == null) {
            return new ComandoInclusaoDocumentoDossieProduto(
                    identificadorDossieProduto,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        return new ComandoInclusaoDocumentoDossieProduto(
                identificadorDossieProduto,
                request.id(),
                request.pathStorage(),
                request.codigoGed(),
                request.objectStoreGed(),
                request.tipoDocumento(),
                vinculo(request.vinculoDossie()),
                atributos(request.atributos()),
                propriedades(request.propriedades()));
    }

    static InclusaoDocumentoDossieProdutoResponse paraResposta(
            ResultadoInclusaoDocumentoDossieProduto resultado
    ) {
        if (resultado == null) {
            return null;
        }
        return new InclusaoDocumentoDossieProdutoResponse(
                resultado.identificadorDocumento(),
                resultado.identificadorInstanciaDocumento());
    }

    static Throwable paraExcecaoRest(FalhaInclusaoDocumentoDossieProduto falha) {
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

    private static VinculoDocumentoDossieProduto vinculo(
            DossieProdutoDocumentoVinculoDossieDto vinculo
    ) {
        if (vinculo == null) {
            return null;
        }
        return new VinculoDocumentoDossieProduto(
                cliente(vinculo.cliente()),
                vinculo.elementoConteudo(),
                garantia(vinculo.garantia()));
    }

    private static ClienteDocumentoDossieProduto cliente(
            DossieProdutoDocumentoClienteDto cliente
    ) {
        return cliente != null
                ? new ClienteDocumentoDossieProduto(
                        cliente.cpf(), cliente.cnpj(), cliente.tipoVinculo())
                : null;
    }

    private static GarantiaDocumentoDossieProduto garantia(
            DossieProdutoDocumentoGarantiaDto garantia
    ) {
        if (garantia == null) {
            return null;
        }
        return new GarantiaDocumentoDossieProduto(
                garantia.codigoBacen(),
                garantia.produtoOperacao(),
                garantia.produtoModalidade(),
                clientes(garantia.clienteAvalista()));
    }

    private static List<ClienteDocumentoDossieProduto> clientes(
            List<DossieProdutoDocumentoClienteDto> clientes
    ) {
        if (clientes == null) {
            return null;
        }
        List<ClienteDocumentoDossieProduto> resultado = new ArrayList<>(clientes.size());
        for (DossieProdutoDocumentoClienteDto cliente : clientes) {
            resultado.add(cliente(cliente));
        }
        return resultado;
    }

    private static List<AtributoDocumentoDossieProduto> atributos(
            List<DossieProdutoDocumentoAtributoDto> atributos
    ) {
        if (atributos == null) {
            return null;
        }
        List<AtributoDocumentoDossieProduto> resultado = new ArrayList<>(atributos.size());
        for (DossieProdutoDocumentoAtributoDto atributo : atributos) {
            resultado.add(atributo != null
                    ? new AtributoDocumentoDossieProduto(
                            atributo.chave(),
                            atributo.valor(),
                            atributo.objeto(),
                            atributo.opcoesSelecionadas())
                    : null);
        }
        return resultado;
    }

    private static List<PropriedadeDocumentoDossieProduto> propriedades(
            List<DossieProdutoDocumentoPropriedadeDto> propriedades
    ) {
        if (propriedades == null) {
            return null;
        }
        List<PropriedadeDocumentoDossieProduto> resultado =
                new ArrayList<>(propriedades.size());
        for (DossieProdutoDocumentoPropriedadeDto propriedade : propriedades) {
            resultado.add(propriedade != null
                    ? new PropriedadeDocumentoDossieProduto(
                            propriedade.chave(), propriedade.valor(), propriedade.objeto())
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
