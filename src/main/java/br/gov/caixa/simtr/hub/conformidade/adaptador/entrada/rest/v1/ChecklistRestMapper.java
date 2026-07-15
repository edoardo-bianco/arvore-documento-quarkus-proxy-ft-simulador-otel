package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.checklist.ChecklistApontamentoDto;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import java.util.ArrayList;
import java.util.List;

final class ChecklistRestMapper {

    private ChecklistRestMapper() {
    }

    static ChecklistDto paraResposta(Checklist checklist) {
        if (checklist == null) {
            return null;
        }
        return new ChecklistDto(
                checklist.nome(),
                checklist.identificadorNegocial(),
                checklist.versao(),
                checklist.dataHoraCriacao(),
                checklist.dataHoraUltimaAlteracao(),
                checklist.verificacaoPrevia(),
                checklist.orientacaoOperador(),
                apontamentos(checklist.apontamentos())
        );
    }

    static Throwable paraExcecaoRest(FalhaConsultaChecklist falha) {
        int status = falha.status() != null ? falha.status() : 500;
        ErroPadraoDto erro = new ErroPadraoDto(
                falha.status(),
                falha.recurso(),
                falha.idErro(),
                falha.codigoErro(),
                mensagens(falha.mensagens()),
                falha.detalhe(),
                falha.stacktraceExterno()
        );

        return switch (falha.tipo()) {
            case NEGOCIO -> new MtrBusinessErrorException(status, erro);
            case TECNICA_CLIENTE -> new MtrClientTechnicalException(status, erro);
            case DEPENDENCIA_INDISPONIVEL, TIMEOUT ->
                    new MtrServerErrorException(status, erro);
        };
    }

    private static List<ChecklistApontamentoDto> apontamentos(
            List<ApontamentoChecklist> apontamentos
    ) {
        if (apontamentos == null) {
            return null;
        }
        List<ChecklistApontamentoDto> resposta = new ArrayList<>(apontamentos.size());
        for (ApontamentoChecklist apontamento : apontamentos) {
            resposta.add(apontamento(apontamento));
        }
        return resposta;
    }

    private static ChecklistApontamentoDto apontamento(ApontamentoChecklist apontamento) {
        if (apontamento == null) {
            return null;
        }
        return new ChecklistApontamentoDto(
                apontamento.identificadorNegocial(),
                apontamento.nome(),
                apontamento.descricao(),
                apontamento.orientacaoOperador(),
                apontamento.indicadorReanalise(),
                apontamento.sequenciaApresentacao()
        );
    }

    private static List<ErroMensagemDto> mensagens(List<String> mensagens) {
        if (mensagens == null) {
            return null;
        }
        List<ErroMensagemDto> resposta = new ArrayList<>(mensagens.size());
        for (String mensagem : mensagens) {
            resposta.add(mensagem != null ? new ErroMensagemDto(mensagem) : null);
        }
        return resposta;
    }
}
