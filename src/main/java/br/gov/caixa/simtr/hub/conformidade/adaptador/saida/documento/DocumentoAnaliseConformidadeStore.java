package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Consumer;

public class DocumentoAnaliseConformidadeStore
        implements ArmazenarEstadoAnaliseConformidade {

    public static final Short VERSAO_SCHEMA = 1;
    private static final String TIPO_ENTRADA = "entrada-analise";
    private static final String TIPO_PROJECAO = "projecao-analise";
    private static final String TIPO_CHECKLIST = "checklist-analise";
    private static final String TIPO_RESULTADO_PRELIMINAR = "resultado-preliminar";
    private static final String TIPO_REVISAO = "revisao-humana";
    private static final String TIPO_RESULTADO_FINAL = "resultado-final";
    private static final String TIPO_FALHA = "falha-analise";
    private static final String TIPO_EMISSAO = "emissao-cloud-event";
    private static final String CAMPO_CORRELATION_ID = "correlationId";
    private static final String CAMPO_INSTANCE_ID = "instanceId";
    private static final String CAMPO_IDENTIFICADOR_DOCUMENTO = "identificadorDocumento";
    private static final String CAMPO_IDENTIFICADOR_CHECKLIST = "identificadorChecklist";
    private static final String CAMPO_VERSAO_CHECKLIST = "versaoChecklist";
    private static final String CAMPO_VERSAO_SCHEMA = "versaoSchema";
    private static final String CAMPO_STATUS = "status";
    private static final String CAMPO_CHECKLIST_REF = "checklistRef";
    private static final String CAMPO_CHECKLIST_HASH = "checklistHash";
    private static final String CAMPO_HASH_CONTEUDO = "hashConteudo";
    private static final String CAMPO_CHECKLIST = "checklist";
    private static final String CAMPO_RESULTADO = "resultado";
    private static final String CAMPO_REVISAO = "revisao";
    private static final String CAMPO_REVISAO_REF = "revisaoRef";
    private static final String CAMPO_RESULTADO_PRELIMINAR_REF = "resultadoPreliminarRef";
    private static final String CAMPO_RESULTADO_FINAL_REF = "resultadoFinalRef";

    private final RepositorioDocumental repositorio;
    private final ObjectMapper objectMapper;
    private final ReferenciasDocumentoAnaliseConformidade referencias;

    protected DocumentoAnaliseConformidadeStore(
            RepositorioDocumental repositorio,
            ObjectMapper objectMapper) {
        this.repositorio = java.util.Objects.requireNonNull(
                repositorio,
                "repositorio");
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
        this.referencias = new ReferenciasDocumentoAnaliseConformidade(objectMapper);
    }

    @Override
    public Uni<Void> iniciar(
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        return Uni.createFrom().deferred(() -> {
            validarInstanceId(instanceId);
            if (solicitacao == null) {
                throw FalhaAnaliseConformidade.solicitacaoInvalida(
                        "A solicitação da análise é obrigatória");
            }
            return gravarImutavel(
                            IdsDocumentoAnaliseConformidade.entrada(
                                    solicitacao.correlationId()),
                            documentoEntrada(instanceId, solicitacao))
                    .chain(() -> gravarNovo(
                            IdsDocumentoAnaliseConformidade.projecao(instanceId),
                            documentoProjecao(instanceId, solicitacao)));
        });
    }

    @Override
    public Uni<SolicitacaoAnaliseConformidade> carregarSolicitacao(String instanceId) {
        return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
            JsonNode projecao = projecaoPersistida.documento();
            String correlationId = texto(projecao, CAMPO_CORRELATION_ID);
            return consultarDocumento(
                            IdsDocumentoAnaliseConformidade.entrada(correlationId),
                            correlationId)
                    .map(documentoOpcional -> {
                        JsonNode documento = documentoOpcional.orElseThrow(
                                FalhaAnaliseConformidade::indisponibilidadeTecnica);
                        exigirTipoEVersao(documento, TIPO_ENTRADA);
                        if (!instanceId.equals(texto(documento, CAMPO_INSTANCE_ID))) {
                            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
                        }
                        try {
                            return new SolicitacaoAnaliseConformidade(
                                    texto(documento, CAMPO_CORRELATION_ID),
                                    texto(documento, CAMPO_IDENTIFICADOR_DOCUMENTO),
                                    texto(documento, "texto"),
                                    inteiroLongo(documento, CAMPO_IDENTIFICADOR_CHECKLIST),
                                    inteiro(documento, CAMPO_VERSAO_CHECKLIST));
                        } catch (FalhaAnaliseConformidade _) {
                            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
                        }
                    });
        });
    }

    @Override
    public Uni<Void> registrarChecklist(String instanceId, Checklist checklist) {
        return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
            ObjectNode projecao = projecaoPersistida.documento();
            exigirStatus(projecao, StatusAnaliseConformidade.EM_PROCESSAMENTO);
            validarChecklist(projecao, checklist);
            String referencia = IdsDocumentoAnaliseConformidade.checklist(
                    texto(projecao, CAMPO_CORRELATION_ID));
            JsonNode snapshot = objectMapper.valueToTree(checklist);
            String hash = hashCanonico(snapshot);
            return gravarImutavel(
                            referencia,
                            documentoFato(referencia, TIPO_CHECKLIST, projecao)
                                    .put(CAMPO_HASH_CONTEUDO, hash)
                                    .set(CAMPO_CHECKLIST, snapshot))
                    .chain(() -> {
                        if (referencia.equals(textoOpcional(
                                projecao,
                                CAMPO_CHECKLIST_REF))) {
                            return Uni.createFrom().voidItem();
                        }
                        return atualizarProjecao(projecaoPersistida, atualizada -> atualizada
                                .put(CAMPO_CHECKLIST_REF, referencia)
                                .put(CAMPO_CHECKLIST_HASH, hash));
                    });
        });
    }

    @Override
    public Uni<Checklist> carregarChecklist(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return carregarDocumentoReferenciado(
                        instanceId,
                        referencia,
                        CAMPO_CHECKLIST_REF,
                        TIPO_CHECKLIST)
                .map(documento -> {
                    Checklist checklist = converter(
                            documento, CAMPO_CHECKLIST, Checklist.class);
                    exigirReferencia(
                            referencias.checklist(
                                    texto(documento, CAMPO_CORRELATION_ID),
                                    checklist),
                            referencia);
                    return checklist;
                });
    }

    @Override
    public Uni<Void> aguardarRevisao(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        return Uni.createFrom().deferred(() -> {
            validarResultadoPreliminar(resultado);
            return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
                ObjectNode projecao = projecaoPersistida.documento();
                exigirStatus(projecao, StatusAnaliseConformidade.EM_PROCESSAMENTO);
                exigirReferencia(projecao, CAMPO_CHECKLIST_REF);
                validarIdentidadesResultado(projecao, resultado);
                var referencia = referencias.resultadoPreliminar(
                        texto(projecao, CAMPO_CORRELATION_ID), resultado);
                return gravarImutavel(
                                referencia.documentoRef(),
                                documentoFato(
                                                referencia.documentoRef(),
                                                TIPO_RESULTADO_PRELIMINAR,
                                                projecao)
                                        .put(CAMPO_HASH_CONTEUDO, referencia.hashConteudo())
                                        .set(CAMPO_RESULTADO, objectMapper.valueToTree(resultado)))
                        .chain(() -> atualizarProjecao(
                                projecaoPersistida,
                                atualizada -> atualizada
                                .put(
                                        CAMPO_STATUS,
                                        StatusAnaliseConformidade.AGUARDANDO_REVISAO.name())
                                .put(
                                        CAMPO_RESULTADO_PRELIMINAR_REF,
                                        referencia.documentoRef())));
            });
        });
    }

    @Override
    public Uni<Void> prepararResultadoPreliminar(
            String instanceId,
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return Uni.createFrom().deferred(() -> {
            validarResultadoPreliminar(resultado);
            return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
                ObjectNode projecao = projecaoPersistida.documento();
                exigirStatus(projecao, StatusAnaliseConformidade.EM_PROCESSAMENTO);
                exigirReferencia(projecao, CAMPO_CHECKLIST_REF);
                validarIdentidadesResultado(projecao, resultado);
                var esperada = referencias.resultadoPreliminar(
                        texto(projecao, CAMPO_CORRELATION_ID), resultado);
                exigirReferencia(esperada, referencia);
                return gravarResultadoReferenciado(
                        projecao,
                        TIPO_RESULTADO_PRELIMINAR,
                        resultado,
                        referencia);
            });
        });
    }

    @Override
    public Uni<ResultadoAnaliseConformidade> carregarResultadoPreliminar(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return carregarDocumentoReferenciado(
                        instanceId,
                        referencia,
                        CAMPO_RESULTADO_PRELIMINAR_REF,
                        TIPO_RESULTADO_PRELIMINAR)
                .map(documento -> {
                    ResultadoAnaliseConformidade resultado = converter(
                            documento,
                            CAMPO_RESULTADO,
                            ResultadoAnaliseConformidade.class);
                    exigirReferencia(
                            referencias.resultadoPreliminar(
                                    texto(documento, CAMPO_CORRELATION_ID),
                                    resultado),
                            referencia);
                    return resultado;
                });
    }

    @Override
    public Uni<Void> reservarRevisao(
            String instanceId,
            RevisaoHumanaConformidade revisao) {
        return Uni.createFrom().deferred(() -> {
            if (revisao == null) {
                throw FalhaAnaliseConformidade.revisaoInconsistente(
                        "A revisão humana é obrigatória");
            }
            return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
                ObjectNode projecao = projecaoPersistida.documento();
                exigirStatus(projecao, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
                var referencia = referencias.revisao(
                        texto(projecao, CAMPO_CORRELATION_ID), revisao);
                Uni<Void> reservarReferencia;
                if (referencia.documentoRef().equals(textoOpcional(
                        projecao,
                        CAMPO_REVISAO_REF))) {
                    reservarReferencia = Uni.createFrom().voidItem();
                } else {
                    reservarReferencia = atualizarProjecao(
                                    projecaoPersistida,
                                    atualizada -> atualizada.put(
                                            CAMPO_REVISAO_REF,
                                            referencia.documentoRef()))
                            .onFailure(FalhaAnaliseConformidade.class)
                            .recoverWithUni(falha -> recuperarReservaConcorrente(
                                    instanceId,
                                    referencia.documentoRef(),
                                    falha));
                }
                return reservarReferencia.chain(() -> gravarImutavel(
                        referencia.documentoRef(),
                        documentoFato(
                                        referencia.documentoRef(),
                                        TIPO_REVISAO,
                                        projecao)
                                .put(
                                        CAMPO_HASH_CONTEUDO,
                                        referencia.hashConteudo())
                                .set(
                                        CAMPO_REVISAO,
                                        objectMapper.valueToTree(revisao))));
            });
        });
    }

    @Override
    public Uni<RevisaoHumanaConformidade> carregarRevisao(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return carregarDocumentoReferenciado(
                        instanceId,
                        referencia,
                        CAMPO_REVISAO_REF,
                        TIPO_REVISAO)
                .map(documento -> {
                    RevisaoHumanaConformidade revisao = converter(
                            documento,
                            CAMPO_REVISAO,
                            RevisaoHumanaConformidade.class);
                    exigirReferencia(
                            referencias.revisao(
                                    texto(documento, CAMPO_CORRELATION_ID),
                                    revisao),
                            referencia);
                    return revisao;
                });
    }

    @Override
    public Uni<Void> concluir(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        return Uni.createFrom().deferred(() -> {
            validarResultadoFinal(resultado);
            return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
                ObjectNode projecao = projecaoPersistida.documento();
                exigirStatus(projecao, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
                exigirReferencia(projecao, CAMPO_REVISAO_REF);
                validarIdentidadesResultado(projecao, resultado);
                var referencia = referencias.resultadoFinal(
                        texto(projecao, CAMPO_CORRELATION_ID), resultado);
                return gravarImutavel(
                                referencia.documentoRef(),
                                documentoFato(
                                                referencia.documentoRef(),
                                                TIPO_RESULTADO_FINAL,
                                                projecao)
                                        .put(CAMPO_HASH_CONTEUDO, referencia.hashConteudo())
                                        .set(CAMPO_RESULTADO, objectMapper.valueToTree(resultado)))
                        .chain(() -> atualizarProjecao(
                                projecaoPersistida,
                                atualizada -> atualizada
                                .put(
                                        CAMPO_STATUS,
                                        StatusAnaliseConformidade.CONCLUIDA.name())
                                .put(CAMPO_RESULTADO_FINAL_REF, referencia.documentoRef())));
            });
        });
    }

    @Override
    public Uni<Void> prepararResultadoFinal(
            String instanceId,
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return Uni.createFrom().deferred(() -> {
            validarResultadoFinal(resultado);
            return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
                ObjectNode projecao = projecaoPersistida.documento();
                exigirStatus(projecao, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
                exigirReferencia(projecao, CAMPO_REVISAO_REF);
                validarIdentidadesResultado(projecao, resultado);
                var esperada = referencias.resultadoFinal(
                        texto(projecao, CAMPO_CORRELATION_ID), resultado);
                exigirReferencia(esperada, referencia);
                return gravarResultadoReferenciado(
                        projecao,
                        TIPO_RESULTADO_FINAL,
                        resultado,
                        referencia);
            });
        });
    }

    @Override
    public Uni<Void> registrarEmissao(EmissaoReferencialAnaliseConformidade emissao) {
        return Uni.createFrom().deferred(() -> {
            if (emissao == null) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            return projecaoObrigatoria(emissao.instanceId()).chain(projecaoPersistida -> {
                ObjectNode projecao = projecaoPersistida.documento();
                if (!texto(projecao, CAMPO_CORRELATION_ID).equals(emissao.correlationId())) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                return validarDocumentoReferenciado(projecao, emissao)
                        .chain(() -> gravarFatoEmissao(projecao, emissao))
                        .chain(() -> projetarEmissao(projecaoPersistida, emissao));
            });
        });
    }

    @Override
    public Uni<Void> falhar(String instanceId, String mensagem) {
        return Uni.createFrom().deferred(() -> {
            validarInstanceId(instanceId);
            if (mensagem == null || mensagem.isBlank()) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
                ObjectNode projecao = projecaoPersistida.documento();
                StatusAnaliseConformidade status = status(projecao);
                if (status != StatusAnaliseConformidade.EM_PROCESSAMENTO
                        && status != StatusAnaliseConformidade.AGUARDANDO_REVISAO) {
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                }
                String referencia = IdsDocumentoAnaliseConformidade.falha(
                        texto(projecao, CAMPO_CORRELATION_ID));
                return gravarImutavel(
                                referencia,
                                documentoFato(referencia, TIPO_FALHA, projecao)
                                        .put("mensagem", mensagem))
                        .chain(() -> atualizarProjecao(
                                projecaoPersistida,
                                atualizada -> atualizada
                                .put(CAMPO_STATUS, StatusAnaliseConformidade.FALHOU.name())
                                .put("falhaRef", referencia)));
            });
        });
    }

    @Override
    public Uni<Optional<VisaoAnaliseConformidade>> consultar(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return Uni.createFrom().item(Optional.empty());
        }
        return consultarDocumento(
                        IdsDocumentoAnaliseConformidade.projecao(instanceId),
                        null)
                .chain(documento -> documento
                        .map(valor -> mapearProjecao(instanceId, valor)
                                .map(Optional::of))
                        .orElseGet(() -> Uni.createFrom().item(Optional.empty())));
    }

    private Uni<VisaoAnaliseConformidade> mapearProjecao(
            String instanceId,
            JsonNode documento) {
        exigirTipoEVersao(documento, TIPO_PROJECAO);
        String instanceIdPersistido = texto(documento, CAMPO_INSTANCE_ID);
        if (!instanceId.equals(instanceIdPersistido)) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        return resultadoReferenciado(
                        documento,
                        CAMPO_RESULTADO_PRELIMINAR_REF,
                        TIPO_RESULTADO_PRELIMINAR)
                .chain(resultadoPreliminar -> resultadoReferenciado(
                                documento,
                                CAMPO_RESULTADO_FINAL_REF,
                                TIPO_RESULTADO_FINAL)
                        .chain(resultadoFinal -> mensagemReferenciada(documento)
                                .map(mensagem -> criarVisao(
                                        documento,
                                        instanceIdPersistido,
                                        resultadoPreliminar,
                                        resultadoFinal,
                                        mensagem))));
    }

    private VisaoAnaliseConformidade criarVisao(
            JsonNode documento,
            String instanceId,
            ResultadoAnaliseConformidade resultadoPreliminar,
            ResultadoAnaliseConformidade resultadoFinal,
            String mensagem) {
        try {
            return new VisaoAnaliseConformidade(
                    texto(documento, CAMPO_CORRELATION_ID),
                    instanceId,
                    texto(documento, CAMPO_IDENTIFICADOR_DOCUMENTO),
                    inteiroLongo(documento, CAMPO_IDENTIFICADOR_CHECKLIST),
                    inteiro(documento, CAMPO_VERSAO_CHECKLIST),
                    status(documento),
                    resultadoPreliminar,
                    resultadoFinal,
                    mensagem);
        } catch (FalhaAnaliseConformidade _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private Uni<ResultadoAnaliseConformidade> resultadoReferenciado(
            JsonNode projecao,
            String campo,
            String tipo) {
        String referencia = textoOpcional(projecao, campo);
        if (referencia == null) {
            return Uni.createFrom().nullItem();
        }
        return consultarDocumento(
                        referencia,
                        texto(projecao, CAMPO_CORRELATION_ID))
                .map(documentoOpcional -> {
            JsonNode documento = documentoOpcional.orElseThrow(
                    FalhaAnaliseConformidade::indisponibilidadeTecnica);
            exigirTipoEVersao(documento, tipo);
            try {
                return objectMapper.treeToValue(
                        documento.path(CAMPO_RESULTADO),
                        ResultadoAnaliseConformidade.class);
            } catch (JsonProcessingException | FalhaAnaliseConformidade _) {
                throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
            }
        });
    }

    private Uni<String> mensagemReferenciada(JsonNode projecao) {
        String referencia = textoOpcional(projecao, "falhaRef");
        if (referencia == null) {
            return Uni.createFrom().nullItem();
        }
        return consultarDocumento(
                        referencia,
                        texto(projecao, CAMPO_CORRELATION_ID))
                .map(documentoOpcional -> {
            JsonNode documento = documentoOpcional.orElseThrow(
                    FalhaAnaliseConformidade::indisponibilidadeTecnica);
            exigirTipoEVersao(documento, TIPO_FALHA);
            return texto(documento, "mensagem");
        });
    }

    private Uni<RepositorioDocumental.DocumentoPersistido> projecaoObrigatoria(
            String instanceId) {
        return Uni.createFrom().deferred(() -> {
            validarInstanceId(instanceId);
            return consultarDocumentoPersistido(
                            IdsDocumentoAnaliseConformidade.projecao(instanceId),
                            null)
                    .map(documentoPersistidoOpcional -> {
                        var documentoPersistido = documentoPersistidoOpcional.orElseThrow(
                                FalhaAnaliseConformidade::instanciaNaoEncontrada);
                        JsonNode documento = documentoPersistido.documento();
                        exigirTipoEVersao(documento, TIPO_PROJECAO);
                        if (!instanceId.equals(texto(documento, CAMPO_INSTANCE_ID))) {
                            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
                        }
                        return documentoPersistido;
                    });
        });
    }

    private Uni<JsonNode> carregarDocumentoReferenciado(
            String instanceId,
            ReferenciaDocumentoAnaliseConformidade referencia,
            String campoReferencia,
            String tipo) {
        if (referencia == null) {
            return Uni.createFrom().failure(
                    FalhaAnaliseConformidade.transicaoInvalida());
        }
        return projecaoObrigatoria(instanceId).chain(projecaoPersistida -> {
            JsonNode projecao = projecaoPersistida.documento();
            if (!referencia.documentoRef().equals(
                    textoOpcional(projecao, campoReferencia))) {
                throw FalhaAnaliseConformidade.transicaoInvalida();
            }
            String correlationId = texto(projecao, CAMPO_CORRELATION_ID);
            return consultarDocumento(referencia.documentoRef(), correlationId)
                    .map(documentoOpcional -> {
                        JsonNode documento = documentoOpcional.orElseThrow(
                                FalhaAnaliseConformidade::indisponibilidadeTecnica);
                        exigirTipoEVersao(documento, tipo);
                        if (!instanceId.equals(texto(documento, CAMPO_INSTANCE_ID))
                                || !correlationId.equals(texto(
                                        documento, CAMPO_CORRELATION_ID))
                                || !referencia.hashConteudo().equals(texto(
                                        documento, CAMPO_HASH_CONTEUDO))) {
                            throw FalhaAnaliseConformidade.transicaoInvalida();
                        }
                        return documento;
                    });
        });
    }

    private <T> T converter(JsonNode documento, String campo, Class<T> tipo) {
        JsonNode conteudo = documento.path(campo);
        if (!conteudo.isObject()) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        try {
            return objectMapper.treeToValue(conteudo, tipo);
        } catch (JsonProcessingException | FalhaAnaliseConformidade _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private Uni<Void> atualizarProjecao(
            RepositorioDocumental.DocumentoPersistido atual,
            Consumer<ObjectNode> alteracao) {
        ObjectNode atualizada = atual.documento();
        alteracao.accept(atualizada);
        return repositorio.substituir(
                        texto(atualizada, "id"),
                        atual.versao(),
                        atualizada)
                .onFailure()
                .transform(DocumentoAnaliseConformidadeStore::sanitizarFalha)
                .map(resultado -> {
                    if (resultado == RepositorioDocumental.ResultadoGravacao.GRAVADO) {
                        return null;
                    }
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                });
    }

    private Uni<Void> gravarResultadoReferenciado(
            JsonNode projecao,
            String tipo,
            ResultadoAnaliseConformidade resultado,
            ReferenciaDocumentoAnaliseConformidade referencia) {
        return gravarImutavel(
                referencia.documentoRef(),
                documentoFato(referencia.documentoRef(), tipo, projecao)
                        .put(CAMPO_HASH_CONTEUDO, referencia.hashConteudo())
                        .set(CAMPO_RESULTADO, objectMapper.valueToTree(resultado)));
    }

    private Uni<Void> validarDocumentoReferenciado(
            JsonNode projecao,
            EmissaoReferencialAnaliseConformidade emissao) {
        String correlationId = texto(projecao, CAMPO_CORRELATION_ID);
        String tipoDocumento;
        String referenciaEsperada;
        switch (emissao.tipo()) {
            case REVISAO_SOLICITADA -> {
                tipoDocumento = TIPO_RESULTADO_PRELIMINAR;
                referenciaEsperada = IdsDocumentoAnaliseConformidade
                        .resultadoPreliminar(correlationId);
            }
            case ANALISE_CONCLUIDA -> {
                tipoDocumento = TIPO_RESULTADO_FINAL;
                referenciaEsperada = IdsDocumentoAnaliseConformidade
                        .resultadoFinal(correlationId);
            }
            default -> throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        var referencia = emissao.documento();
        if (!referenciaEsperada.equals(referencia.documentoRef())) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        return consultarDocumento(referencia.documentoRef(), correlationId)
                .map(documentoOpcional -> {
                    JsonNode documento = documentoOpcional.orElseThrow(
                            FalhaAnaliseConformidade::transicaoInvalida);
                    exigirTipoEVersao(documento, tipoDocumento);
                    String hashPersistido = texto(documento, CAMPO_HASH_CONTEUDO);
                    String hashRecalculado = hashCanonico(documento.path(CAMPO_RESULTADO));
                    if (!hashPersistido.equals(hashRecalculado)
                            || !referencia.hashConteudo().equals(hashRecalculado)) {
                        throw FalhaAnaliseConformidade.transicaoInvalida();
                    }
                    return null;
                });
    }

    private Uni<Void> gravarFatoEmissao(
            JsonNode projecao,
            EmissaoReferencialAnaliseConformidade emissao) {
        String documentoId = IdsDocumentoAnaliseConformidade.emissao(emissao.id());
        return gravarImutavel(
                documentoId,
                documentoFato(documentoId, TIPO_EMISSAO, projecao)
                        .put("eventoId", emissao.id())
                        .put("eventoTipo", emissao.tipo().cloudEventType())
                        .put("documentoRef", emissao.documento().documentoRef())
                        .put(CAMPO_HASH_CONTEUDO, emissao.documento().hashConteudo()));
    }

    private Uni<Void> projetarEmissao(
            RepositorioDocumental.DocumentoPersistido projecaoPersistida,
            EmissaoReferencialAnaliseConformidade emissao) {
        ObjectNode projecao = projecaoPersistida.documento();
        return switch (emissao.tipo()) {
            case REVISAO_SOLICITADA -> projetarSolicitacaoRevisao(
                    emissao.instanceId(),
                    projecaoPersistida,
                    projecao,
                    emissao.documento().documentoRef());
            case ANALISE_CONCLUIDA -> projetarAnaliseConcluida(
                    emissao.instanceId(),
                    projecaoPersistida,
                    projecao,
                    emissao.documento().documentoRef());
        };
    }

    private Uni<Void> projetarSolicitacaoRevisao(
            String instanceId,
            RepositorioDocumental.DocumentoPersistido projecaoPersistida,
            JsonNode projecao,
            String referencia) {
        StatusAnaliseConformidade status = status(projecao);
        if (status == StatusAnaliseConformidade.AGUARDANDO_REVISAO
                && referencia.equals(textoOpcional(
                        projecao,
                        CAMPO_RESULTADO_PRELIMINAR_REF))) {
            return Uni.createFrom().voidItem();
        }
        if (status != StatusAnaliseConformidade.EM_PROCESSAMENTO) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        return atualizarProjecao(projecaoPersistida, atualizada -> atualizada
                .put(CAMPO_STATUS, StatusAnaliseConformidade.AGUARDANDO_REVISAO.name())
                .put(CAMPO_RESULTADO_PRELIMINAR_REF, referencia))
                .onFailure(FalhaAnaliseConformidade.class)
                .recoverWithUni(falha -> recuperarEmissaoConcorrente(
                        instanceId,
                        StatusAnaliseConformidade.AGUARDANDO_REVISAO,
                        CAMPO_RESULTADO_PRELIMINAR_REF,
                        referencia,
                        falha));
    }

    private Uni<Void> projetarAnaliseConcluida(
            String instanceId,
            RepositorioDocumental.DocumentoPersistido projecaoPersistida,
            JsonNode projecao,
            String referencia) {
        StatusAnaliseConformidade status = status(projecao);
        if (status == StatusAnaliseConformidade.CONCLUIDA
                && referencia.equals(textoOpcional(projecao, CAMPO_RESULTADO_FINAL_REF))) {
            return Uni.createFrom().voidItem();
        }
        if (status != StatusAnaliseConformidade.AGUARDANDO_REVISAO) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        exigirReferencia(projecao, CAMPO_REVISAO_REF);
        return atualizarProjecao(projecaoPersistida, atualizada -> atualizada
                .put(CAMPO_STATUS, StatusAnaliseConformidade.CONCLUIDA.name())
                .put(CAMPO_RESULTADO_FINAL_REF, referencia))
                .onFailure(FalhaAnaliseConformidade.class)
                .recoverWithUni(falha -> recuperarEmissaoConcorrente(
                        instanceId,
                        StatusAnaliseConformidade.CONCLUIDA,
                        CAMPO_RESULTADO_FINAL_REF,
                        referencia,
                        falha));
    }

    private Uni<Void> recuperarEmissaoConcorrente(
            String instanceId,
            StatusAnaliseConformidade statusEsperado,
            String campoReferencia,
            String referencia,
            FalhaAnaliseConformidade falha) {
        if (falha.tipo() != FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA) {
            return Uni.createFrom().failure(falha);
        }
        return projecaoObrigatoria(instanceId).chain(atual -> {
            JsonNode projecao = atual.documento();
            if (status(projecao) == statusEsperado
                    && referencia.equals(textoOpcional(projecao, campoReferencia))) {
                return Uni.createFrom().voidItem();
            }
            return Uni.createFrom().failure(falha);
        });
    }

    private Uni<Void> recuperarReservaConcorrente(
            String instanceId,
            String referencia,
            FalhaAnaliseConformidade falha) {
        if (falha.tipo() != FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA) {
            return Uni.createFrom().failure(falha);
        }
        return projecaoObrigatoria(instanceId).chain(atual -> {
            if (referencia.equals(textoOpcional(
                    atual.documento(),
                    CAMPO_REVISAO_REF))) {
                return Uni.createFrom().voidItem();
            }
            return Uni.createFrom().failure(falha);
        });
    }

    private ObjectNode documentoEntrada(
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        String documentId = IdsDocumentoAnaliseConformidade.entrada(
                solicitacao.correlationId());
        return identidades(objectMapper.createObjectNode(), instanceId, solicitacao)
                .put("id", documentId)
                .put("tipo", TIPO_ENTRADA)
                .put(CAMPO_VERSAO_SCHEMA, VERSAO_SCHEMA)
                .put("texto", solicitacao.texto());
    }

    private ObjectNode documentoProjecao(
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        String documentId = IdsDocumentoAnaliseConformidade.projecao(instanceId);
        return identidades(objectMapper.createObjectNode(), instanceId, solicitacao)
                .put("id", documentId)
                .put("tipo", TIPO_PROJECAO)
                .put(CAMPO_VERSAO_SCHEMA, VERSAO_SCHEMA)
                .put(CAMPO_STATUS, StatusAnaliseConformidade.EM_PROCESSAMENTO.name());
    }

    private ObjectNode documentoFato(
            String documentId,
            String tipo,
            JsonNode projecao) {
        return copiarIdentidades(objectMapper.createObjectNode(), projecao)
                .put("id", documentId)
                .put("tipo", tipo)
                .put(CAMPO_VERSAO_SCHEMA, VERSAO_SCHEMA);
    }

    private static ObjectNode identidades(
            ObjectNode documento,
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        return documento
                .put(CAMPO_CORRELATION_ID, solicitacao.correlationId())
                .put(CAMPO_INSTANCE_ID, instanceId)
                .put(CAMPO_IDENTIFICADOR_DOCUMENTO, solicitacao.identificadorDocumento())
                .put(CAMPO_IDENTIFICADOR_CHECKLIST, solicitacao.identificadorChecklist())
                .put(CAMPO_VERSAO_CHECKLIST, solicitacao.versaoChecklist());
    }

    private static ObjectNode copiarIdentidades(
            ObjectNode destino,
            JsonNode origem) {
        return destino
                .put(CAMPO_CORRELATION_ID, texto(origem, CAMPO_CORRELATION_ID))
                .put(CAMPO_INSTANCE_ID, texto(origem, CAMPO_INSTANCE_ID))
                .put(
                        CAMPO_IDENTIFICADOR_DOCUMENTO,
                        texto(origem, CAMPO_IDENTIFICADOR_DOCUMENTO))
                .put(
                        CAMPO_IDENTIFICADOR_CHECKLIST,
                        inteiroLongo(origem, CAMPO_IDENTIFICADOR_CHECKLIST))
                .put(CAMPO_VERSAO_CHECKLIST, inteiro(origem, CAMPO_VERSAO_CHECKLIST));
    }

    private Uni<Void> gravarNovo(String documentId, ObjectNode documento) {
        return repositorio.criar(documentId, documento)
                .onFailure()
                .transform(DocumentoAnaliseConformidadeStore::sanitizarFalha)
                .map(resultado -> {
                    if (resultado == RepositorioDocumental.ResultadoGravacao.GRAVADO) {
                        return null;
                    }
                    throw FalhaAnaliseConformidade.transicaoInvalida();
                });
    }

    private Uni<Void> gravarImutavel(String documentId, ObjectNode documento) {
        return repositorio.criar(documentId, documento)
                .onFailure()
                .transform(DocumentoAnaliseConformidadeStore::sanitizarFalha)
                .chain(resultado -> {
                    if (resultado == RepositorioDocumental.ResultadoGravacao.GRAVADO) {
                        return Uni.createFrom().voidItem();
                    }
                    return consultarDocumento(
                                    documentId,
                                    texto(documento, CAMPO_CORRELATION_ID))
                            .map(existenteOpcional -> {
                                JsonNode existente = existenteOpcional.orElseThrow(
                                        FalhaAnaliseConformidade::indisponibilidadeTecnica);
                                if (!normalizar(existente).equals(normalizar(documento))) {
                                    throw FalhaAnaliseConformidade.transicaoInvalida();
                                }
                                return null;
                            });
                });
    }

    private Uni<Optional<ObjectNode>> consultarDocumento(
            String documentId,
            String correlationId) {
        return consultarDocumentoPersistido(documentId, correlationId)
                .map(documento -> documento.map(
                        RepositorioDocumental.DocumentoPersistido::documento));
    }

    private Uni<Optional<RepositorioDocumental.DocumentoPersistido>>
            consultarDocumentoPersistido(
                    String documentId,
                    String correlationId) {
        return repositorio.consultar(documentId, correlationId)
                .onFailure()
                .transform(DocumentoAnaliseConformidadeStore::sanitizarFalha);
    }

    private static Throwable sanitizarFalha(Throwable falha) {
        if (falha instanceof FalhaAnaliseConformidade) {
            return falha;
        }
        return FalhaAnaliseConformidade.indisponibilidadeTecnica();
    }

    private JsonNode normalizar(JsonNode documento) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsBytes(documento));
        } catch (IOException _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private String hashCanonico(JsonNode conteudo) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(conteudo);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (IOException | NoSuchAlgorithmException _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private static void exigirTipoEVersao(JsonNode documento, String tipo) {
        if (documento == null
                || !documento.isObject()
                || !tipo.equals(texto(documento, "tipo"))
                || !documento.path(CAMPO_VERSAO_SCHEMA).isIntegralNumber()
                || !documento.path(CAMPO_VERSAO_SCHEMA).canConvertToInt()
                || documento.path(CAMPO_VERSAO_SCHEMA).intValue()
                        != VERSAO_SCHEMA.intValue()) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private static void exigirStatus(
            JsonNode projecao,
            StatusAnaliseConformidade esperado) {
        if (status(projecao) != esperado) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }

    private static StatusAnaliseConformidade status(JsonNode documento) {
        try {
            return StatusAnaliseConformidade.valueOf(texto(documento, CAMPO_STATUS));
        } catch (IllegalArgumentException _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private static void validarChecklist(JsonNode projecao, Checklist checklist) {
        if (checklist == null
                || checklist.apontamentos() == null
                || checklist.apontamentos().isEmpty()
                || !inteiroLongo(projecao, CAMPO_IDENTIFICADOR_CHECKLIST)
                        .equals(checklist.identificadorNegocial())
                || !inteiro(projecao, CAMPO_VERSAO_CHECKLIST).equals(checklist.versao())) {
            throw FalhaAnaliseConformidade.checklistInvalido(
                    "O checklist não corresponde à análise");
        }
    }

    private static void validarIdentidadesResultado(
            JsonNode projecao,
            ResultadoAnaliseConformidade resultado) {
        if (!inteiroLongo(projecao, CAMPO_IDENTIFICADOR_CHECKLIST)
                        .equals(resultado.identificadorChecklist())
                || !inteiro(projecao, CAMPO_VERSAO_CHECKLIST)
                        .equals(resultado.versaoChecklist())) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O resultado não corresponde à análise");
        }
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

    private static void exigirReferencia(JsonNode documento, String campo) {
        texto(documento, campo);
    }

    private static void exigirReferencia(
            ReferenciaDocumentoAnaliseConformidade esperada,
            ReferenciaDocumentoAnaliseConformidade recebida) {
        if (!esperada.equals(recebida)) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }

    private static String texto(JsonNode documento, String campo) {
        var valor = documento.path(campo);
        if (!valor.isTextual() || valor.textValue().isBlank()) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        return valor.textValue();
    }

    private static String textoOpcional(JsonNode documento, String campo) {
        var valor = documento.path(campo);
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        return texto(documento, campo);
    }

    private static Long inteiroLongo(JsonNode documento, String campo) {
        var valor = documento.path(campo);
        if (!valor.canConvertToLong() || valor.longValue() <= 0) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        return valor.longValue();
    }

    private static Integer inteiro(JsonNode documento, String campo) {
        var valor = documento.path(campo);
        if (!valor.canConvertToInt() || valor.intValue() <= 0) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        return valor.intValue();
    }

    private static void validarInstanceId(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }
}
