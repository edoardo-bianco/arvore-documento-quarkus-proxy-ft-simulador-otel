package br.gov.caixa.simtr.hub.gestaodocumento.mapeamento;

import br.gov.caixa.simtr.hub.gestaodocumento.recurso.rest.v1.dto.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.GestaoDocumentoCredencialContainerVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface GestaoDocumentoMapper {

    GestaoDocumentoCredencialContainerVo toVo(GestaoDocumentoCredencialContainerDto dto);

    GestaoDocumentoCredencialContainerDto toDto(GestaoDocumentoCredencialContainerVo vo);
}
