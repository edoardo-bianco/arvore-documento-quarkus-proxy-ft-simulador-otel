package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import java.util.Optional;

public interface ArmazenarEstadoAnaliseConformidade {

    Uni<Void> iniciar(String instanceId, SolicitacaoAnaliseConformidade solicitacao);

    Uni<SolicitacaoAnaliseConformidade> carregarSolicitacao(String instanceId);

    Uni<Void> registrarChecklist(String instanceId, Checklist checklist);

    Uni<Checklist> carregarChecklist(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia);

    Uni<Void> aguardarRevisao(
            String instanceId,
            ResultadoAnaliseConformidade resultado);

    Uni<Void> prepararResultadoPreliminar(
            String instanceId,
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia);

    Uni<ResultadoAnaliseConformidade> carregarResultadoPreliminar(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia);

    Uni<Void> reservarRevisao(
            String instanceId,
            RevisaoHumanaConformidade revisao);

    Uni<RevisaoHumanaConformidade> carregarRevisao(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia);

    Uni<Void> concluir(
            String instanceId,
            ResultadoAnaliseConformidade resultado);

    Uni<Void> prepararResultadoFinal(
            String instanceId,
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia);

    Uni<Void> registrarEmissao(EmissaoReferencialAnaliseConformidade emissao);

    Uni<Void> falhar(
            String instanceId,
            String mensagem);

    Uni<Optional<VisaoAnaliseConformidade>> consultar(String instanceId);
}
