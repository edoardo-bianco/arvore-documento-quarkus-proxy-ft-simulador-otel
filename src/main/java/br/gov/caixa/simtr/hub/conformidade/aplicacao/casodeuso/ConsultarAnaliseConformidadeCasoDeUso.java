package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
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
    public Uni<VisaoAnaliseConformidade> executar(String instanceId) {
        return estados.consultar(instanceId)
                .map(atual -> atual.orElseThrow(
                        FalhaAnaliseConformidade::instanciaNaoEncontrada));
    }
}
