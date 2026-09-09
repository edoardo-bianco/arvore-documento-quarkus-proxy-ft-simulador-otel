package br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao.dto.PreValidacaoSimuladaDto;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.PreValidacaoConsultada;
import jakarta.enterprise.context.ApplicationScoped;

/** Traduz a borda simulada para a visao propria do monitoramento, sem decidir elegibilidade. */
@ApplicationScoped
public class PreValidacaoSimuladaMapper {

    /** Preserva o texto original e identifica a origem simulada, rejeitando resposta ausente. */
    public PreValidacaoConsultada paraModelo(PreValidacaoSimuladaDto resposta) {
        if (resposta == null) {
            throw new IllegalArgumentException("Resposta simulada de pre-validacao ausente.");
        }
        return new PreValidacaoConsultada(resposta.situacao(), true);
    }
}
