package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.ConsultarDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ConsultaDossieProdutoObservabilidade implements ConsultarDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            ConsultaDossieProdutoObservabilidade.class
    );
    private static final String EVENTO = "simtr-hub.dossie-produto.consulta.service.";
    private static final String OPERACAO = "consultar-dossie-produto";

    private final ConsultarDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public ConsultaDossieProdutoObservabilidade(
            ObterDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new ConsultarDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan(value = "simtr-hub.service.dossie-produto.consultar", kind = SpanKind.INTERNAL)
    public Uni<DossieProdutoConsultado> executar(IdentificadorDossieProduto identificador) {
        Long id = identificador != null ? identificador.valor() : null;
        String origem = simuladorHabilitado ? "mock" : "mtr";
        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado", simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", origem);
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(LOG, EVENTO + "iniciada", campos(id, origem, null, null, null, null));

        return casoDeUso.executar(identificador)
                .invoke(resposta -> registrarSucesso(span, id, origem, resposta))
                .onFailure().invoke(erro -> registrarFalha(span, id, origem, erro));
    }

    private static void registrarSucesso(
            Span span,
            Long id,
            String origem,
            DossieProdutoConsultado resposta
    ) {
        Integer clientes = tamanho(resposta != null ? resposta.clientes() : null);
        Integer unidades = tamanho(resposta != null ? resposta.unidadesTratamento() : null);
        Integer produtos = tamanho(resposta != null ? resposta.produtosContratados() : null);
        setIntAttribute(span, "dossie_produto.clientes.quantidade", clientes);
        setIntAttribute(span, "dossie_produto.unidades_tratamento.quantidade", unidades);
        setIntAttribute(span, "dossie_produto.produtos_contratados.quantidade", produtos);
        ObservabilityLog.info(
                LOG,
                EVENTO + "concluida",
                campos(id, origem, clientes, unidades, produtos, "sucesso")
        );
    }

    private static void registrarFalha(Span span, Long id, String origem, Throwable erro) {
        String tipoErro = erro.getClass().getSimpleName();
        span.setStatus(StatusCode.ERROR, tipoErro);
        span.setAttribute("erro.tipo", tipoErro);
        ObservabilityLog.info(
                LOG,
                EVENTO + "falhou",
                ObservabilityLog.fields(
                        "camada", "application",
                        "componente", "DossieProdutoService",
                        "operacao", OPERACAO,
                        "dossie_produto_id", id,
                        "origem", origem,
                        "simulador_habilitado", "mock".equals(origem),
                        "erro_tipo", tipoErro,
                        "resultado", "erro"
                )
        );
    }

    private static java.util.Map<String, Object> campos(
            Long id,
            String origem,
            Integer clientes,
            Integer unidades,
            Integer produtos,
            String resultado
    ) {
        return ObservabilityLog.fields(
                "camada", "application",
                "componente", "DossieProdutoService",
                "operacao", OPERACAO,
                "dossie_produto_id", id,
                "origem", origem,
                "simulador_habilitado", "mock".equals(origem),
                "clientes_quantidade", clientes,
                "unidades_tratamento_quantidade", unidades,
                "produtos_contratados_quantidade", produtos,
                "resultado", resultado
        );
    }

    private static Integer tamanho(List<?> itens) {
        return itens != null ? itens.size() : null;
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
