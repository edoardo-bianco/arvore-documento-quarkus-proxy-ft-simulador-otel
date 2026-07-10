package br.gov.caixa.simtr.arvoredocumento.mapper.gestaodocumento;

import br.gov.caixa.simtr.arvoredocumento.api.dto.gestaodocumento.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.arvoredocumento.domain.gestaodocumento.GestaoDocumentoCredencialContainerVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface GestaoDocumentoMapper {

    GestaoDocumentoCredencialContainerVo toVo(GestaoDocumentoCredencialContainerDto dto);

    GestaoDocumentoCredencialContainerDto toDto(GestaoDocumentoCredencialContainerVo vo);
}
