package br.gov.caixa.simtr.monitoramento.adaptador.configuracao;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import io.smallrye.config.ConfigMapping;

// https://quarkus.io/guides/config-mappings/#maps
@ConfigMapping(prefix = "monitoramento.politicas")
public interface PoliticasMonitoramentoConfig {

    String ativa();

    Map<String, Definicao> definicoes();

    interface Definicao {
        String versao();

        Tipo tipo();

        List<Duration> intervalos();

        OptionalInt maxTentativas();

        Duration duracaoMaxima();
    }

    enum Tipo {
        PROGRESSIVA
    }
}
