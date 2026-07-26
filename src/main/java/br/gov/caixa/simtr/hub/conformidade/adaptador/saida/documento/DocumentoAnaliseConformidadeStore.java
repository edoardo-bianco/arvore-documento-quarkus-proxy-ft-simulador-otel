package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
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
    private static final String CAMPO_REVISAO_REF = "revisaoRef";

    private final RepositorioDocumental repositorio;
    private final ObjectMapper objectMapper;

    protected DocumentoAnaliseConformidadeStore(
            RepositorioDocumental repositorio,
            ObjectMapper objectMapper) {
        this.repositorio = java.util.Objects.requireNonNull(
                repositorio,
                "repositorio");
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
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
                String referencia = IdsDocumentoAnaliseConformidade.resultadoPreliminar(
                        texto(projecao, CAMPO_CORRELATION_ID));
                return gravarImutavel(
                                referencia,
                                documentoFato(referencia, TIPO_RESULTADO_PRELIMINAR, projecao)
                                        .set(CAMPO_RESULTADO, objectMapper.valueToTree(resultado)))
                        .chain(() -> atualizarProjecao(
                                projecaoPersistida,
                                atualizada -> atualizada
                                .put(
                                        CAMPO_STATUS,
                                        StatusAnaliseConformidade.AGUARDANDO_REVISAO.name())
                                .put("resultadoPreliminarRef", referencia)));
            });
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
                String referencia = IdsDocumentoAnaliseConformidade.revisao(
                        texto(projecao, CAMPO_CORRELATION_ID));
                return gravarImutavel(
                                referencia,
                                documentoFato(referencia, TIPO_REVISAO, projecao)
                                        .set("revisao", objectMapper.valueToTree(revisao)))
                        .chain(() -> {
                            if (referencia.equals(textoOpcional(
                                    projecao,
                                    CAMPO_REVISAO_REF))) {
                                return Uni.createFrom().voidItem();
                            }
                            return atualizarProjecao(
                                            projecaoPersistida,
                                            atualizada -> atualizada.put(
                                                    CAMPO_REVISAO_REF,
                                                    referencia))
                                    .onFailure(FalhaAnaliseConformidade.class)
                                    .recoverWithUni(falha -> recuperarReservaConcorrente(
                                            instanceId,
                                            referencia,
                                            falha));
                        });
            });
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
                String referencia = IdsDocumentoAnaliseConformidade.resultadoFinal(
                        texto(projecao, CAMPO_CORRELATION_ID));
                return gravarImutavel(
                                referencia,
                                documentoFato(referencia, TIPO_RESULTADO_FINAL, projecao)
                                        .set(CAMPO_RESULTADO, objectMapper.valueToTree(resultado)))
                        .chain(() -> atualizarProjecao(
                                projecaoPersistida,
                                atualizada -> atualizada
                                .put(
                                        CAMPO_STATUS,
                                        StatusAnaliseConformidade.CONCLUIDA.name())
                                .put("resultadoFinalRef", referencia)));
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
                        "resultadoPreliminarRef",
                        TIPO_RESULTADO_PRELIMINAR)
                .chain(resultadoPreliminar -> resultadoReferenciado(
                                documento,
                                "resultadoFinalRef",
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
                || !documento.path(CAMPO_VERSAO_SCHEMA).canConvertToInt()
                || documento.path(CAMPO_VERSAO_SCHEMA).shortValue() != VERSAO_SCHEMA) {
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
