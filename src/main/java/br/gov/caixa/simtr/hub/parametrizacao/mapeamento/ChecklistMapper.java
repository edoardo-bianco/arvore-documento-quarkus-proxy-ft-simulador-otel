package br.gov.caixa.simtr.hub.parametrizacao.mapeamento;

import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.parametrizacao.dominio.checklist.ChecklistVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface ChecklistMapper {

    ChecklistVo toVo(ChecklistDto dto);

    ChecklistDto toDto(ChecklistVo vo);
}
