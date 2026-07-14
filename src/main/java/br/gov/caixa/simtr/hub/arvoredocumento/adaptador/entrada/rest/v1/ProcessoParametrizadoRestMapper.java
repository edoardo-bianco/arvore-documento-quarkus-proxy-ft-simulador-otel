package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.CampoFormularioDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.ChecklistReferenciaDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.DocumentoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.FaseDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.FuncaoDocumentalDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.GarantiaDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.MacroprocessoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.OpcaoDisponivelDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.ProdutoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.RelacionamentoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo.TipoDocumentoDto;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.erro.FalhaConsultaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.CampoFormularioProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.DocumentoProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.FaseProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.FuncaoDocumentalProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.GarantiaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.MacroprocessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.OpcaoDisponivelProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProdutoProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ReferenciaChecklistProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.RelacionamentoProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.TipoDocumentoProcessoParametrizado;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class ProcessoParametrizadoRestMapper {

    private ProcessoParametrizadoRestMapper() {
    }

    static ProcessoDto paraResposta(ProcessoParametrizado processo) {
        if (processo == null) {
            return null;
        }
        return new ProcessoDto(
                processo.identificadorNegocial(),
                processo.nome(),
                processo.ativo(),
                processo.ultimaAlteracao(),
                processo.indicadorProdutoObrigatorio(),
                macroprocesso(processo.macroprocesso()),
                mapear(processo.relacionamentos(), ProcessoParametrizadoRestMapper::relacionamento),
                mapear(processo.produtos(), ProcessoParametrizadoRestMapper::produto),
                mapear(processo.fases(), ProcessoParametrizadoRestMapper::fase),
                mapear(processo.documentos(), ProcessoParametrizadoRestMapper::documento),
                checklist(processo.checklist())
        );
    }

    static Throwable paraExcecaoRest(FalhaConsultaProcessoParametrizado falha) {
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
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT -> new MtrServerErrorException(status, erro);
        };
    }

    private static MacroprocessoDto macroprocesso(MacroprocessoParametrizado origem) {
        if (origem == null) {
            return null;
        }
        return new MacroprocessoDto(
                origem.identificadorNegocial(), origem.nome(), origem.ativo(),
                origem.ultimaAlteracao());
    }

    private static RelacionamentoDto relacionamento(RelacionamentoProcessoParametrizado origem) {
        if (origem == null) {
            return null;
        }
        return new RelacionamentoDto(
                origem.identificadorNegocial(),
                origem.nome(),
                origem.tipoPessoa(),
                origem.principal(),
                origem.obrigatorio(),
                origem.relacionado(),
                origem.sequencia(),
                mapear(origem.camposFormulario(), ProcessoParametrizadoRestMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoRestMapper::documento)
        );
    }

    private static ProdutoDto produto(ProdutoProcessoParametrizado origem) {
        if (origem == null) {
            return null;
        }
        return new ProdutoDto(
                origem.codigoOperacao(),
                origem.codigoModalidade(),
                origem.nome(),
                mapear(origem.camposFormulario(), ProcessoParametrizadoRestMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoRestMapper::documento),
                mapear(origem.garantias(), ProcessoParametrizadoRestMapper::garantia),
                checklist(origem.checklist())
        );
    }

    private static FaseDto fase(FaseProcessoParametrizado origem) {
        if (origem == null) {
            return null;
        }
        return new FaseDto(
                origem.identificadorNegocial(),
                origem.nome(),
                origem.ativo(),
                origem.ultimaAlteracao(),
                origem.ordem(),
                origem.orientacaoUsuario(),
                mapear(origem.produtos(), ProcessoParametrizadoRestMapper::produto),
                mapear(origem.garantias(), ProcessoParametrizadoRestMapper::garantia),
                mapear(origem.camposFormulario(), ProcessoParametrizadoRestMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoRestMapper::documento),
                mapear(origem.checklist(), ProcessoParametrizadoRestMapper::checklist)
        );
    }

    private static GarantiaDto garantia(GarantiaProcessoParametrizado origem) {
        if (origem == null) {
            return null;
        }
        return new GarantiaDto(
                origem.codigoBacen(),
                origem.nomeGarantia(),
                origem.fidejussoria(),
                mapear(origem.camposFormulario(), ProcessoParametrizadoRestMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoRestMapper::documento),
                checklist(origem.checklist())
        );
    }

    private static DocumentoDto documento(DocumentoProcessoParametrizado origem) {
        if (origem == null) {
            return null;
        }
        return new DocumentoDto(
                funcaoDocumental(origem.funcaoDocumental()),
                tipoDocumento(origem.tipoDocumento()),
                origem.obrigatorio()
        );
    }

    private static FuncaoDocumentalDto funcaoDocumental(
            FuncaoDocumentalProcessoParametrizado origem
    ) {
        if (origem == null) {
            return null;
        }
        return new FuncaoDocumentalDto(
                origem.nome(),
                mapear(origem.tiposDocumento(), ProcessoParametrizadoRestMapper::tipoDocumento),
                checklist(origem.checklist())
        );
    }

    private static TipoDocumentoDto tipoDocumento(TipoDocumentoProcessoParametrizado origem) {
        if (origem == null) {
            return null;
        }
        return new TipoDocumentoDto(
                origem.codigoTipologia(),
                origem.nome(),
                origem.permiteReuso(),
                origem.permiteMultiplo(),
                origem.ativo(),
                checklist(origem.checklist())
        );
    }

    private static CampoFormularioDto campoFormulario(
            CampoFormularioProcessoParametrizado origem
    ) {
        if (origem == null) {
            return null;
        }
        return new CampoFormularioDto(
                origem.identificadorNegocial(),
                origem.label(),
                origem.obrigatorio(),
                origem.ativo(),
                origem.exibicaoCondicional(),
                origem.tamanhoApresentacao(),
                origem.ordemApresentacao(),
                origem.tipo(),
                origem.mascara(),
                origem.placeholder(),
                origem.tamanhoMinimo(),
                origem.tamanhoMaximo(),
                origem.orientacaoPreenchimento(),
                origem.bloquearEdicao(),
                mapear(origem.opcoesDisponiveis(), ProcessoParametrizadoRestMapper::opcaoDisponivel)
        );
    }

    private static OpcaoDisponivelDto opcaoDisponivel(
            OpcaoDisponivelProcessoParametrizado origem
    ) {
        if (origem == null) {
            return null;
        }
        return new OpcaoDisponivelDto(
                origem.valorOpcao(), origem.descricaoOpcao(), origem.ativo());
    }

    private static ChecklistReferenciaDto checklist(
            ReferenciaChecklistProcessoParametrizado origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ChecklistReferenciaDto(
                origem.identificadorChecklist(), origem.versaoChecklist());
    }

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
