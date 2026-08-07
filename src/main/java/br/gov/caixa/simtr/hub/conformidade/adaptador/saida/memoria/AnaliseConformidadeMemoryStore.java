package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.memoria;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.EmissaoReferencialAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ReferenciaDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.OrigemResultado;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.StatusAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnaliseConformidadeMemoryStore implements ArmazenarEstadoAnaliseConformidade {

    private final ConcurrentMap<String, EstadoArmazenado> estados = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SolicitacaoAnaliseConformidade> solicitacoes =
            new ConcurrentHashMap<>();
    private final ReferenciasDocumentoAnaliseConformidade referencias =
            new ReferenciasDocumentoAnaliseConformidade(new ObjectMapper());

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
                    null,
                    null,
                    null);
            if (estados.putIfAbsent(instanceId, novo) != null) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            solicitacoes.put(instanceId, solicitacao);
        });
    }

    @Override
    public Uni<SolicitacaoAnaliseConformidade> carregarSolicitacao(String instanceId) {
        return Uni.createFrom().item(() -> {
            validarInstanceId(instanceId);
            exigirEstado(estados.get(instanceId));
            return exigirConteudo(solicitacoes.get(instanceId));
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
                        encontrado.resultadoPreliminar(),
                        encontrado.resultadoFinal(),
                        encontrado.revisao());
            });
        });
    }

    @Override
    public Uni<Checklist> carregarChecklist(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return Uni.createFrom().item(() -> {
            validarInstanceId(instanceId);
            EstadoArmazenado estado = exigirEstado(estados.get(instanceId));
            Checklist checklist = exigirConteudo(estado.checklist());
            exigirReferencia(
                    referencias.checklist(estado.visao().correlationId(), checklist),
                    referencia);
            return checklist;
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
                        encontrado.resultadoPreliminar(),
                        encontrado.resultadoFinal(),
                        null);
            });
        });
    }

    @Override
    public Uni<Void> prepararResultadoPreliminar(
            String instanceId,
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return executar(() -> {
            validarInstanceId(instanceId);
            validarResultadoPreliminar(resultado);
            estados.compute(instanceId, (id, atual) -> {
                EstadoArmazenado encontrado = exigirEstado(atual);
                if (encontrado.visao().status() != StatusAnaliseConformidade.EM_PROCESSAMENTO
                        || encontrado.checklist() == null) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                encontrado.visao().aguardandoRevisao(resultado);
                var esperada = referencias.resultadoPreliminar(
                        encontrado.visao().correlationId(), resultado);
                exigirReferencia(esperada, referencia);
                var preparado = new ResultadoPreparado(resultado, referencia);
                if (encontrado.resultadoPreliminar() != null
                        && !encontrado.resultadoPreliminar().equals(preparado)) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return new EstadoArmazenado(
                        encontrado.visao(),
                        encontrado.checklist(),
                        preparado,
                        encontrado.resultadoFinal(),
                        encontrado.revisao());
            });
        });
    }

    @Override
    public Uni<ResultadoAnaliseConformidade> carregarResultadoPreliminar(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return Uni.createFrom().item(() -> {
            validarInstanceId(instanceId);
            EstadoArmazenado estado = exigirEstado(estados.get(instanceId));
            ResultadoAnaliseConformidade resultado = estado.resultadoPreliminar() == null
                    ? estado.visao().resultadoPreliminar()
                    : estado.resultadoPreliminar().resultado();
            resultado = exigirConteudo(resultado);
            exigirReferencia(
                    referencias.resultadoPreliminar(
                            estado.visao().correlationId(), resultado),
                    referencia);
            return resultado;
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
                        encontrado.resultadoPreliminar(),
                        encontrado.resultadoFinal(),
                        revisao);
            });
        });
    }

    @Override
    public Uni<RevisaoHumanaConformidade> carregarRevisao(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return Uni.createFrom().item(() -> {
            validarInstanceId(instanceId);
            EstadoArmazenado estado = exigirEstado(estados.get(instanceId));
            RevisaoHumanaConformidade revisao = exigirConteudo(estado.revisao());
            exigirReferencia(
                    referencias.revisao(estado.visao().correlationId(), revisao),
                    referencia);
            return revisao;
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
                        encontrado.resultadoPreliminar(),
                        encontrado.resultadoFinal(),
                        encontrado.revisao());
            });
        });
    }

    @Override
    public Uni<Void> prepararResultadoFinal(
            String instanceId,
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return executar(() -> {
            validarInstanceId(instanceId);
            validarResultadoFinal(resultado);
            estados.compute(instanceId, (id, atual) -> {
                EstadoArmazenado encontrado = exigirEstado(atual);
                if (encontrado.visao().status() != StatusAnaliseConformidade.AGUARDANDO_REVISAO
                        || encontrado.revisao() == null) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                encontrado.visao().concluida(resultado);
                var esperada = referencias.resultadoFinal(
                        encontrado.visao().correlationId(), resultado);
                exigirReferencia(esperada, referencia);
                var preparado = new ResultadoPreparado(resultado, referencia);
                if (encontrado.resultadoFinal() != null
                        && !encontrado.resultadoFinal().equals(preparado)) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return new EstadoArmazenado(
                        encontrado.visao(),
                        encontrado.checklist(),
                        encontrado.resultadoPreliminar(),
                        preparado,
                        encontrado.revisao());
            });
        });
    }

    @Override
    public Uni<Void> registrarEmissao(EmissaoReferencialAnaliseConformidade emissao) {
        return executar(() -> {
            if (emissao == null) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            estados.compute(emissao.instanceId(), (id, atual) -> {
                EstadoArmazenado encontrado = exigirEstado(atual);
                if (!encontrado.visao().correlationId().equals(emissao.correlationId())) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return switch (emissao.tipo()) {
                    case REVISAO_SOLICITADA -> registrarSolicitacaoRevisao(
                            encontrado, emissao.documento());
                    case ANALISE_CONCLUIDA -> registrarAnaliseConcluida(
                            encontrado, emissao.documento());
                };
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
                        encontrado.resultadoPreliminar(),
                        encontrado.resultadoFinal(),
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

    private static <T> T exigirConteudo(T conteudo) {
        if (conteudo == null) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        return conteudo;
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

    private static EstadoArmazenado registrarSolicitacaoRevisao(
            EstadoArmazenado estado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        ResultadoPreparado preparado = estado.resultadoPreliminar();
        if (preparado == null || !preparado.referencia().equals(referencia)) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        if (estado.visao().status() == StatusAnaliseConformidade.AGUARDANDO_REVISAO
                && preparado.resultado().equals(estado.visao().resultadoPreliminar())) {
            return estado;
        }
        if (estado.visao().status() != StatusAnaliseConformidade.EM_PROCESSAMENTO) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        return new EstadoArmazenado(
                estado.visao().aguardandoRevisao(preparado.resultado()),
                estado.checklist(),
                preparado,
                estado.resultadoFinal(),
                estado.revisao());
    }

    private static EstadoArmazenado registrarAnaliseConcluida(
            EstadoArmazenado estado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        ResultadoPreparado preparado = estado.resultadoFinal();
        if (preparado == null || !preparado.referencia().equals(referencia)) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        if (estado.visao().status() == StatusAnaliseConformidade.CONCLUIDA
                && preparado.resultado().equals(estado.visao().resultadoFinal())) {
            return estado;
        }
        if (estado.visao().status() != StatusAnaliseConformidade.AGUARDANDO_REVISAO
                || estado.revisao() == null) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        return new EstadoArmazenado(
                estado.visao().concluida(preparado.resultado()),
                estado.checklist(),
                estado.resultadoPreliminar(),
                preparado,
                estado.revisao());
    }

    private static void exigirReferencia(
            ReferenciaDocumentoAnaliseConformidade esperada,
            ReferenciaDocumentoAnaliseConformidade recebida) {
        if (!esperada.equals(recebida)) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }

    private record EstadoArmazenado(
            VisaoAnaliseConformidade visao,
            Checklist checklist,
            ResultadoPreparado resultadoPreliminar,
            ResultadoPreparado resultadoFinal,
            RevisaoHumanaConformidade revisao) {
    }

    private record ResultadoPreparado(
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
    }
}
