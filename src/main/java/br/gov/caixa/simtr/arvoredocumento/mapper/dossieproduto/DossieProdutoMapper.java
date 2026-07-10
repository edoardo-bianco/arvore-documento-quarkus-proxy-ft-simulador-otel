package br.gov.caixa.simtr.arvoredocumento.mapper.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoFormularioVo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "jakarta-cdi")
public interface DossieProdutoMapper {

    DossieProdutoCriacaoVo toVo(DossieProdutoCriacaoDto dto);

    DossieProdutoCriacaoDto toDto(DossieProdutoCriacaoVo vo);

    DossieProdutoCriadoVo toVo(DossieProdutoCriadoDto dto);

    DossieProdutoCriadoDto toDto(DossieProdutoCriadoVo vo);

    List<DossieProdutoFormularioVo> toFormularioVo(List<DossieProdutoFormularioDto> dto);

    List<DossieProdutoFormularioDto> toFormularioDto(List<DossieProdutoFormularioVo> vo);
}
