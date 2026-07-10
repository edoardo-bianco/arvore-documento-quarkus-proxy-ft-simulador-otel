package br.gov.caixa.simtr.hub.parametrizacao.mapeamento;

import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.parametrizacao.dominio.processo.ProcessoVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface ProcessoMapper {

    ProcessoVo toVo(ProcessoDto dto);

    ProcessoDto toDto(ProcessoVo vo);
}
