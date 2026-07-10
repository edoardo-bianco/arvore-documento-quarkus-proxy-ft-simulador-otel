package br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo.ProcessoVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface ProcessoMapper {

    ProcessoVo toVo(ProcessoDto dto);

    ProcessoDto toDto(ProcessoVo vo);
}
