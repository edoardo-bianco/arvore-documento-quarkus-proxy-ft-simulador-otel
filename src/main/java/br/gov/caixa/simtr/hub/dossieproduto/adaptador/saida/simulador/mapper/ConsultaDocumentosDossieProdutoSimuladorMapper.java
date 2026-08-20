package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ConsultaDocumentosDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ConsultaDocumentosDossieProdutoSimuladorMapper {

    public List<DocumentoDossieProdutoConsultado> paraDominio(
            ConsultaDocumentosDossieProdutoSimuladorResponse resposta
    ) {
        if (resposta == null || resposta.documentos() == null) {
            return List.of();
        }
        return mapear(
                resposta.documentos(),
                ConsultaDocumentosDossieProdutoSimuladorMapper::documento);
    }

    private static DocumentoDossieProdutoConsultado documento(
            ConsultaDocumentosDossieProdutoSimuladorResponse.Documento origem
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
                        ConsultaDocumentosDossieProdutoSimuladorMapper::atributo),
                mapear(origem.assinaturasDigitais(),
                        ConsultaDocumentosDossieProdutoSimuladorMapper::assinatura),
                mapear(origem.conformidade(),
                        ConsultaDocumentosDossieProdutoSimuladorMapper::conformidade),
                mapear(origem.propriedades(),
                        ConsultaDocumentosDossieProdutoSimuladorMapper::propriedade),
                mapear(origem.outsourcing(),
                        ConsultaDocumentosDossieProdutoSimuladorMapper::outsourcing),
                mapear(origem.armazenamento(),
                        ConsultaDocumentosDossieProdutoSimuladorMapper::armazenamento));
    }

    private static DocumentoDossieProdutoConsultado.TipoDocumento tipoDocumento(
            ConsultaDocumentosDossieProdutoSimuladorResponse.TipoDocumento origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.TipoDocumento(
                        origem.id(), origem.nome(), origem.codigoTipologia(), origem.ativo())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.VinculoDossie vinculo(
            ConsultaDocumentosDossieProdutoSimuladorResponse.VinculoDossie origem
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
            ConsultaDocumentosDossieProdutoSimuladorResponse.Cliente origem
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
            ConsultaDocumentosDossieProdutoSimuladorResponse.Produto origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Produto(
                        origem.id(), origem.nome(), origem.modalidade(), origem.operacao())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Garantia garantia(
            ConsultaDocumentosDossieProdutoSimuladorResponse.Garantia origem
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
                        ConsultaDocumentosDossieProdutoSimuladorMapper::clienteAvalista));
    }

    private static DocumentoDossieProdutoConsultado.ClienteAvalista clienteAvalista(
            ConsultaDocumentosDossieProdutoSimuladorResponse.ClienteAvalista origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.ClienteAvalista(
                        origem.cpf(), origem.cnpj())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Fase fase(
            ConsultaDocumentosDossieProdutoSimuladorResponse.Fase origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Fase(
                        origem.id(), origem.nome(), origem.identificadorNegocial())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Processo processo(
            ConsultaDocumentosDossieProdutoSimuladorResponse.Processo origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Processo(
                        origem.id(), origem.nome(), origem.identificadorNegocial(),
                        origem.macroprocesso())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Atributo atributo(
            ConsultaDocumentosDossieProdutoSimuladorResponse.Atributo origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Atributo(
                        origem.chave(),
                        origem.valor(),
                        mapear(origem.opcoesSelecionadas(), Function.identity()))
                : null;
    }

    private static DocumentoDossieProdutoConsultado.AssinaturaDigital assinatura(
            ConsultaDocumentosDossieProdutoSimuladorResponse.AssinaturaDigital origem
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
            ConsultaDocumentosDossieProdutoSimuladorResponse.Conformidade origem
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
            ConsultaDocumentosDossieProdutoSimuladorResponse.Checklist origem
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
                        ConsultaDocumentosDossieProdutoSimuladorMapper::apontamento));
    }

    private static DocumentoDossieProdutoConsultado.Apontamento apontamento(
            ConsultaDocumentosDossieProdutoSimuladorResponse.Apontamento origem
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
            ConsultaDocumentosDossieProdutoSimuladorResponse.Propriedade origem
    ) {
        return origem != null
                ? new DocumentoDossieProdutoConsultado.Propriedade(
                        origem.chave(), origem.valor())
                : null;
    }

    private static DocumentoDossieProdutoConsultado.Outsourcing outsourcing(
            ConsultaDocumentosDossieProdutoSimuladorResponse.Outsourcing origem
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
                    ConsultaDocumentosDossieProdutoSimuladorResponse
                            .ResultadoValidacaoNegocial origem
            ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocial(
                origem.identificadorChecklist(),
                origem.versaoChecklist(),
                mapear(origem.apontamentos(),
                        ConsultaDocumentosDossieProdutoSimuladorMapper::resultadoApontamento));
    }

    private static DocumentoDossieProdutoConsultado.ResultadoValidacaoNegocialApontamento
            resultadoApontamento(
                    ConsultaDocumentosDossieProdutoSimuladorResponse
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
            ConsultaDocumentosDossieProdutoSimuladorResponse.Armazenamento origem
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

    @SuppressWarnings("java:S1168") // Null distingue lista ausente de lista vazia na fixture.
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
