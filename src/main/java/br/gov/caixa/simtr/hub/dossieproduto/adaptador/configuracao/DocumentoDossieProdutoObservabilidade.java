package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.IncluirDocumentoDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IncluirDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DocumentoDossieProdutoObservabilidade
        implements IncluirDocumentoDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            DocumentoDossieProdutoObservabilidade.class);

    private final IncluirDocumentoDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public DocumentoDossieProdutoObservabilidade(
            SolicitarInclusaoDocumentoDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new IncluirDocumentoDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.dossie-produto.documento.incluir")
    public Uni<ResultadoInclusaoDocumentoDossieProduto> executar(
            ComandoInclusaoDocumentoDossieProduto comando
    ) {
        Long id = comando != null ? comando.identificadorDossieProduto() : null;
        String tipoDocumento = comando != null ? comando.tipoDocumento() : null;
        Integer quantidadeAtributos = comando != null && comando.atributos() != null
                ? comando.atributos().size() : null;
        Integer quantidadePropriedades = comando != null && comando.propriedades() != null
                ? comando.propriedades().size() : null;

        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado",
                simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        setLongAttribute(span, "dossie_produto.id", id);
        setStringAttribute(span, "dossie_produto.documento.tipo", tipoDocumento);
        setIntAttribute(span, "dossie_produto.documento.atributos.quantidade",
                quantidadeAtributos);
        setIntAttribute(span, "dossie_produto.documento.propriedades.quantidade",
                quantidadePropriedades);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.documento.service.iniciado",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", "incluir-documento-dossie-produto",
                        "dossie_produto_id", id,
                        "tipo_documento", tipoDocumento,
                        "documento_atributos_quantidade", quantidadeAtributos,
                        "documento_propriedades_quantidade", quantidadePropriedades,
                        "simulador_habilitado", simuladorHabilitado));

        return casoDeUso.executar(comando)
                .invoke(resposta -> {
                    registrarConclusao(span, id, resposta);
                })
                .onFailure().invoke(erro -> {
                    registrarFalha(span, id, tipoDocumento, erro);
                });
    }

    private static void registrarConclusao(Span span, Long id,
            ResultadoInclusaoDocumentoDossieProduto resposta) {
        if (resposta != null && resposta.identificadorDocumento() != null) {
            span.setAttribute("dossie_produto.documento.id", resposta.identificadorDocumento());
        }
        if (resposta != null && resposta.identificadorInstanciaDocumento() != null) {
            span.setAttribute("dossie_produto.documento.instancia.id",
                    resposta.identificadorInstanciaDocumento());
        }
        ObservabilityLog.info(LOG, "simtr-hub.dossie-produto.documento.service.concluido",
                ObservabilityLog.fields("camada", "application", "componente", "DossieProdutoService",
                        "operacao", "incluir-documento-dossie-produto", "dossie_produto_id", id,
                        "id_documento", resposta != null ? resposta.identificadorDocumento() : null,
                        "id_instancia_documento", resposta != null
                                ? resposta.identificadorInstanciaDocumento() : null,
                        "resultado", "sucesso"));
    }

    private static void registrarFalha(Span span, Long id, String tipoDocumento, Throwable erro) {
        span.recordException(erro);
        span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
        ObservabilityLog.error(LOG, "simtr-hub.dossie-produto.documento.service.falhou", erro,
                ObservabilityLog.fields("camada", "application", "componente", "DossieProdutoService",
                        "operacao", "incluir-documento-dossie-produto", "dossie_produto_id", id,
                        "tipo_documento", tipoDocumento, "erro_tipo", erro.getClass().getSimpleName(),
                        "resultado", "erro"));
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
