package br.gov.caixa.simtr.hub.dossieproduto.mapeamento;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoInclusaoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoFormularioVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
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

    DossieProdutoDocumentoInclusaoVo toVo(DossieProdutoDocumentoInclusaoDto dto);

    DossieProdutoDocumentoInclusaoDto toDto(DossieProdutoDocumentoInclusaoVo vo);

    DossieProdutoDocumentoCriadoVo toVo(DossieProdutoDocumentoCriadoDto dto);

    DossieProdutoDocumentoCriadoDto toDto(DossieProdutoDocumentoCriadoVo vo);

    DossieProdutoValidacaoNegocialVo toVo(DossieProdutoValidacaoNegocialDto dto);

    DossieProdutoValidacaoNegocialDto toDto(DossieProdutoValidacaoNegocialVo vo);
}
