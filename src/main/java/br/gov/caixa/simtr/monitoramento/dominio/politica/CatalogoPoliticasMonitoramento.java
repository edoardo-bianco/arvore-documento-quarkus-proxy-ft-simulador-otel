package br.gov.caixa.simtr.monitoramento.dominio.politica;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Resolve a versao recebida sem depender da politica ativa para novas iniciacoes. */
public final class CatalogoPoliticasMonitoramento {

    private static final PoliticaMonitoramento PADRAO_V1 = new PoliticaMonitoramentoProgressiva(
            "v1", List.of(Duration.ofMinutes(30)),
            OptionalInt.empty(), Duration.ofHours(24));

    private final Map<String, PoliticaMonitoramento> porVersao;

    public CatalogoPoliticasMonitoramento(Collection<PoliticaMonitoramento> politicas) {
        var definicoes = new HashMap<String, PoliticaMonitoramento>();
        for (var politica : politicas) {
            if (definicoes.putIfAbsent(politica.versao(), politica) != null) {
                throw new IllegalArgumentException("versoes das politicas devem ser unicas");
            }
        }
        porVersao = Map.copyOf(definicoes);
    }

    /**
     * Usa a definicao configurada ou os valores fixos da v1 quando a versao foi removida.
     * O chamador preserva a tentativa e avalia a politica com o limite recebido, sem recalcula-lo.
     *
     * @param versaoRecebida versao nao vazia transportada pela mensagem validada
     * @return politica efetiva e evidencia de recuperacao, sem alterar a versao solicitada
     */
    public Resolucao resolver(String versaoRecebida) {
        if (versaoRecebida == null || versaoRecebida.isBlank()) {
            throw new IllegalArgumentException("versao recebida deve ser informada");
        }
        var configurada = porVersao.get(versaoRecebida);
        if (configurada != null) {
            return new Resolucao(versaoRecebida, configurada, false);
        }
        return new Resolucao(versaoRecebida, PADRAO_V1, true);
    }

    /** Distingue a versao pedida da politica aplicada, inclusive quando ambas se chamam v1. */
    public record Resolucao(
            String versaoSolicitada,
            PoliticaMonitoramento politica,
            boolean padraoAplicado) {
    }
}
