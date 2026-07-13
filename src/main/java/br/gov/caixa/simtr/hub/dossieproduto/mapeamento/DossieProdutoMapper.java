package br.gov.caixa.simtr.hub.dossieproduto.mapeamento;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface DossieProdutoMapper {

    DossieProdutoValidacaoNegocialVo toVo(DossieProdutoValidacaoNegocialDto dto);

    DossieProdutoValidacaoNegocialDto toDto(DossieProdutoValidacaoNegocialVo vo);
}
