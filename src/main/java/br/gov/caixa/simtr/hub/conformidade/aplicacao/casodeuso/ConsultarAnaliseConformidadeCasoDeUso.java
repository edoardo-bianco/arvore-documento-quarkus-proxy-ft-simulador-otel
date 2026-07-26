package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConsultarAnaliseConformidadeCasoDeUso implements ConsultarAnaliseConformidade {

    private final ArmazenarEstadoAnaliseConformidade estados;

    @Inject
    public ConsultarAnaliseConformidadeCasoDeUso(
            ArmazenarEstadoAnaliseConformidade estados) {
        this.estados = estados;
    }

    @Override
    public VisaoAnaliseConformidade executar(String instanceId) {
        return estados.consultar(instanceId)
                .orElseThrow(FalhaAnaliseConformidade::instanciaNaoEncontrada);
    }
}
