package br.gov.caixa.simtr.arvoredocumento.mapper.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoClienteRelacionadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoClienteRelacionadoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoClienteVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriadoVo;
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
