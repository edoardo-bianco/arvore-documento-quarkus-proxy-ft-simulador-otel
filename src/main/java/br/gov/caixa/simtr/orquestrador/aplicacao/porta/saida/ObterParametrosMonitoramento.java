package br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.ParametrosMonitoramento;
import java.time.Instant;

/** Obtem parametros pela ACL local, sem acesso a politica ou configuracao do fornecedor. */
public interface ObterParametrosMonitoramento {

    /** @param iniciadoEm instante inicial do servidor
     * @return limite e versao traduzidos para tipo proprio; falhas da preparacao sao propagadas */
    ParametrosMonitoramento executar(Instant iniciadoEm);
}
