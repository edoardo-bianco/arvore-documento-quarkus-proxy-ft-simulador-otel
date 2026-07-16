package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.DocumentoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.DocumentoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento.DocumentoDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.DocumentoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.DocumentoDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@DocumentoMtr
public class DocumentoDossieProdutoMtrAdapter
        implements SolicitarInclusaoDocumentoDossieProduto {

    private static final Logger LOG = Logger.getLogger(DocumentoDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String INCLUIR = "incluir-documento-dossie-produto-v2";

    private final DocumentoDossieProdutoMtrClient client;
    private final DocumentoDossieProdutoMtrMapper mapper;

    @Inject
    public DocumentoDossieProdutoMtrAdapter(
            @RestClient DocumentoDossieProdutoMtrClient client,
            DocumentoDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.documento.incluir", kind = SpanKind.CLIENT)
    public Uni<ResultadoInclusaoDocumentoDossieProduto> incluir(
            ComandoInclusaoDocumentoDossieProduto comando
    ) {
        Long identificador = comando != null ? comando.identificadorDossieProduto() : null;
        String tipoDocumento = comando != null ? comando.tipoDocumento() : null;
        Integer quantidadeAtributos = comando != null && comando.atributos() != null
                ? comando.atributos().size() : null;
        Integer quantidadePropriedades = comando != null && comando.propriedades() != null
                ? comando.propriedades().size() : null;

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v2");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("url.path", "/simtr/dossie-produto/v2/dossie-produto/"
                + identificador + "/documento");
        setLongAttribute(span, "dossie_produto.id", identificador);
        setStringAttribute(span, "dossie_produto.documento.tipo", tipoDocumento);
        setIntAttribute(span, "dossie_produto.documento.atributos.quantidade",
                quantidadeAtributos);
        setIntAttribute(span, "dossie_produto.documento.propriedades.quantidade",
                quantidadePropriedades);

        ObservabilityLog.info(LOG, "mtr.dossie-produto.documento.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE, OPERACAO, INCLUIR,
                        "dossie_produto_id", identificador,
                        "tipo_documento", tipoDocumento,
                        "documento_atributos_quantidade", quantidadeAtributos,
                        "documento_propriedades_quantidade", quantidadePropriedades));

        return client.incluir(identificador, mapper.paraMtr(comando))
                .invoke(resposta -> {
                    registrarConclusao(span, identificador, resposta);
                })
                .onFailure().invoke(erro -> {
                    registrarFalha(span, identificador, tipoDocumento, erro);
                })
                .map(mapper::paraResultado)
                .onFailure().transform(DocumentoDossieProdutoMtrAdapter::traduzir);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof DocumentoDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaInclusaoDocumentoDossieProduto.Tipo tipo = falha instanceof TimeoutException
                ? FalhaInclusaoDocumentoDossieProduto.Tipo.TIMEOUT
                : FalhaInclusaoDocumentoDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaInclusaoDocumentoDossieProduto(
                tipo, null, "simtr-dossie-produto", null, null,
                null, null, null, falha);
    }

    private static void registrarConclusao(Span span, Long identificador,
            DocumentoDossieProdutoMtrResponse resposta) {
        span.setAttribute("mtr.resposta.sucesso", true);
        if (resposta != null && resposta.idDocumento() != null) {
            span.setAttribute("dossie_produto.documento.id", resposta.idDocumento());
        }
        if (resposta != null && resposta.idInstanciaDocumento() != null) {
            span.setAttribute("dossie_produto.documento.instancia.id", resposta.idInstanciaDocumento());
        }
        ObservabilityLog.info(LOG, "mtr.dossie-produto.documento.chamada.concluida",
                ObservabilityLog.fields(CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE, OPERACAO, INCLUIR,
                        "dossie_produto_id", identificador, "id_documento",
                        resposta != null ? resposta.idDocumento() : null, "id_instancia_documento",
                        resposta != null ? resposta.idInstanciaDocumento() : null, "resultado", "sucesso"));
    }

    private static void registrarFalha(Span span, Long identificador, String tipoDocumento, Throwable erro) {
        span.recordException(erro);
        span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
        span.setAttribute("mtr.resposta.sucesso", false);
        span.setAttribute("erro.tipo", erro.getClass().getName());
        ObservabilityLog.error(LOG, "mtr.dossie-produto.documento.chamada.falhou", erro,
                ObservabilityLog.fields(CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE, OPERACAO, INCLUIR,
                        "dossie_produto_id", identificador, "tipo_documento", tipoDocumento,
                        "erro_tipo", erro.getClass().getSimpleName(), "resultado", "erro"));
    }

    private static FalhaInclusaoDocumentoDossieProduto traduzirMtr(
            DocumentoDossieProdutoMtrException falha
    ) {
        DocumentoDossieProdutoMtrException.Erro erro = falha.erro();
        FalhaInclusaoDocumentoDossieProduto.Tipo tipo = switch (falha) {
            case DocumentoDossieProdutoMtrException.Negocio ignored ->
                    FalhaInclusaoDocumentoDossieProduto.Tipo.NEGOCIO;
            case DocumentoDossieProdutoMtrException.TecnicaCliente ignored ->
                    FalhaInclusaoDocumentoDossieProduto.Tipo.TECNICA_CLIENTE;
            case DocumentoDossieProdutoMtrException.Servidor ignored ->
                    FalhaInclusaoDocumentoDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaInclusaoDocumentoDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaInclusaoDocumentoDossieProduto(
                tipo,
                falha.status(),
                erro != null ? erro.recurso() : null,
                erro != null ? erro.idErro() : null,
                erro != null ? erro.codigoErro() : null,
                mensagens(erro),
                erro != null ? erro.detalhe() : null,
                erro != null ? erro.stacktrace() : null,
                falha);
    }

    private static List<String> mensagens(DocumentoDossieProdutoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (DocumentoDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

    private static void setIntAttribute(Span span, String nome, Integer valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

    private static void setStringAttribute(Span span, String nome, String valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
