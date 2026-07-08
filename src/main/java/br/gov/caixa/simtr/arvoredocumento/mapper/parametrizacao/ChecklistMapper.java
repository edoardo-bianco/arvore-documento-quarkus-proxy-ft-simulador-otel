package br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistApontamentoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.checklist.ChecklistApontamentoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.checklist.ChecklistVo;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ChecklistMapper {

    public ChecklistVo toVo(ChecklistDto dto) {
        if (dto == null) {
            return null;
        }
        return new ChecklistVo(
                dto.nome(),
                dto.identificadorNegocial(),
                dto.versao(),
                dto.dataHoraCriacao(),
                dto.dataHoraUltimaAlteracao(),
                dto.verificacaoPrevia(),
                dto.orientacaoOperador(),
                map(dto.apontamentos(), this::toVo)
        );
    }

    public ChecklistDto toDto(ChecklistVo vo) {
        if (vo == null) {
            return null;
        }
        return new ChecklistDto(
                vo.nome(),
                vo.identificadorNegocial(),
                vo.versao(),
                vo.dataHoraCriacao(),
                vo.dataHoraUltimaAlteracao(),
                vo.verificacaoPrevia(),
                vo.orientacaoOperador(),
                map(vo.apontamentos(), this::toDto)
        );
    }

    private ChecklistApontamentoVo toVo(ChecklistApontamentoDto dto) {
        if (dto == null) {
            return null;
        }
        return new ChecklistApontamentoVo(
                dto.identificadorNegocial(),
                dto.nome(),
                dto.descricao(),
                dto.orientacaoOperador(),
                dto.indicadorReanalise(),
                dto.sequenciaApresentacao()
        );
    }

    private ChecklistApontamentoDto toDto(ChecklistApontamentoVo vo) {
        if (vo == null) {
            return null;
        }
        return new ChecklistApontamentoDto(
                vo.identificadorNegocial(),
                vo.nome(),
                vo.descricao(),
                vo.orientacaoOperador(),
                vo.indicadorReanalise(),
                vo.sequenciaApresentacao()
        );
    }

    private <S, T> List<T> map(List<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        return source.stream().map(mapper).toList();
    }
}
