package br.gov.caixa.simtr.monitoramento.adaptador.configuracao;

import java.util.HashMap;
import java.util.Map;

import br.gov.caixa.simtr.monitoramento.dominio.politica.CatalogoPoliticasMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramentoProgressiva;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

// Inicializa e valida mesmo sem consumidor da politica; o metodo produtor apenas entrega o resultado.
// https://quarkus.io/guides/lifecycle/#using-startup-to-initialize-a-cdi-bean-at-application-startup
@Startup
@ApplicationScoped
public class PoliticaMonitoramentoProducer {

    private final PoliticaMonitoramento politicaSelecionada;
    private final CatalogoPoliticasMonitoramento catalogo;

    @Inject
    public PoliticaMonitoramentoProducer(PoliticasMonitoramentoConfig config) {
        if (config.ativa().isBlank() || !config.definicoes().containsKey(config.ativa())) {
            throw new IllegalArgumentException("monitoramento.politicas.ativa deve selecionar uma definicao existente");
        }

        Map<String, PoliticaMonitoramento> politicas = new HashMap<>();
        config.definicoes().forEach((nome, definicao) -> politicas.put(nome, switch (definicao.tipo()) {
            case PROGRESSIVA -> new PoliticaMonitoramentoProgressiva(
                    definicao.versao(), definicao.intervalos(),
                    definicao.maxTentativas(), definicao.duracaoMaxima());
        }));
        politicaSelecionada = politicas.get(config.ativa());
        catalogo = new CatalogoPoliticasMonitoramento(politicas.values());
    }

    @Produces
    @Singleton
    PoliticaMonitoramento politica() {
        return politicaSelecionada;
    }

    @Produces
    @Singleton
    CatalogoPoliticasMonitoramento catalogo() {
        return catalogo;
    }
}
