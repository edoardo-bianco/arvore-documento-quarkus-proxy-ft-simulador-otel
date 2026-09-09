package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada;
import io.smallrye.mutiny.Uni;

/**
 * Consulta a situacao minima do dossie por uma ACL local para a API publica do Hub.
 *
 * <p>A selecao MTR/simulador continua no Hub. O resultado preserva id/nome; a correspondencia
 * com estados conclusivos do monitoramento deve ser definida no processamento de 7.1.
 */
public interface ConsultarSituacaoDossie {

    /**
     * @param idDossieMtr string decimal positiva no intervalo de Long; conversao pertence a ACL
     * @return Uni com id/nome originais; entrada invalida, resposta/situacao ausente e nome vazio
     *         falham sem retorno ficticio; falhas do Hub permanecem assincronas sem retry adicional
     */
    Uni<SituacaoDossieConsultada> executar(String idDossieMtr);
}
