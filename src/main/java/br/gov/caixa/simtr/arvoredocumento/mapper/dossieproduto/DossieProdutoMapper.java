package br.gov.caixa.simtr.arvoredocumento.mapper.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteAvalistaDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteRelacionadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoRespostaFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoClienteDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoDossieDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoGarantiaDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoVinculoProdutoDto;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoClienteAvalistaVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoClienteRelacionadoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoClienteVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoFormularioVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoRespostaFormularioVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoVinculoClienteVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoVinculoDossieVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoVinculoGarantiaVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoVinculoProdutoVo;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class DossieProdutoMapper {

    public DossieProdutoCriacaoVo toVo(DossieProdutoCriacaoDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoCriacaoVo(
                dto.processo(),
                dto.chaveCorrelacaoCanal(),
                dto.numeroNegocio(),
                map(dto.clientes(), this::toVo)
        );
    }

    public DossieProdutoCriacaoDto toDto(DossieProdutoCriacaoVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoCriacaoDto(
                vo.processo(),
                vo.chaveCorrelacaoCanal(),
                vo.numeroNegocio(),
                map(vo.clientes(), this::toDto)
        );
    }

    public DossieProdutoCriadoVo toVo(DossieProdutoCriadoDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoCriadoVo(dto.id());
    }

    public DossieProdutoCriadoDto toDto(DossieProdutoCriadoVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoCriadoDto(vo.id());
    }

    public List<DossieProdutoFormularioVo> toFormularioVo(List<DossieProdutoFormularioDto> dto) {
        return map(dto, item -> toVo(item));
    }

    public List<DossieProdutoFormularioDto> toFormularioDto(List<DossieProdutoFormularioVo> vo) {
        return map(vo, item -> toDto(item));
    }

    private DossieProdutoFormularioVo toVo(DossieProdutoFormularioDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoFormularioVo(toVo(dto.vinculoDossie()));
    }

    private DossieProdutoFormularioDto toDto(DossieProdutoFormularioVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoFormularioDto(toDto(vo.vinculoDossie()));
    }

    private DossieProdutoVinculoDossieVo toVo(DossieProdutoVinculoDossieDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoVinculoDossieVo(
                dto.fase(),
                toVo(dto.cliente()),
                toVo(dto.produto()),
                toVo(dto.garantia()),
                map(dto.respostasFormulario(), item -> toVo(item))
        );
    }

    private DossieProdutoVinculoDossieDto toDto(DossieProdutoVinculoDossieVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoVinculoDossieDto(
                vo.fase(),
                toDto(vo.cliente()),
                toDto(vo.produto()),
                toDto(vo.garantia()),
                map(vo.respostasFormulario(), item -> toDto(item))
        );
    }

    private DossieProdutoVinculoClienteVo toVo(DossieProdutoVinculoClienteDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoVinculoClienteVo(dto.cpf(), dto.cnpj(), dto.tipoVinculo());
    }

    private DossieProdutoVinculoClienteDto toDto(DossieProdutoVinculoClienteVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoVinculoClienteDto(vo.cpf(), vo.cnpj(), vo.tipoVinculo());
    }

    private DossieProdutoVinculoProdutoVo toVo(DossieProdutoVinculoProdutoDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoVinculoProdutoVo(dto.codigoOperacao(), dto.codigoModalidade());
    }

    private DossieProdutoVinculoProdutoDto toDto(DossieProdutoVinculoProdutoVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoVinculoProdutoDto(vo.codigoOperacao(), vo.codigoModalidade());
    }

    private DossieProdutoVinculoGarantiaVo toVo(DossieProdutoVinculoGarantiaDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoVinculoGarantiaVo(
                dto.codigoBacen(),
                dto.produtoOperacao(),
                dto.produtoModalidade(),
                map(dto.clientesAvalistas(), item -> toVo(item))
        );
    }

    private DossieProdutoVinculoGarantiaDto toDto(DossieProdutoVinculoGarantiaVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoVinculoGarantiaDto(
                vo.codigoBacen(),
                vo.produtoOperacao(),
                vo.produtoModalidade(),
                map(vo.clientesAvalistas(), item -> toDto(item))
        );
    }

    private DossieProdutoClienteAvalistaVo toVo(DossieProdutoClienteAvalistaDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoClienteAvalistaVo(dto.cpf(), dto.cnpj());
    }

    private DossieProdutoClienteAvalistaDto toDto(DossieProdutoClienteAvalistaVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoClienteAvalistaDto(vo.cpf(), vo.cnpj());
    }

    private DossieProdutoRespostaFormularioVo toVo(DossieProdutoRespostaFormularioDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoRespostaFormularioVo(
                dto.campoFormulario(),
                dto.resposta(),
                dto.opcoesSelecionadas(),
                dto.excluir()
        );
    }

    private DossieProdutoRespostaFormularioDto toDto(DossieProdutoRespostaFormularioVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoRespostaFormularioDto(
                vo.campoFormulario(),
                vo.resposta(),
                vo.opcoesSelecionadas(),
                vo.excluir()
        );
    }

    private DossieProdutoClienteVo toVo(DossieProdutoClienteDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoClienteVo(
                dto.cpf(),
                dto.cnpj(),
                dto.tipoVinculo(),
                toVo(dto.clienteRelacionado()),
                dto.sequenciaTitularidade()
        );
    }

    private DossieProdutoClienteDto toDto(DossieProdutoClienteVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoClienteDto(
                vo.cpf(),
                vo.cnpj(),
                vo.tipoVinculo(),
                toDto(vo.clienteRelacionado()),
                vo.sequenciaTitularidade()
        );
    }

    private DossieProdutoClienteRelacionadoVo toVo(DossieProdutoClienteRelacionadoDto dto) {
        if (dto == null) {
            return null;
        }
        return new DossieProdutoClienteRelacionadoVo(dto.cpf(), dto.cnpj());
    }

    private DossieProdutoClienteRelacionadoDto toDto(DossieProdutoClienteRelacionadoVo vo) {
        if (vo == null) {
            return null;
        }
        return new DossieProdutoClienteRelacionadoDto(vo.cpf(), vo.cnpj());
    }

    private <S, T> List<T> map(List<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        return source.stream().map(mapper).toList();
    }
}
