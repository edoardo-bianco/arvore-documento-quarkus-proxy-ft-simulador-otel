package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.mapper;

import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.dto.ProcessoParametrizadoSimuladorResponse;
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
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ProcessoParametrizadoSimuladorMapper {

    public ProcessoParametrizado paraDominio(ProcessoParametrizadoSimuladorResponse resposta) {
        if (resposta == null) {
            return null;
        }
        return new ProcessoParametrizado(
                resposta.identificadorNegocial(),
                resposta.nome(),
                resposta.ativo(),
                resposta.ultimaAlteracao(),
                resposta.indicadorProdutoObrigatorio(),
                macroprocesso(resposta.macroprocesso()),
                mapear(resposta.relacionamentos(),
                        ProcessoParametrizadoSimuladorMapper::relacionamento),
                mapear(resposta.produtos(), ProcessoParametrizadoSimuladorMapper::produto),
                mapear(resposta.fases(), ProcessoParametrizadoSimuladorMapper::fase),
                mapear(resposta.documentos(), ProcessoParametrizadoSimuladorMapper::documento),
                checklist(resposta.checklist())
        );
    }

    private static MacroprocessoParametrizado macroprocesso(
            ProcessoParametrizadoSimuladorResponse.Macroprocesso origem
    ) {
        if (origem == null) {
            return null;
        }
        return new MacroprocessoParametrizado(
                origem.identificadorNegocial(),
                origem.nome(),
                origem.ativo(),
                origem.ultimaAlteracao()
        );
    }

    private static RelacionamentoProcessoParametrizado relacionamento(
            ProcessoParametrizadoSimuladorResponse.Relacionamento origem
    ) {
        if (origem == null) {
            return null;
        }
        return new RelacionamentoProcessoParametrizado(
                origem.identificadorNegocial(),
                origem.nome(),
                origem.tipoPessoa(),
                origem.principal(),
                origem.obrigatorio(),
                origem.relacionado(),
                origem.sequencia(),
                mapear(origem.camposFormulario(),
                        ProcessoParametrizadoSimuladorMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoSimuladorMapper::documento)
        );
    }

    private static ProdutoProcessoParametrizado produto(
            ProcessoParametrizadoSimuladorResponse.Produto origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ProdutoProcessoParametrizado(
                origem.codigoOperacao(),
                origem.codigoModalidade(),
                origem.nome(),
                mapear(origem.camposFormulario(),
                        ProcessoParametrizadoSimuladorMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoSimuladorMapper::documento),
                mapear(origem.garantias(), ProcessoParametrizadoSimuladorMapper::garantia),
                checklist(origem.checklist())
        );
    }

    private static FaseProcessoParametrizado fase(
            ProcessoParametrizadoSimuladorResponse.Fase origem
    ) {
        if (origem == null) {
            return null;
        }
        return new FaseProcessoParametrizado(
                origem.identificadorNegocial(),
                origem.nome(),
                origem.ativo(),
                origem.ultimaAlteracao(),
                origem.ordem(),
                origem.orientacaoUsuario(),
                mapear(origem.produtos(), ProcessoParametrizadoSimuladorMapper::produto),
                mapear(origem.garantias(), ProcessoParametrizadoSimuladorMapper::garantia),
                mapear(origem.camposFormulario(),
                        ProcessoParametrizadoSimuladorMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoSimuladorMapper::documento),
                mapear(origem.checklist(), ProcessoParametrizadoSimuladorMapper::checklist)
        );
    }

    private static GarantiaProcessoParametrizado garantia(
            ProcessoParametrizadoSimuladorResponse.Garantia origem
    ) {
        if (origem == null) {
            return null;
        }
        return new GarantiaProcessoParametrizado(
                origem.codigoBacen(),
                origem.nomeGarantia(),
                origem.fidejussoria(),
                mapear(origem.camposFormulario(),
                        ProcessoParametrizadoSimuladorMapper::campoFormulario),
                mapear(origem.documentos(), ProcessoParametrizadoSimuladorMapper::documento),
                checklist(origem.checklist())
        );
    }

    private static DocumentoProcessoParametrizado documento(
            ProcessoParametrizadoSimuladorResponse.Documento origem
    ) {
        if (origem == null) {
            return null;
        }
        return new DocumentoProcessoParametrizado(
                funcaoDocumental(origem.funcaoDocumental()),
                tipoDocumento(origem.tipoDocumento()),
                origem.obrigatorio()
        );
    }

    private static FuncaoDocumentalProcessoParametrizado funcaoDocumental(
            ProcessoParametrizadoSimuladorResponse.FuncaoDocumental origem
    ) {
        if (origem == null) {
            return null;
        }
        return new FuncaoDocumentalProcessoParametrizado(
                origem.nome(),
                mapear(origem.tiposDocumento(),
                        ProcessoParametrizadoSimuladorMapper::tipoDocumento),
                checklist(origem.checklist())
        );
    }

    private static TipoDocumentoProcessoParametrizado tipoDocumento(
            ProcessoParametrizadoSimuladorResponse.TipoDocumento origem
    ) {
        if (origem == null) {
            return null;
        }
        return new TipoDocumentoProcessoParametrizado(
                origem.codigoTipologia(),
                origem.nome(),
                origem.permiteReuso(),
                origem.permiteMultiplo(),
                origem.ativo(),
                checklist(origem.checklist())
        );
    }

    private static CampoFormularioProcessoParametrizado campoFormulario(
            ProcessoParametrizadoSimuladorResponse.CampoFormulario origem
    ) {
        if (origem == null) {
            return null;
        }
        return new CampoFormularioProcessoParametrizado(
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
                mapear(origem.opcoesDisponiveis(),
                        ProcessoParametrizadoSimuladorMapper::opcaoDisponivel)
        );
    }

    private static OpcaoDisponivelProcessoParametrizado opcaoDisponivel(
            ProcessoParametrizadoSimuladorResponse.OpcaoDisponivel origem
    ) {
        if (origem == null) {
            return null;
        }
        return new OpcaoDisponivelProcessoParametrizado(
                origem.valorOpcao(), origem.descricaoOpcao(), origem.ativo()
        );
    }

    private static ReferenciaChecklistProcessoParametrizado checklist(
            ProcessoParametrizadoSimuladorResponse.ReferenciaChecklist origem
    ) {
        if (origem == null) {
            return null;
        }
        return new ReferenciaChecklistProcessoParametrizado(
                origem.identificadorChecklist(), origem.versaoChecklist()
        );
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
