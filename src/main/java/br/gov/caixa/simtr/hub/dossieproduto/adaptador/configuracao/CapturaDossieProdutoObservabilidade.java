package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.CapturarDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CapturarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CapturaDossieProdutoObservabilidade implements CapturarDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            CapturaDossieProdutoObservabilidade.class);
    private static final String EVENTO =
            "simtr-hub.dossie-produto.captura.processamento.";
    private static final String OPERACAO = "capturar-dossie-produto-v1";
    private static final String RESULTADO = "resultado";

    private final CapturarDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public CapturaDossieProdutoObservabilidade(
            SolicitarCapturaDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new CapturarDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan(value = "simtr-hub.service.dossie-produto.capturar",
            kind = SpanKind.INTERNAL)
    public Uni<ResultadoCapturaDossieProduto> executar(
            IdentificadorDossieProduto identificador) {
        Long id = identificador != null ? identificador.valor() : null;
        String origem = simuladorHabilitado ? "mock" : "mtr";
        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado",
                simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", origem);
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(LOG, EVENTO + "iniciada", campos(id, origem, null, null));

        return casoDeUso.executar(identificador)
                .invoke(_ -> registrarSucesso(span, id, origem))
                .onFailure().invoke(erro -> registrarFalha(span, id, origem, erro));
    }

    private static void registrarSucesso(Span span, Long id, String origem) {
        span.setAttribute(RESULTADO, "sucesso");
        ObservabilityLog.info(
                LOG,
                EVENTO + "concluida",
                campos(id, origem, "sucesso", null));
    }

    private static void registrarFalha(
            Span span,
            Long id,
            String origem,
            Throwable erro
    ) {
        String tipoErro = erro.getClass().getSimpleName();
        span.setStatus(StatusCode.ERROR, "falha na captura de dossie produto");
        span.setAttribute("erro.tipo", tipoErro);
        span.setAttribute(RESULTADO, "erro");
        ObservabilityLog.info(
                LOG,
                EVENTO + "falhou",
                campos(id, origem, "erro", tipoErro));
    }

    private static java.util.Map<String, Object> campos(
            Long id,
            String origem,
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
                "erro_tipo", tipoErro,
                RESULTADO, resultado);
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
