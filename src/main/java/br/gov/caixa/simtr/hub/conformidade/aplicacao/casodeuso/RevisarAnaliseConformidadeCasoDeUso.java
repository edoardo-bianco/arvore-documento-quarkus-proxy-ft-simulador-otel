package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.RevisarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.PublicarRevisaoNoWorkflow;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.validacao.ValidadorAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RevisarAnaliseConformidadeCasoDeUso implements RevisarAnaliseConformidade {

    private final ArmazenarEstadoAnaliseConformidade estados;
    private final PublicarRevisaoNoWorkflow publicarRevisao;
    private final ValidadorAnaliseConformidade validador =
            new ValidadorAnaliseConformidade();

    @Inject
    public RevisarAnaliseConformidadeCasoDeUso(
            ArmazenarEstadoAnaliseConformidade estados,
            PublicarRevisaoNoWorkflow publicarRevisao) {
        this.estados = estados;
        this.publicarRevisao = publicarRevisao;
    }

    @Override
    public Uni<Void> executar(
            String instanceId,
            RevisaoHumanaConformidade revisao) {
        return estados.consultar(instanceId)
                .map(atual -> atual.orElseThrow(
                        FalhaAnaliseConformidade::instanciaNaoEncontrada))
                .invoke(atual -> validarRevisao(atual, revisao))
                .chain(() -> estados.reservarRevisao(instanceId, revisao))
                .chain(() -> publicarRevisao.publicar(instanceId, revisao));
    }

    private void validarRevisao(
            VisaoAnaliseConformidade atual,
            RevisaoHumanaConformidade revisao) {
        if (atual.status() != StatusAnaliseConformidade.AGUARDANDO_REVISAO) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }

        atual.validarIdentidadesPersistidas();
        validador.consolidarRevisao(atual.resultadoPreliminar(), revisao);
    }
}
