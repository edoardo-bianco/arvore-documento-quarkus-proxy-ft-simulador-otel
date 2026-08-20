package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.ConsultarDocumentosDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ConsultaDocumentosDossieProdutoObservabilidade
        implements ConsultarDocumentosDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            ConsultaDocumentosDossieProdutoObservabilidade.class);
    private static final String EVENTO =
            "simtr-hub.dossie-produto.documentos.consulta.service.";
    private static final String OPERACAO = "consultar-documentos-dossie-produto-v4";

    private final ConsultarDocumentosDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public ConsultaDocumentosDossieProdutoObservabilidade(
            ObterDocumentosDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new ConsultarDocumentosDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan(
            value = "simtr-hub.service.dossie-produto.documentos.consultar",
            kind = SpanKind.INTERNAL)
    public Uni<List<DocumentoDossieProdutoConsultado>> executar(
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        Long id = criterios != null && criterios.identificador() != null
                ? criterios.identificador().valor()
                : null;
        String origem = simuladorHabilitado ? "mock" : "mtr";
        Span span = Span.current();
        span.setAttribute(
                "simtr_hub.simulador_dossie_produto_habilitado",
                simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", origem);
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(
                LOG,
                EVENTO + "iniciada",
                campos(id, origem, null, null, null));

        return casoDeUso.executar(criterios)
                .invoke(documentos -> registrarSucesso(span, id, origem, documentos))
                .onFailure().invoke(erro -> registrarFalha(span, id, origem, erro));
    }

    private static void registrarSucesso(
            Span span,
            Long id,
            String origem,
            List<DocumentoDossieProdutoConsultado> documentos
    ) {
        Integer quantidade = documentos != null ? documentos.size() : null;
        setIntAttribute(span, "dossie_produto.documentos.quantidade", quantidade);
        ObservabilityLog.info(
                LOG,
                EVENTO + "concluida",
                campos(id, origem, quantidade, "sucesso", null));
    }

    private static void registrarFalha(
            Span span,
            Long id,
            String origem,
            Throwable erro
    ) {
        String tipoErro = erro.getClass().getSimpleName();
        span.setStatus(StatusCode.ERROR, "falha na consulta de documentos do dossie produto");
        span.setAttribute("erro.tipo", tipoErro);
        ObservabilityLog.info(
                LOG,
                EVENTO + "falhou",
                campos(id, origem, null, "erro", tipoErro));
    }

    private static Map<String, Object> campos(
            Long id,
            String origem,
            Integer quantidade,
            String resultado,
            String tipoErro
    ) {
        return ObservabilityLog.fields(
                "camada", "application",
                "componente", "DossieProdutoService",
                "operacao", OPERACAO,
                "dossie_produto_id", id,
                "origem", origem,
                "simulador_habilitado", "mock".equals(origem),
                "documentos_quantidade", quantidade,
                "erro_tipo", tipoErro,
                "resultado", resultado);
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

    private static void setIntAttribute(Span span, String nome, Integer valor) {
        if (valor != null) {
            span.setAttribute(nome, valor.longValue());
        }
    }
}
