package br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.*;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo.*;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ProcessoMapper {

    public ProcessoVo toVo(ProcessoDto dto) {
        if (dto == null) {
            return null;
        }
        return new ProcessoVo(
                dto.identificadorNegocial(),
                dto.nome(),
                dto.ativo(),
                dto.ultimaAlteracao(),
                dto.indicadorProdutoObrigatorio(),
                toVo(dto.macroprocesso()),
                map(dto.relacionamentos(), this::toVo),
                map(dto.produtos(), this::toVo),
                map(dto.fases(), this::toVo),
                map(dto.documentos(), this::toVo),
                toVo(dto.checklist())
        );
    }

    public ProcessoDto toDto(ProcessoVo vo) {
        if (vo == null) {
            return null;
        }
        return new ProcessoDto(
                vo.identificadorNegocial(),
                vo.nome(),
                vo.ativo(),
                vo.ultimaAlteracao(),
                vo.indicadorProdutoObrigatorio(),
                toDto(vo.macroprocesso()),
                map(vo.relacionamentos(), this::toDto),
                map(vo.produtos(), this::toDto),
                map(vo.fases(), this::toDto),
                map(vo.documentos(), this::toDto),
                toDto(vo.checklist())
        );
    }

    private MacroprocessoVo toVo(MacroprocessoDto dto) {
        if (dto == null) {
            return null;
        }
        return new MacroprocessoVo(dto.identificadorNegocial(), dto.nome(), dto.ativo(), dto.ultimaAlteracao());
    }

    private MacroprocessoDto toDto(MacroprocessoVo vo) {
        if (vo == null) {
            return null;
        }
        return new MacroprocessoDto(vo.identificadorNegocial(), vo.nome(), vo.ativo(), vo.ultimaAlteracao());
    }

    private RelacionamentoVo toVo(RelacionamentoDto dto) {
        if (dto == null) {
            return null;
        }
        return new RelacionamentoVo(
                dto.identificadorNegocial(),
                dto.nome(),
                dto.tipoPessoa(),
                dto.principal(),
                dto.obrigatorio(),
                dto.relacionado(),
                dto.sequencia(),
                map(dto.camposFormulario(), this::toVo),
                map(dto.documentos(), this::toVo)
        );
    }

    private RelacionamentoDto toDto(RelacionamentoVo vo) {
        if (vo == null) {
            return null;
        }
        return new RelacionamentoDto(
                vo.identificadorNegocial(),
                vo.nome(),
                vo.tipoPessoa(),
                vo.principal(),
                vo.obrigatorio(),
                vo.relacionado(),
                vo.sequencia(),
                map(vo.camposFormulario(), this::toDto),
                map(vo.documentos(), this::toDto)
        );
    }

    private ProdutoVo toVo(ProdutoDto dto) {
        if (dto == null) {
            return null;
        }
        return new ProdutoVo(
                dto.codigoOperacao(),
                dto.codigoModalidade(),
                dto.nome(),
                map(dto.camposFormulario(), this::toVo),
                map(dto.documentos(), this::toVo),
                map(dto.garantias(), this::toVo),
                toVo(dto.checklist())
        );
    }

    private ProdutoDto toDto(ProdutoVo vo) {
        if (vo == null) {
            return null;
        }
        return new ProdutoDto(
                vo.codigoOperacao(),
                vo.codigoModalidade(),
                vo.nome(),
                map(vo.camposFormulario(), this::toDto),
                map(vo.documentos(), this::toDto),
                map(vo.garantias(), this::toDto),
                toDto(vo.checklist())
        );
    }

    private FaseVo toVo(FaseDto dto) {
        if (dto == null) {
            return null;
        }
        return new FaseVo(
                dto.identificadorNegocial(),
                dto.nome(),
                dto.ativo(),
                dto.ultimaAlteracao(),
                dto.ordem(),
                dto.orientacaoUsuario(),
                map(dto.produtos(), this::toVo),
                map(dto.garantias(), this::toVo),
                map(dto.camposFormulario(), this::toVo),
                map(dto.documentos(), this::toVo),
                map(dto.checklist(), this::toVo)
        );
    }

    private FaseDto toDto(FaseVo vo) {
        if (vo == null) {
            return null;
        }
        return new FaseDto(
                vo.identificadorNegocial(),
                vo.nome(),
                vo.ativo(),
                vo.ultimaAlteracao(),
                vo.ordem(),
                vo.orientacaoUsuario(),
                map(vo.produtos(), this::toDto),
                map(vo.garantias(), this::toDto),
                map(vo.camposFormulario(), this::toDto),
                map(vo.documentos(), this::toDto),
                map(vo.checklist(), this::toDto)
        );
    }

    private GarantiaVo toVo(GarantiaDto dto) {
        if (dto == null) {
            return null;
        }
        return new GarantiaVo(
                dto.codigoBacen(),
                dto.nomeGarantia(),
                dto.fidejussoria(),
                map(dto.camposFormulario(), this::toVo),
                map(dto.documentos(), this::toVo),
                toVo(dto.checklist())
        );
    }

    private GarantiaDto toDto(GarantiaVo vo) {
        if (vo == null) {
            return null;
        }
        return new GarantiaDto(
                vo.codigoBacen(),
                vo.nomeGarantia(),
                vo.fidejussoria(),
                map(vo.camposFormulario(), this::toDto),
                map(vo.documentos(), this::toDto),
                toDto(vo.checklist())
        );
    }

    private CampoFormularioVo toVo(CampoFormularioDto dto) {
        if (dto == null) {
            return null;
        }
        return new CampoFormularioVo(
                dto.identificadorNegocial(),
                dto.label(),
                dto.obrigatorio(),
                dto.ativo(),
                dto.exibicaoCondicional(),
                dto.tamanhoApresentacao(),
                dto.ordemApresentacao(),
                dto.tipo(),
                dto.mascara(),
                dto.placeholder(),
                dto.tamanhoMinimo(),
                dto.tamanhoMaximo(),
                dto.orientacaoPreenchimento(),
                dto.bloquearEdicao(),
                map(dto.opcoesDisponiveis(), this::toVo)
        );
    }

    private CampoFormularioDto toDto(CampoFormularioVo vo) {
        if (vo == null) {
            return null;
        }
        return new CampoFormularioDto(
                vo.identificadorNegocial(),
                vo.label(),
                vo.obrigatorio(),
                vo.ativo(),
                vo.exibicaoCondicional(),
                vo.tamanhoApresentacao(),
                vo.ordemApresentacao(),
                vo.tipo(),
                vo.mascara(),
                vo.placeholder(),
                vo.tamanhoMinimo(),
                vo.tamanhoMaximo(),
                vo.orientacaoPreenchimento(),
                vo.bloquearEdicao(),
                map(vo.opcoesDisponiveis(), this::toDto)
        );
    }

    private OpcaoDisponivelVo toVo(OpcaoDisponivelDto dto) {
        if (dto == null) {
            return null;
        }
        return new OpcaoDisponivelVo(dto.valorOpcao(), dto.descricaoOpcao(), dto.ativo());
    }

    private OpcaoDisponivelDto toDto(OpcaoDisponivelVo vo) {
        if (vo == null) {
            return null;
        }
        return new OpcaoDisponivelDto(vo.valorOpcao(), vo.descricaoOpcao(), vo.ativo());
    }

    private DocumentoVo toVo(DocumentoDto dto) {
        if (dto == null) {
            return null;
        }
        return new DocumentoVo(toVo(dto.funcaoDocumental()), toVo(dto.tipoDocumento()), dto.obrigatorio());
    }

    private DocumentoDto toDto(DocumentoVo vo) {
        if (vo == null) {
            return null;
        }
        return new DocumentoDto(toDto(vo.funcaoDocumental()), toDto(vo.tipoDocumento()), vo.obrigatorio());
    }

    private FuncaoDocumentalVo toVo(FuncaoDocumentalDto dto) {
        if (dto == null) {
            return null;
        }
        return new FuncaoDocumentalVo(dto.nome(), map(dto.tiposDocumento(), this::toVo), toVo(dto.checklist()));
    }

    private FuncaoDocumentalDto toDto(FuncaoDocumentalVo vo) {
        if (vo == null) {
            return null;
        }
        return new FuncaoDocumentalDto(vo.nome(), map(vo.tiposDocumento(), this::toDto), toDto(vo.checklist()));
    }

    private TipoDocumentoVo toVo(TipoDocumentoDto dto) {
        if (dto == null) {
            return null;
        }
        return new TipoDocumentoVo(dto.codigoTipologia(), dto.nome(), dto.permiteReuso(), dto.permiteMultiplo(), dto.ativo(), toVo(dto.checklist()));
    }

    private TipoDocumentoDto toDto(TipoDocumentoVo vo) {
        if (vo == null) {
            return null;
        }
        return new TipoDocumentoDto(vo.codigoTipologia(), vo.nome(), vo.permiteReuso(), vo.permiteMultiplo(), vo.ativo(), toDto(vo.checklist()));
    }

    private ChecklistReferenciaVo toVo(ChecklistReferenciaDto dto) {
        if (dto == null) {
            return null;
        }
        return new ChecklistReferenciaVo(dto.identificadorChecklist(), dto.versaoChecklist());
    }

    private ChecklistReferenciaDto toDto(ChecklistReferenciaVo vo) {
        if (vo == null) {
            return null;
        }
        return new ChecklistReferenciaDto(vo.identificadorChecklist(), vo.versaoChecklist());
    }

    private <S, T> List<T> map(List<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        return source.stream().map(mapper).toList();
    }
}
