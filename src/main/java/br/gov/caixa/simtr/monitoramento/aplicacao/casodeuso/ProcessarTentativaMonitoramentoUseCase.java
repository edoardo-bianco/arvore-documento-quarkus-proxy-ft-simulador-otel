package br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.ProcessarTentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarPreValidacao;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarSituacaoDossie;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.PublicarResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento.PoliticaAplicada;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.CatalogoPoliticasMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.CatalogoPoliticasMonitoramento.Resolucao;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Coordena elegibilidade, limites e consultas; confirma resultados sem executar settlement.
 * Versão ausente usa a v1 interna do catálogo, sempre com o prazo original.
 * A intencao de reagendamento e executada pela borda transacional associada a entrega.
 */
@ApplicationScoped
public class ProcessarTentativaMonitoramentoUseCase implements ProcessarTentativaMonitoramento {

    private final ConsultarPreValidacao preValidacao;
    private final ConsultarSituacaoDossie hub;
    private final PublicarResultadoMonitoramento publicacao;
    private final CatalogoPoliticasMonitoramento catalogo;
    private final Clock relogio;

    @Inject
    public ProcessarTentativaMonitoramentoUseCase(ConsultarPreValidacao preValidacao,
            ConsultarSituacaoDossie hub, PublicarResultadoMonitoramento publicacao,
            CatalogoPoliticasMonitoramento catalogo) {
        this(preValidacao, hub, publicacao, catalogo, Clock.systemUTC());
    }

    ProcessarTentativaMonitoramentoUseCase(ConsultarPreValidacao preValidacao,
            ConsultarSituacaoDossie hub, PublicarResultadoMonitoramento publicacao,
            CatalogoPoliticasMonitoramento catalogo, Clock relogio) {
        this.preValidacao = preValidacao;
        this.hub = hub;
        this.publicacao = publicacao;
        this.catalogo = catalogo;
        this.relogio = relogio;
    }

    @Override
    public Uni<DecisaoProcessamento> executar(TentativaMonitoramento tentativa, long inputSequenceNumber) {
        return Uni.createFrom().deferred(() -> {
            Objects.requireNonNull(tentativa, "Tentativa de monitoramento obrigatoria.");
            if (tentativa.tentativaAtual() < 1) {
                throw new IllegalArgumentException("tentativaAtual deve ser maior que zero");
            }
            return preValidacao.executar(tentativa.idDossiePreValidacao()).flatMap(pre -> {
                if (!"EM_ANALISE_ENVIO_MTR".equals(pre.situacao())) {
                    return Uni.createFrom().item(new DecisaoProcessamento.Ignorar());
                }
                return processarElegivel(tentativa, inputSequenceNumber);
            });
        }).memoize().indefinitely();
    }

    private Uni<DecisaoProcessamento> processarElegivel(TentativaMonitoramento tentativa, long sequencia) {
        var resolucao = catalogo.resolver(tentativa.politicaMonitoramentoVersao());
        var agora = relogio.instant();
        int realizadas = tentativa.tentativaAtual() - 1;
        var motivo = resolucao.politica().motivoEncerramento(realizadas, agora, tentativa.limiteEm());
        if (motivo.isPresent()) {
            return publicarQuarentena(tentativa, sequencia, resolucao, motivo.get(), null, realizadas, agora);
        }
        return hub.executar(tentativa.idDossieMtr())
                .flatMap(situacao -> decidirAposConsulta(tentativa, sequencia, resolucao, situacao));
    }

    private Uni<DecisaoProcessamento> decidirAposConsulta(TentativaMonitoramento tentativa,
            long sequencia, Resolucao resolucao, SituacaoDossieConsultada situacao) {
        var agora = relogio.instant();
        String situacaoCalculada = switch (situacao.nome()) {
            case "FINALIZADO_CONFORME" -> "CONFORME";
            case "FINALIZADO_INCONFORME", "PENDENTE_INFORMACA" -> "INCONFORME";
            default -> null;
        };
        // Conclusão de consulta iniciada no prazo tem precedência sobre a expiração em voo.
        if (situacaoCalculada != null) {
            var resultado = new ResultadoMonitoramento(tentativa.monitoramentoId(), tentativa.orquestracaoId(),
                    tentativa.idDossiePreValidacao(), tentativa.idDossieMtr(), "CONCLUSIVO", situacao.nome(),
                    situacaoCalculada, "SITUACAO_CONCLUSIVA_MTR", tentativa.tentativaAtual(),
                    tentativa.iniciadoEm(), agora, sequencia);
            return publicar(resultado, resolucao);
        }
        return switch (resolucao.politica().avaliarTentativaNaoConclusiva(
                tentativa.tentativaAtual(), agora, tentativa.limiteEm())) {
            case PoliticaMonitoramento.Decisao.Encerrar(var motivo) ->
                publicarQuarentena(tentativa, sequencia, resolucao, motivo,
                        situacao.nome(), tentativa.tentativaAtual(), agora);
            case PoliticaMonitoramento.Decisao.Reagendar(var proximaTentativa, var intervalo) ->
                Uni.createFrom().item(new DecisaoProcessamento.ReagendamentoPendente(tentativa,
                        proximaTentativa, intervalo, agora, evidencia(resolucao)));
        };
    }

    private Uni<DecisaoProcessamento> publicarQuarentena(TentativaMonitoramento tentativa,
            long sequencia, Resolucao resolucao, PoliticaMonitoramento.MotivoEncerramento motivo,
            String situacaoMtr, int realizadas, Instant agora) {
        var resultado = new ResultadoMonitoramento(tentativa.monitoramentoId(), tentativa.orquestracaoId(),
                tentativa.idDossiePreValidacao(), tentativa.idDossieMtr(), "QUARENTENA", situacaoMtr,
                "QUARENTENA", motivo.name(), realizadas, tentativa.iniciadoEm(), agora, sequencia);
        return publicar(resultado, resolucao);
    }

    private Uni<DecisaoProcessamento> publicar(ResultadoMonitoramento resultado, Resolucao resolucao) {
        return publicacao.executar(resultado)
                .replaceWith(new DecisaoProcessamento.ResultadoPublicado(resultado, evidencia(resolucao)));
    }

    private static PoliticaAplicada evidencia(Resolucao resolucao) {
        return new PoliticaAplicada(resolucao.versaoSolicitada(), resolucao.politica().versao(),
                resolucao.padraoAplicado());
    }
}
