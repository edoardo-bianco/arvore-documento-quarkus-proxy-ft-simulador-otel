package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ConsultaDocumentosDossieProdutoQueryParams;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.ConsultaDocumentosDossieProdutoResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class ConsultaDocumentosDossieProdutoRestMapper {

    private ConsultaDocumentosDossieProdutoRestMapper() {
    }

    static CriteriosConsultaDocumentosDossieProduto paraCriterios(
            Long identificador,
            ConsultaDocumentosDossieProdutoQueryParams query
    ) {
        return new CriteriosConsultaDocumentosDossieProduto(
                new IdentificadorDossieProduto(identificador),
                query != null ? query.cnpj() : null,
                query != null ? query.cpf() : null,
                query != null ? query.fase() : null,
                query != null ? query.incluiArmazenamento() : null,
                query != null ? query.incluiAssinaturas() : null,
                query != null ? query.incluiAtributos() : null,
                query != null ? query.incluiConformidade() : null,
                query != null ? query.incluiOutsourcing() : null,
                query != null ? query.incluiPropriedades() : null,
                query != null ? query.incluiUrl() : null,
                query != null ? query.ipUsuario() : null,
                query != null ? query.tipologia() : null);
    }

    static List<ConsultaDocumentosDossieProdutoResponse> paraResposta(
            List<DocumentoDossieProdutoConsultado> documentos
    ) {
        return mapear(documentos, ConsultaDocumentosDossieProdutoRestMapper::documento);
    }

    static Throwable paraExcecaoRest(FalhaConsultaDocumentosDossieProduto falha) {
        int status = falha.status() != null ? falha.status() : 500;
        String mensagemObservavel = falha.getMessage();
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
                    status, erro, mensagemObservavel);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(
                    status, erro, mensagemObservavel);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT ->
                    new MtrServerErrorException(status, erro, mensagemObservavel);
        };
    }

    private static ConsultaDocumentosDossieProdutoResponse documento(
            DocumentoDossieProdutoConsultado origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse(
                origem.idInstanciaDocumento(),
                origem.idDocumento(),
                origem.codigoGed(),
                origem.dataHoraCaptura(),
                origem.dataHoraValidade(),
                origem.matriculaCaptura(),
                tipoDocumento(origem.tipoDocumento()),
                origem.situacaoDocumento(),
                vinculo(origem.vinculoDossie()),
                origem.url(),
                mapear(origem.atributos(), ConsultaDocumentosDossieProdutoRestMapper::atributo),
                mapear(
                        origem.assinaturasDigitais(),
                        ConsultaDocumentosDossieProdutoRestMapper::assinatura),
                mapear(
                        origem.conformidades(),
                        ConsultaDocumentosDossieProdutoRestMapper::conformidade),
                mapear(
                        origem.propriedades(),
                        ConsultaDocumentosDossieProdutoRestMapper::propriedade),
                mapear(
                        origem.outsourcings(),
                        ConsultaDocumentosDossieProdutoRestMapper::outsourcing),
                mapear(
                        origem.armazenamentos(),
                        ConsultaDocumentosDossieProdutoRestMapper::armazenamento));
    }

    private static ConsultaDocumentosDossieProdutoResponse.TipoDocumento tipoDocumento(
            DocumentoDossieProdutoConsultado.TipoDocumento origem
    ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse.TipoDocumento(
                        origem.id(), origem.nome(), origem.codigoTipologia(), origem.ativo())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.VinculoDossie vinculo(
            DocumentoDossieProdutoConsultado.VinculoDossie origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.VinculoDossie(
                cliente(origem.cliente()),
                produto(origem.produto()),
                garantia(origem.garantia()),
                fase(origem.fase()),
                processo(origem.processo()));
    }

    private static ConsultaDocumentosDossieProdutoResponse.Cliente cliente(
            DocumentoDossieProdutoConsultado.Cliente origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.Cliente(
                origem.cpf(),
                origem.cnpj(),
                origem.nome(),
                origem.razaoSocial(),
                origem.tipoVinculo(),
                origem.identificadorNegocialVinculo(),
                origem.principal());
    }

    private static ConsultaDocumentosDossieProdutoResponse.Produto produto(
            DocumentoDossieProdutoConsultado.Produto origem
    ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse.Produto(
                        origem.id(), origem.nome(), origem.modalidade(), origem.operacao())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.Garantia garantia(
            DocumentoDossieProdutoConsultado.Garantia origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.Garantia(
                origem.id(),
                origem.nome(),
                origem.codigoBacen(),
                produto(origem.produto()),
                mapear(
                        origem.clientesAvalistas(),
                        ConsultaDocumentosDossieProdutoRestMapper::clienteAvalista));
    }

    private static ConsultaDocumentosDossieProdutoResponse.ClienteAvalista clienteAvalista(
            DocumentoDossieProdutoConsultado.ClienteAvalista origem
    ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse.ClienteAvalista(
                        origem.cpf(), origem.cnpj())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.Fase fase(
            DocumentoDossieProdutoConsultado.Fase origem
    ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse.Fase(
                        origem.id(), origem.nome(), origem.identificadorNegocial())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.Processo processo(
            DocumentoDossieProdutoConsultado.Processo origem
    ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse.Processo(
                        origem.id(), origem.nome(), origem.identificadorNegocial(),
                        origem.macroprocesso())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.Atributo atributo(
            DocumentoDossieProdutoConsultado.Atributo origem
    ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse.Atributo(
                        origem.chave(), origem.valor(), origem.opcoesSelecionadas())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.AssinaturaDigital assinatura(
            DocumentoDossieProdutoConsultado.AssinaturaDigital origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.AssinaturaDigital(
                origem.dataAssinatura(),
                origem.emissor(),
                origem.cpf(),
                origem.cnpj(),
                origem.nome(),
                origem.cpfResponsavelPj(),
                origem.nomeResponsavelPj());
    }

    private static ConsultaDocumentosDossieProdutoResponse.Conformidade conformidade(
            DocumentoDossieProdutoConsultado.Conformidade origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.Conformidade(
                origem.id(),
                origem.tipoConformidade(),
                origem.unidadeConformidade(),
                origem.dataHoraVerificacao(),
                origem.dossieProduto(),
                origem.fornecedor(),
                origem.resultado(),
                checklist(origem.checklist()));
    }

    private static ConsultaDocumentosDossieProdutoResponse.Checklist checklist(
            DocumentoDossieProdutoConsultado.Checklist origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.Checklist(
                origem.id(),
                origem.identificadorNegocial(),
                origem.nome(),
                origem.versao(),
                mapear(
                        origem.apontamentos(),
                        ConsultaDocumentosDossieProdutoRestMapper::apontamento));
    }

    private static ConsultaDocumentosDossieProdutoResponse.Apontamento apontamento(
            DocumentoDossieProdutoConsultado.Apontamento origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.Apontamento(
                origem.id(),
                origem.identificadorNegocial(),
                origem.nome(),
                origem.tipo(),
                origem.complexidade(),
                origem.aprovado(),
                origem.orientacao(),
                origem.comentario());
    }

    private static ConsultaDocumentosDossieProdutoResponse.Propriedade propriedade(
            DocumentoDossieProdutoConsultado.Propriedade origem
    ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse.Propriedade(
                        origem.chave(), origem.valor())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.Outsourcing outsourcing(
            DocumentoDossieProdutoConsultado.Outsourcing origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.Outsourcing(
                origem.solicitacao(),
                origem.processo(),
                origem.analiseConjunta(),
                origem.codigoControle(),
                origem.codigoFornecedor(),
                origem.siglaFornecedor(),
                origem.siglaCanal(),
                origem.codigoCanal(),
                origem.retornoAgrupado(),
                origem.dataHoraEnvio(),
                origem.statusEnvio(),
                origem.solicitacaoTratamentoImagem(),
                origem.solicitacaoExtracao(),
                origem.solicitacaoValidacaoNegocial(),
                origem.solicitacaoClassificacao(),
                origem.solicitacaoAvaliacaoCadastral(),
                origem.solicitacaoAvaliacaoAutenticidade(),
                origem.solicitacaoGrafoscopia(),
                origem.solicitacaoValidacaoExterna(),
                origem.solicitacaoConsultaExterna(),
                origem.janelaExtracao(),
                origem.dataHoraRetornoClassificacao(),
                origem.dataHoraRetornoExtracao(),
                origem.dataHoraRetornoValidacaoNegocial(),
                origem.dataHoraRetornoGrafoscopia(),
                origem.dataHoraRetornoAvaliacaoAutenticidade(),
                origem.dataHoraRetornoImagemTratada(),
                origem.dataHoraValidacaoExterna(),
                origem.dataConsultaExterna(),
                origem.dataAvaliacaoCadastral(),
                origem.resultadoIndiceAvaliacaoAutenticidade(),
                origem.resultadoIndiceGrafoscopia(),
                origem.resultadoCodigoRejeicao(),
                origem.resultadoDescricaoRejeicao(),
                origem.resultadoConsultaExterna(),
                origem.resultadoClassificacao(),
                origem.resultadoExtracao(),
                resultadoValidacao(origem.resultadoValidacaoNegocial()));
    }

    private static ConsultaDocumentosDossieProdutoResponse.ResultadoValidacaoNegocial
            resultadoValidacao(
                    DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial origem
            ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.ResultadoValidacaoNegocial(
                origem.identificadorChecklist(),
                origem.versaoChecklist(),
                mapear(
                        origem.apontamentos(),
                        ConsultaDocumentosDossieProdutoRestMapper::resultadoApontamento));
    }

    private static ConsultaDocumentosDossieProdutoResponse
            .ResultadoValidacaoNegocialApontamento resultadoApontamento(
                    DocumentoDossieProdutoConsultado
                            .ResultadoValidacaoNegocialApontamento origem
            ) {
        return origem != null
                ? new ConsultaDocumentosDossieProdutoResponse
                        .ResultadoValidacaoNegocialApontamento(
                                origem.identificadorApontamento(),
                                origem.aprovado(),
                                origem.comentario())
                : null;
    }

    private static ConsultaDocumentosDossieProdutoResponse.Armazenamento armazenamento(
            DocumentoDossieProdutoConsultado.Armazenamento origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ConsultaDocumentosDossieProdutoResponse.Armazenamento(
                origem.id(),
                origem.dataHoraArmazenamento(),
                origem.tipoArmazenamento(),
                origem.pathStorage(),
                origem.objectStoreGed(),
                origem.codigoGed(),
                origem.dataHoraPrevisaoExclusao(),
                origem.dataHoraExclusao());
    }

    private static List<ErroMensagemDto> mensagens(List<String> mensagens) {
        return mapear(
                mensagens,
                mensagem -> mensagem != null ? new ErroMensagemDto(mensagem) : null);
    }

    @SuppressWarnings("java:S1168") // Null distingue lista ausente de lista vazia no contrato.
    private static <O, D> List<D> mapear(List<O> origens, Function<O, D> conversor) {
        if (origens == null) {
            return null;
        }
        List<D> destinos = new ArrayList<>(origens.size());
        for (O origem : origens) {
            destinos.add(conversor.apply(origem));
        }
        return destinos;
    }
}
