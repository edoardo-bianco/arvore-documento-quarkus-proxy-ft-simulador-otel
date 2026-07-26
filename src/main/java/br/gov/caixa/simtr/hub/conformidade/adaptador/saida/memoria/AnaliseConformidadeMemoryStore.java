package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class AnaliseConformidadeMemoryStore implements ArmazenarEstadoAnaliseConformidade {

    private final ConcurrentMap<String, EstadoArmazenado> estados = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> iniciar(
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        return executar(() -> {
            validarInstanceId(instanceId);
            if (solicitacao == null) {
                throw FalhaAnaliseConformidade.solicitacaoInvalida(
                        "A solicitação da análise é obrigatória");
            }
            var novo = new EstadoArmazenado(
                    VisaoAnaliseConformidade.emProcessamento(
                            solicitacao.correlationId(),
                            instanceId,
                            solicitacao.identificadorDocumento(),
                            solicitacao.identificadorChecklist(),
                            solicitacao.versaoChecklist()),
                    null,
                    null);
            if (estados.putIfAbsent(instanceId, novo) != null) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
        });
    }

    @Override
    public Uni<Void> registrarChecklist(String instanceId, Checklist checklist) {
        return executar(() -> {
            validarInstanceId(instanceId);
            estados.compute(instanceId, (id, atual) -> {
                EstadoArmazenado encontrado = exigirEstado(atual);
                validarChecklist(encontrado.visao(), checklist);
                Checklist congelado = congelar(checklist);
                if (encontrado.checklist() != null && !encontrado.checklist().equals(congelado)) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return new EstadoArmazenado(
                        encontrado.visao(),
                        congelado,
                        encontrado.revisao());
            });
        });
    }

    @Override
    public Uni<Void> aguardarRevisao(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        return executar(() -> {
            validarInstanceId(instanceId);
            validarResultadoPreliminar(resultado);
            estados.compute(instanceId, (id, atual) -> {
                EstadoArmazenado encontrado = exigirEstado(atual);
                if (encontrado.visao().status() != StatusAnaliseConformidade.EM_PROCESSAMENTO
                        || encontrado.checklist() == null) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return new EstadoArmazenado(
                        encontrado.visao().aguardandoRevisao(resultado),
                        encontrado.checklist(),
                        null);
            });
        });
    }

    @Override
    public Uni<Void> reservarRevisao(
            String instanceId,
            RevisaoHumanaConformidade revisao) {
        return executar(() -> {
            validarInstanceId(instanceId);
            if (revisao == null) {
                throw FalhaAnaliseConformidade.revisaoInconsistente(
                        "A revisão humana é obrigatória");
            }
            estados.compute(instanceId, (id, atual) -> {
                EstadoArmazenado encontrado = exigirEstado(atual);
                if (encontrado.visao().status() != StatusAnaliseConformidade.AGUARDANDO_REVISAO) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                if (encontrado.revisao() != null) {
                    if (encontrado.revisao().equals(revisao)) {
                        return encontrado;
                    }
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return new EstadoArmazenado(
                        encontrado.visao(),
                        encontrado.checklist(),
                        revisao);
            });
        });
    }

    @Override
    public Uni<Void> concluir(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        return executar(() -> {
            validarInstanceId(instanceId);
            validarResultadoFinal(resultado);
            estados.compute(instanceId, (id, atual) -> {
                EstadoArmazenado encontrado = exigirEstado(atual);
                if (encontrado.visao().status() != StatusAnaliseConformidade.AGUARDANDO_REVISAO
                        || encontrado.revisao() == null) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return new EstadoArmazenado(
                        encontrado.visao().concluida(resultado),
                        encontrado.checklist(),
                        encontrado.revisao());
            });
        });
    }

    @Override
    public Uni<Void> falhar(String instanceId, String mensagem) {
        return executar(() -> {
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
                        encontrado.visao().falhou(mensagem),
                        encontrado.checklist(),
                        encontrado.revisao());
            });
        });
    }

    @Override
    public Uni<Optional<VisaoAnaliseConformidade>> consultar(String instanceId) {
        return Uni.createFrom().item(() -> {
            if (instanceId == null || instanceId.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(estados.get(instanceId))
                    .map(EstadoArmazenado::visao);
        });
    }

    private static Uni<Void> executar(Runnable operacao) {
        return Uni.createFrom().item(() -> {
            operacao.run();
            return null;
        });
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

    private static void validarChecklist(
            VisaoAnaliseConformidade visao,
            Checklist checklist) {
        if (checklist == null
                || checklist.apontamentos() == null
                || checklist.apontamentos().isEmpty()
                || !visao.identificadorChecklist().equals(checklist.identificadorNegocial())
                || !visao.versaoChecklist().equals(checklist.versao())) {
            throw FalhaAnaliseConformidade.checklistInvalido(
                    "O checklist não corresponde à análise");
        }
    }

    private static Checklist congelar(Checklist checklist) {
        return new Checklist(
                checklist.nome(),
                checklist.identificadorNegocial(),
                checklist.versao(),
                checklist.dataHoraCriacao(),
                checklist.dataHoraUltimaAlteracao(),
                checklist.verificacaoPrevia(),
                checklist.orientacaoOperador(),
                java.util.List.copyOf(checklist.apontamentos()));
    }

    private record EstadoArmazenado(
            VisaoAnaliseConformidade visao,
            Checklist checklist,
            RevisaoHumanaConformidade revisao) {
    }
}
