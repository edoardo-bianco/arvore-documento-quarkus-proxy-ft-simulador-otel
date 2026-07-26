package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class AnaliseConformidadeMemoryStore implements ArmazenarEstadoAnaliseConformidade {

    private final ConcurrentMap<String, EstadoArmazenado> estados = new ConcurrentHashMap<>();

    @Override
    public void iniciar(String instanceId) {
        validarInstanceId(instanceId);
        var novo = new EstadoArmazenado(
                VisaoAnaliseConformidade.emProcessamento(instanceId),
                false);
        if (estados.putIfAbsent(instanceId, novo) != null) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }

    @Override
    public void aguardarRevisao(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        validarInstanceId(instanceId);
        validarResultadoPreliminar(resultado);
        estados.compute(instanceId, (id, atual) -> {
            EstadoArmazenado encontrado = exigirEstado(atual);
            if (encontrado.visao().status() != StatusAnaliseConformidade.EM_PROCESSAMENTO) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            return new EstadoArmazenado(
                    VisaoAnaliseConformidade.aguardandoRevisao(id, resultado),
                    false);
        });
    }

    @Override
    public void reservarRevisao(String instanceId) {
        validarInstanceId(instanceId);
        estados.compute(instanceId, (id, atual) -> {
            EstadoArmazenado encontrado = exigirEstado(atual);
            if (encontrado.visao().status() != StatusAnaliseConformidade.AGUARDANDO_REVISAO
                    || encontrado.revisaoReservada()) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            return new EstadoArmazenado(encontrado.visao(), true);
        });
    }

    @Override
    public void concluir(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        validarInstanceId(instanceId);
        validarResultadoFinal(resultado);
        estados.compute(instanceId, (id, atual) -> {
            EstadoArmazenado encontrado = exigirEstado(atual);
            if (encontrado.visao().status() != StatusAnaliseConformidade.AGUARDANDO_REVISAO
                    || !encontrado.revisaoReservada()) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            return new EstadoArmazenado(
                    VisaoAnaliseConformidade.concluida(
                            id,
                            encontrado.visao().resultadoPreliminar(),
                            resultado),
                    true);
        });
    }

    @Override
    public void falhar(String instanceId, String mensagem) {
        validarInstanceId(instanceId);
        if (mensagem == null || mensagem.isBlank()) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        estados.compute(instanceId, (id, atual) -> {
            EstadoArmazenado encontrado = exigirEstado(atual);
            StatusAnaliseConformidade status = encontrado.visao().status();
            if (status != StatusAnaliseConformidade.EM_PROCESSAMENTO
                    && status != StatusAnaliseConformidade.AGUARDANDO_REVISAO) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            return new EstadoArmazenado(
                    VisaoAnaliseConformidade.falhou(
                            id,
                            encontrado.visao().resultadoPreliminar(),
                            mensagem),
                    encontrado.revisaoReservada());
        });
    }

    @Override
    public Optional<VisaoAnaliseConformidade> consultar(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(estados.get(instanceId))
                .map(EstadoArmazenado::visao);
    }

    private static void validarInstanceId(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }

    private static EstadoArmazenado exigirEstado(EstadoArmazenado estado) {
        if (estado == null) {
            throw FalhaAnaliseConformidade.instanciaNaoEncontrada();
        }
        return estado;
    }

    private static void validarResultadoPreliminar(ResultadoAnaliseConformidade resultado) {
        if (resultado == null || resultado.origem() == OrigemResultado.REVISAO_HUMANA) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O estado de revisão exige resultado preliminar válido");
        }
    }

    private static void validarResultadoFinal(ResultadoAnaliseConformidade resultado) {
        if (resultado == null || resultado.origem() != OrigemResultado.REVISAO_HUMANA) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "A conclusão exige resultado de revisão humana");
        }
    }

    private record EstadoArmazenado(
            VisaoAnaliseConformidade visao,
            boolean revisaoReservada) {
    }
}
