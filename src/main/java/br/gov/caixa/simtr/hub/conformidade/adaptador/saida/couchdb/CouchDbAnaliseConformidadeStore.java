package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

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
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Consumer;

public class CouchDbAnaliseConformidadeStore
        implements ArmazenarEstadoAnaliseConformidade {

    static final Short VERSAO_SCHEMA = 1;
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

    private final CouchDbClient client;
    private final ObjectMapper objectMapper;
    private final String database;

    CouchDbAnaliseConformidadeStore(
            CouchDbClient client,
            ObjectMapper objectMapper,
            String database) {
        this.client = java.util.Objects.requireNonNull(client, "client");
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
        this.database = validarDatabase(database);
    }

    @Override
    public void iniciar(
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        validarInstanceId(instanceId);
        if (solicitacao == null) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "A solicitação da análise é obrigatória");
        }
        gravarImutavel(
                CouchDbIds.entrada(solicitacao.correlationId()),
                documentoEntrada(instanceId, solicitacao));
        gravarNovo(
                CouchDbIds.projecao(instanceId),
                documentoProjecao(instanceId, solicitacao));
    }

    @Override
    public void registrarChecklist(String instanceId, Checklist checklist) {
        ObjectNode projecao = projecaoObrigatoria(instanceId);
        exigirStatus(projecao, StatusAnaliseConformidade.EM_PROCESSAMENTO);
        validarChecklist(projecao, checklist);
        String referencia = CouchDbIds.checklist(texto(projecao, CAMPO_CORRELATION_ID));
        JsonNode snapshot = objectMapper.valueToTree(checklist);
        String hash = hashCanonico(snapshot);
        gravarImutavel(
                referencia,
                documentoFato(referencia, TIPO_CHECKLIST, projecao)
                        .put(CAMPO_HASH_CONTEUDO, hash)
                        .set(CAMPO_CHECKLIST, snapshot));
        if (referencia.equals(textoOpcional(projecao, CAMPO_CHECKLIST_REF))) {
            return;
        }
        atualizarProjecao(projecao, atualizada -> atualizada
                .put(CAMPO_CHECKLIST_REF, referencia)
                .put(CAMPO_CHECKLIST_HASH, hash));
    }

    @Override
    public void aguardarRevisao(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        validarResultadoPreliminar(resultado);
        ObjectNode projecao = projecaoObrigatoria(instanceId);
        exigirStatus(projecao, StatusAnaliseConformidade.EM_PROCESSAMENTO);
        exigirReferencia(projecao, CAMPO_CHECKLIST_REF);
        validarIdentidadesResultado(projecao, resultado);
        String referencia = CouchDbIds.resultadoPreliminar(
                texto(projecao, CAMPO_CORRELATION_ID));
        gravarImutavel(
                referencia,
                documentoFato(referencia, TIPO_RESULTADO_PRELIMINAR, projecao)
                        .set(CAMPO_RESULTADO, objectMapper.valueToTree(resultado)));
        atualizarProjecao(projecao, atualizada -> atualizada
                .put(CAMPO_STATUS, StatusAnaliseConformidade.AGUARDANDO_REVISAO.name())
                .put("resultadoPreliminarRef", referencia));
    }

    @Override
    public void reservarRevisao(
            String instanceId,
            RevisaoHumanaConformidade revisao) {
        if (revisao == null) {
            throw FalhaAnaliseConformidade.revisaoInconsistente(
                    "A revisão humana é obrigatória");
        }
        ObjectNode projecao = projecaoObrigatoria(instanceId);
        exigirStatus(projecao, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        String referencia = CouchDbIds.revisao(texto(projecao, CAMPO_CORRELATION_ID));
        gravarImutavel(
                referencia,
                documentoFato(referencia, TIPO_REVISAO, projecao)
                        .set("revisao", objectMapper.valueToTree(revisao)));
        if (referencia.equals(textoOpcional(projecao, CAMPO_REVISAO_REF))) {
            return;
        }
        try {
            atualizarProjecao(
                    projecao,
                    atualizada -> atualizada.put(CAMPO_REVISAO_REF, referencia));
        } catch (FalhaAnaliseConformidade falha) {
            if (falha.tipo() != FalhaAnaliseConformidade.Tipo.TRANSICAO_INVALIDA
                    || !referencia.equals(textoOpcional(
                            projecaoObrigatoria(instanceId),
                            CAMPO_REVISAO_REF))) {
                throw falha;
            }
        }
    }

    @Override
    public void concluir(
            String instanceId,
            ResultadoAnaliseConformidade resultado) {
        validarResultadoFinal(resultado);
        ObjectNode projecao = projecaoObrigatoria(instanceId);
        exigirStatus(projecao, StatusAnaliseConformidade.AGUARDANDO_REVISAO);
        exigirReferencia(projecao, CAMPO_REVISAO_REF);
        validarIdentidadesResultado(projecao, resultado);
        String referencia = CouchDbIds.resultadoFinal(texto(projecao, CAMPO_CORRELATION_ID));
        gravarImutavel(
                referencia,
                documentoFato(referencia, TIPO_RESULTADO_FINAL, projecao)
                        .set(CAMPO_RESULTADO, objectMapper.valueToTree(resultado)));
        atualizarProjecao(projecao, atualizada -> atualizada
                .put(CAMPO_STATUS, StatusAnaliseConformidade.CONCLUIDA.name())
                .put("resultadoFinalRef", referencia));
    }

    @Override
    public void falhar(String instanceId, String mensagem) {
        validarInstanceId(instanceId);
        if (mensagem == null || mensagem.isBlank()) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        ObjectNode projecao = projecaoObrigatoria(instanceId);
        StatusAnaliseConformidade status = status(projecao);
        if (status != StatusAnaliseConformidade.EM_PROCESSAMENTO
                && status != StatusAnaliseConformidade.AGUARDANDO_REVISAO) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        String referencia = CouchDbIds.falha(texto(projecao, CAMPO_CORRELATION_ID));
        gravarImutavel(
                referencia,
                documentoFato(referencia, TIPO_FALHA, projecao)
                        .put("mensagem", mensagem));
        atualizarProjecao(projecao, atualizada -> atualizada
                .put(CAMPO_STATUS, StatusAnaliseConformidade.FALHOU.name())
                .put("falhaRef", referencia));
    }

    @Override
    public Optional<VisaoAnaliseConformidade> consultar(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return Optional.empty();
        }
        return consultarDocumento(CouchDbIds.projecao(instanceId))
                .map(documento -> mapearProjecao(instanceId, documento));
    }

    private VisaoAnaliseConformidade mapearProjecao(
            String instanceId,
            JsonNode documento) {
        exigirTipoEVersao(documento, TIPO_PROJECAO);
        String instanceIdPersistido = texto(documento, CAMPO_INSTANCE_ID);
        if (!instanceId.equals(instanceIdPersistido)) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        var resultadoPreliminar = resultadoReferenciado(
                documento,
                "resultadoPreliminarRef",
                TIPO_RESULTADO_PRELIMINAR);
        var resultadoFinal = resultadoReferenciado(
                documento,
                "resultadoFinalRef",
                TIPO_RESULTADO_FINAL);
        String mensagem = mensagemReferenciada(documento);
        try {
            return new VisaoAnaliseConformidade(
                    texto(documento, CAMPO_CORRELATION_ID),
                    instanceIdPersistido,
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

    private ResultadoAnaliseConformidade resultadoReferenciado(
            JsonNode projecao,
            String campo,
            String tipo) {
        String referencia = textoOpcional(projecao, campo);
        if (referencia == null) {
            return null;
        }
        JsonNode documento = consultarDocumento(referencia)
                .orElseThrow(FalhaAnaliseConformidade::indisponibilidadeTecnica);
        exigirTipoEVersao(documento, tipo);
        try {
            return objectMapper.treeToValue(
                    documento.path(CAMPO_RESULTADO),
                    ResultadoAnaliseConformidade.class);
        } catch (JsonProcessingException | FalhaAnaliseConformidade _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private String mensagemReferenciada(JsonNode projecao) {
        String referencia = textoOpcional(projecao, "falhaRef");
        if (referencia == null) {
            return null;
        }
        JsonNode documento = consultarDocumento(referencia)
                .orElseThrow(FalhaAnaliseConformidade::indisponibilidadeTecnica);
        exigirTipoEVersao(documento, TIPO_FALHA);
        return texto(documento, "mensagem");
    }

    private ObjectNode projecaoObrigatoria(String instanceId) {
        validarInstanceId(instanceId);
        JsonNode documento = consultarDocumento(CouchDbIds.projecao(instanceId))
                .orElseThrow(FalhaAnaliseConformidade::instanciaNaoEncontrada);
        exigirTipoEVersao(documento, TIPO_PROJECAO);
        if (!(documento instanceof ObjectNode objectNode)
                || !instanceId.equals(texto(documento, CAMPO_INSTANCE_ID))) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        return objectNode;
    }

    private void atualizarProjecao(
            ObjectNode atual,
            Consumer<ObjectNode> alteracao) {
        ObjectNode atualizada = atual.deepCopy();
        alteracao.accept(atualizada);
        var response = client.gravar(
                database,
                texto(atualizada, "_id"),
                atualizada);
        if (response.status() == 201) {
            return;
        }
        if (response.status() == 409) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
    }

    private ObjectNode documentoEntrada(
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        String documentId = CouchDbIds.entrada(solicitacao.correlationId());
        return identidades(objectMapper.createObjectNode(), instanceId, solicitacao)
                .put("id", documentId)
                .put("tipo", TIPO_ENTRADA)
                .put(CAMPO_VERSAO_SCHEMA, VERSAO_SCHEMA)
                .put("texto", solicitacao.texto());
    }

    private ObjectNode documentoProjecao(
            String instanceId,
            SolicitacaoAnaliseConformidade solicitacao) {
        String documentId = CouchDbIds.projecao(instanceId);
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

    private void gravarNovo(String documentId, JsonNode documento) {
        var response = chamarGravacao(documentId, documento);
        if (response.status() == 201) {
            return;
        }
        if (response.status() == 409) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
        throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
    }

    private void gravarImutavel(String documentId, ObjectNode documento) {
        var response = chamarGravacao(documentId, documento);
        if (response.status() == 201) {
            return;
        }
        if (response.status() != 409) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
        JsonNode existente = consultarDocumento(documentId)
                .orElseThrow(FalhaAnaliseConformidade::indisponibilidadeTecnica);
        if (!normalizar(existente).equals(normalizar(documento))) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }

    private CouchDbClient.Resposta chamarGravacao(
            String documentId,
            JsonNode documento) {
        try {
            return client.gravar(database, documentId, documento);
        } catch (RuntimeException _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private Optional<JsonNode> consultarDocumento(String documentId) {
        try {
            var response = client.consultar(database, documentId);
            if (response.status() == 404) {
                return Optional.empty();
            }
            if (response.status() != 200 || response.body() == null) {
                throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
            }
            return Optional.of(response.body());
        } catch (FalhaAnaliseConformidade falha) {
            throw falha;
        } catch (RuntimeException _) {
            throw FalhaAnaliseConformidade.indisponibilidadeTecnica();
        }
    }

    private JsonNode normalizar(JsonNode documento) {
        ObjectNode copia = ((ObjectNode) documento).deepCopy();
        copia.remove("_id");
        copia.remove("_rev");
        try {
            return objectMapper.readTree(objectMapper.writeValueAsBytes(copia));
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

    private static String validarDatabase(String database) {
        if (database == null || !database.matches("[a-z][a-z0-9_$()+-]{0,237}")) {
            throw new IllegalArgumentException("Nome de database CouchDB inválido");
        }
        return database;
    }

    private static void validarInstanceId(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            throw FalhaAnaliseConformidade.transicaoInvalida();
        }
    }
}
