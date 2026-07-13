package br.gov.caixa.simtr.hub.dossieproduto.mapeamento;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoInclusaoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface DossieProdutoMapper {

    DossieProdutoDocumentoInclusaoVo toVo(DossieProdutoDocumentoInclusaoDto dto);

    DossieProdutoDocumentoInclusaoDto toDto(DossieProdutoDocumentoInclusaoVo vo);

    DossieProdutoDocumentoCriadoVo toVo(DossieProdutoDocumentoCriadoDto dto);

    DossieProdutoDocumentoCriadoDto toDto(DossieProdutoDocumentoCriadoVo vo);

    DossieProdutoValidacaoNegocialVo toVo(DossieProdutoValidacaoNegocialDto dto);

    DossieProdutoValidacaoNegocialDto toDto(DossieProdutoValidacaoNegocialVo vo);
}
