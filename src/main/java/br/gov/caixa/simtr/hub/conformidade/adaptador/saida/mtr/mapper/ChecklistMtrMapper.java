package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.dto.v1.checklist.ChecklistMtrResponse;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ChecklistMtrMapper {

    public Checklist paraDominio(ChecklistMtrResponse resposta) {
        if (resposta == null) {
            return null;
        }
        return new Checklist(
                resposta.nome(),
                resposta.identificadorNegocial(),
                resposta.versao(),
                resposta.dataHoraCriacao(),
                resposta.dataHoraUltimaAlteracao(),
                resposta.verificacaoPrevia(),
                resposta.orientacaoOperador(),
                apontamentos(resposta.apontamentos())
        );
    }

    private static List<ApontamentoChecklist> apontamentos(
            List<ChecklistMtrResponse.Apontamento> origens
    ) {
        if (origens == null) {
            return null;
        }
        List<ApontamentoChecklist> destinos = new ArrayList<>(origens.size());
        for (ChecklistMtrResponse.Apontamento origem : origens) {
            destinos.add(apontamento(origem));
        }
        return destinos;
    }

    private static ApontamentoChecklist apontamento(ChecklistMtrResponse.Apontamento origem) {
        if (origem == null) {
            return null;
        }
        return new ApontamentoChecklist(
                origem.identificadorNegocial(),
                origem.nome(),
                origem.descricao(),
                origem.orientacaoOperador(),
                origem.indicadorReanalise(),
                origem.sequenciaApresentacao()
        );
    }
}
