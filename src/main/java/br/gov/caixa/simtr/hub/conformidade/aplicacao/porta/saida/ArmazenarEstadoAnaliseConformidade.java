package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import java.util.Optional;

public interface ArmazenarEstadoAnaliseConformidade {

    void iniciar(String instanceId);

    void aguardarRevisao(
            String instanceId,
            ResultadoAnaliseConformidade resultado);

    void reservarRevisao(String instanceId);

    void concluir(
            String instanceId,
            ResultadoAnaliseConformidade resultado);

    void falhar(
            String instanceId,
            String mensagem);

    Optional<VisaoAnaliseConformidade> consultar(String instanceId);
}
