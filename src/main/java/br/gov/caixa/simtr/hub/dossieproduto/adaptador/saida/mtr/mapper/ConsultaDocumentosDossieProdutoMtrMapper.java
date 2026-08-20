package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ConsultaDocumentosDossieProdutoMtrMapper {

    public List<DocumentoDossieProdutoConsultado> paraDominio(
            List<ConsultaDocumentosDossieProdutoMtrResponse> respostas
    ) {
        if (respostas == null) {
            return List.of();
        }
        return mapear(respostas, ConsultaDocumentosDossieProdutoMtrMapper::documento);
    }

    private static DocumentoDossieProdutoConsultado documento(
            ConsultaDocumentosDossieProdutoMtrResponse origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado(
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
                mapear(origem.atributos(),
                        ConsultaDocumentosDossieProdutoMtrMapper::atributo),
                mapear(origem.assinaturasDigitais(),
                        ConsultaDocumentosDossieProdutoMtrMapper::assinatura),
                mapear(origem.conformidade(),
                        ConsultaDocumentosDossieProdutoMtrMapper::conformidade),
                mapear(origem.propriedades(),
                        ConsultaDocumentosDossieProdutoMtrMapper::propriedade),
                mapear(origem.outsourcing(),
                        ConsultaDocumentosDossieProdutoMtrMapper::outsourcing),
                mapear(origem.armazenamento(),
                        ConsultaDocumentosDossieProdutoMtrMapper::armazenamento));
    }

    private static DocumentoDossieProdutoConsultado.TipoDocumento tipoDocumento(
            ConsultaDocumentosDossieProdutoMtrResponse.TipoDocumento origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.TipoDocumento(
                        origem.id(), origem.nome(), origem.codigoTipologia(), origem.ativo())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.VinculoDossie vinculo(
            ConsultaDocumentosDossieProdutoMtrResponse.VinculoDossie origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.VinculoDossie(
                cliente(origem.cliente()),
                produto(origem.produto()),
                garantia(origem.garantia()),
                fase(origem.fase()),
                processo(origem.processo()));
    }

    private static DocumentoDossieProdutoConsultado.Cliente cliente(
            ConsultaDocumentosDossieProdutoMtrResponse.Cliente origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.Cliente(
                origem.cpf(),
                origem.cnpj(),
                origem.nome(),
                origem.razaoSocial(),
                origem.tipoVinculo(),
                origem.identificadorNegocialVinculo(),
                origem.principal());
    }

    private static DocumentoDossieProdutoConsultado.Produto produto(
            ConsultaDocumentosDossieProdutoMtrResponse.Produto origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Produto(
                        origem.id(), origem.nome(), origem.modalidade(), origem.operacao())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Garantia garantia(
            ConsultaDocumentosDossieProdutoMtrResponse.Garantia origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.Garantia(
                origem.id(),
                origem.nome(),
                origem.codigoBacen(),
                produto(origem.produto()),
                mapear(origem.clientesAvalistas(),
                        ConsultaDocumentosDossieProdutoMtrMapper::clienteAvalista));
    }

    private static DocumentoDossieProdutoConsultado.ClienteAvalista clienteAvalista(
            ConsultaDocumentosDossieProdutoMtrResponse.ClienteAvalista origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.ClienteAvalista(
                        origem.cpf(), origem.cnpj())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Fase fase(
            ConsultaDocumentosDossieProdutoMtrResponse.Fase origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Fase(
                        origem.id(), origem.nome(), origem.identificadorNegocial())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Processo processo(
            ConsultaDocumentosDossieProdutoMtrResponse.Processo origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Processo(
                        origem.id(), origem.nome(), origem.identificadorNegocial(),
                        origem.macroprocesso())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Atributo atributo(
            ConsultaDocumentosDossieProdutoMtrResponse.Atributo origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Atributo(
                        origem.chave(),
                        origem.valor(),
                        mapear(origem.opcoesSelecionadas(), Function.identity()))
                : null;
    }

    private static DocumentoDossieProdutoConsultado.AssinaturaDigital assinatura(
            ConsultaDocumentosDossieProdutoMtrResponse.AssinaturaDigital origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.AssinaturaDigital(
                origem.dataAssinatura(),
                origem.emissor(),
                origem.cpf(),
                origem.cnpj(),
                origem.nome(),
                origem.cpfResponsavelPj(),
                origem.nomeResponsavelPj());
    }

    private static DocumentoDossieProdutoConsultado.Conformidade conformidade(
            ConsultaDocumentosDossieProdutoMtrResponse.Conformidade origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.Conformidade(
                origem.id(),
                origem.tipoConformidade(),
                origem.unidadeConformidade(),
                origem.dataHoraVerificacao(),
                origem.dossieProduto(),
                origem.fornecedor(),
                origem.resultado(),
                checklist(origem.checklist()));
    }

    private static DocumentoDossieProdutoConsultado.Checklist checklist(
            ConsultaDocumentosDossieProdutoMtrResponse.Checklist origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.Checklist(
                origem.id(),
                origem.identificadorNegocial(),
                origem.nome(),
                origem.versao(),
                mapear(origem.apontamentos(),
                        ConsultaDocumentosDossieProdutoMtrMapper::apontamento));
    }

    private static DocumentoDossieProdutoConsultado.Apontamento apontamento(
            ConsultaDocumentosDossieProdutoMtrResponse.Apontamento origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.Apontamento(
                origem.id(),
                origem.identificadorNegocial(),
                origem.nome(),
                origem.tipo(),
                origem.complexidade(),
                origem.aprovado(),
                origem.orientacao(),
                origem.comentario());
    }

    private static DocumentoDossieProdutoConsultado.Propriedade propriedade(
            ConsultaDocumentosDossieProdutoMtrResponse.Propriedade origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Propriedade(
                        origem.chave(), origem.valor())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Outsourcing outsourcing(
            ConsultaDocumentosDossieProdutoMtrResponse.Outsourcing origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.Outsourcing(
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

    private static DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial
            resultadoValidacao(
                    ConsultaDocumentosDossieProdutoMtrResponse
                            .ResultadoValidacaoNegocial origem
            ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial(
                origem.identificadorChecklist(),
                origem.versaoChecklist(),
                mapear(origem.apontamentos(),
                        ConsultaDocumentosDossieProdutoMtrMapper::resultadoApontamento));
    }

    private static DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento
            resultadoApontamento(
                    ConsultaDocumentosDossieProdutoMtrResponse
                            .ResultadoValidacaoNegocialApontamento origem
            ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado
                        .ResultadoValidacaoNegocialApontamento(
                                origem.identificadorApontamento(),
                                origem.aprovado(),
                                origem.comentario())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Armazenamento armazenamento(
            ConsultaDocumentosDossieProdutoMtrResponse.Armazenamento origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.Armazenamento(
                origem.id(),
                origem.dataHoraArmazenamento(),
                origem.tipoArmazenamento(),
                origem.pathStorage(),
                origem.objectStoreGed(),
                origem.codigoGed(),
                origem.dataHoraPrevisaoExclusao(),
                origem.dataHoraExclusao());
    }

    @SuppressWarnings("java:S1168") // Null distingue lista ausente de lista vazia no contrato MTR.
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
