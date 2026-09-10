package br.gov.caixa.simtr.monitoramento.dominio.politica;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Optional;

public final class PoliticaMonitoramentoProgressiva implements PoliticaMonitoramento {

    private final String versao;
    private final List<Duration> intervalos;
    private final OptionalInt maximoTentativas;
    private final Duration duracaoMaxima;

    public PoliticaMonitoramentoProgressiva(
            String versao,
            List<Duration> intervalos,
            OptionalInt maximoTentativas,
            Duration duracaoMaxima) {
        this.versao = validarVersao(versao);
        this.intervalos = validarIntervalos(intervalos);
        this.maximoTentativas = validarMaximoTentativas(maximoTentativas);
        this.duracaoMaxima = validarDuracaoMaxima(duracaoMaxima);
    }

    @Override
    public String versao() {
        return versao;
    }

    @Override
    public Instant calcularLimite(Instant iniciadoEm) {
        return Objects.requireNonNull(iniciadoEm, "iniciadoEm").plus(duracaoMaxima);
    }

    @Override
    public Decisao avaliarTentativaNaoConclusiva(
            int tentativaAtual,
            Instant processadoEm,
            Instant limiteEm) {
        if (tentativaAtual < 1) {
            throw new IllegalArgumentException("tentativaAtual deve ser maior que zero");
        }
        var motivo = motivoEncerramento(tentativaAtual, processadoEm, limiteEm);
        if (motivo.isPresent()) {
            return new Decisao.Encerrar(motivo.get());
        }

        int indiceIntervalo = Math.min(tentativaAtual - 1, intervalos.size() - 1);
        return new Decisao.Reagendar(
                Math.addExact(tentativaAtual, 1),
                intervalos.get(indiceIntervalo));
    }

    @Override
    public Optional<MotivoEncerramento> motivoEncerramento(
            int tentativasRealizadas, Instant processadoEm, Instant limiteEm) {
        if (tentativasRealizadas < 0) {
            throw new IllegalArgumentException("tentativasRealizadas nao pode ser negativo");
        }
        Objects.requireNonNull(processadoEm, "processadoEm");
        Objects.requireNonNull(limiteEm, "limiteEm");
        if (!processadoEm.isBefore(limiteEm)) {
            return Optional.of(MotivoEncerramento.PRAZO_MAXIMO);
        }
        if (tentativasRealizadas == Integer.MAX_VALUE
                || (maximoTentativas.isPresent() && tentativasRealizadas >= maximoTentativas.getAsInt())) {
            return Optional.of(MotivoEncerramento.MAXIMO_TENTATIVAS);
        }
        return Optional.empty();
    }

    private static String validarVersao(String versao) {
        if (versao == null || versao.isBlank()) {
            throw new IllegalArgumentException("versao deve ser informada");
        }
        return versao;
    }

    private static List<Duration> validarIntervalos(List<Duration> intervalos) {
        if (intervalos == null || intervalos.isEmpty()) {
            throw new IllegalArgumentException("ao menos um intervalo deve ser informado");
        }
        if (intervalos.stream().anyMatch(PoliticaMonitoramentoProgressiva::naoPositivo)) {
            throw new IllegalArgumentException("intervalos devem ser positivos");
        }
        return List.copyOf(intervalos);
    }

    private static OptionalInt validarMaximoTentativas(OptionalInt maximoTentativas) {
        Objects.requireNonNull(maximoTentativas, "maximoTentativas");
        if (maximoTentativas.isPresent() && maximoTentativas.getAsInt() < 1) {
            throw new IllegalArgumentException("maximoTentativas deve ser maior que zero");
        }
        return maximoTentativas;
    }

    private static Duration validarDuracaoMaxima(Duration duracaoMaxima) {
        if (naoPositivo(duracaoMaxima)) {
            throw new IllegalArgumentException("duracaoMaxima deve ser positiva");
        }
        return duracaoMaxima;
    }

    private static boolean naoPositivo(Duration duracao) {
        return duracao == null || duracao.isZero() || duracao.isNegative();
    }
}
