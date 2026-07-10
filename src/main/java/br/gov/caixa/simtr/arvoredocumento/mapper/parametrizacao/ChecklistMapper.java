package br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.checklist.ChecklistVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface ChecklistMapper {

    ChecklistVo toVo(ChecklistDto dto);

    ChecklistDto toDto(ChecklistVo vo);
}
