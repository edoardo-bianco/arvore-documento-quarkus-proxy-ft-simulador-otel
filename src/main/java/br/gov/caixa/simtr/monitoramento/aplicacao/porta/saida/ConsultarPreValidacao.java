package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.PreValidacaoConsultada;
import io.smallrye.mutiny.Uni;

/**
 * Consulta a situacao de pre-validacao por modelo proprio do monitoramento.
 *
 * <p>O adapter simulado de 5.1 exige ativacao explicita. Esta porta nao consulta banco,
 * persiste transicoes ou decide no-op/quarentena. O processamento futuro avalia elegibilidade.
 */
public interface ConsultarPreValidacao {

    /**
     * @param idDossiePreValidacao identificador textual nao vazio, sem conversao para UUID
     * @return Uni com situacao original e origem; entrada invalida, ausencia e fonte desabilitada
     *         sao falhas assincronas distintas, nunca situacao nao elegivel ou sucesso ficticio
     */
    Uni<PreValidacaoConsultada> executar(String idDossiePreValidacao);
}
