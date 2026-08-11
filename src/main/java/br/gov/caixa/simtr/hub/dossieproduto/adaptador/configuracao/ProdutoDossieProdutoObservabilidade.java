package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso.AlterarProdutosContratadosDossieProdutoCasoDeUso;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.AlterarProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProdutoDossieProdutoObservabilidade
        implements AlterarProdutosContratadosDossieProduto {

    private static final Logger LOG = Logger.getLogger(ProdutoDossieProdutoObservabilidade.class);
    private static final String CAMADA = "application";
    private static final String COMPONENTE = "DossieProdutoService";
    private static final String OPERACAO = "alterar-produtos-contratados-dossie-produto";
    private static final String CAMADA_KEY = "camada";
    private static final String COMPONENTE_KEY = "componente";
    private static final String OPERACAO_KEY = "operacao";
    private static final String DOSSIE_PRODUTO_ID_KEY = "dossie_produto_id";
    private static final String PRODUTOS_QUANTIDADE_KEY = "produtos_quantidade";

    private final AlterarProdutosContratadosDossieProduto casoDeUso;
    private final boolean simuladorHabilitado;

    @Inject
    public ProdutoDossieProdutoObservabilidade(
            SolicitarAlteracaoProdutosContratadosDossieProduto portaSaida,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        this.casoDeUso = new AlterarProdutosContratadosDossieProdutoCasoDeUso(portaSaida);
        this.simuladorHabilitado = simuladorHabilitado;
    }

    @Override
    @WithSpan("simtr-hub.service.dossie-produto.produto.alterar")
    public Uni<Void> executar(ComandoAlteracaoProdutosContratadosDossieProduto comando) {
        Long id = comando != null ? comando.identificadorDossieProduto() : null;
        Integer quantidadeProdutos = comando != null && comando.produtos() != null
                ? comando.produtos().size() : null;

        Span span = Span.current();
        span.setAttribute("simtr_hub.simulador_dossie_produto_habilitado",
                simuladorHabilitado);
        span.setAttribute("simtr_hub.origem_dados", simuladorHabilitado ? "mock" : "mtr");
        setLongAttribute(span, "dossie_produto.id", id);
        setIntAttribute(span, "dossie_produto.produtos.quantidade", quantidadeProdutos);

        ObservabilityLog.info(
                LOG,
                "simtr-hub.dossie-produto.produto.service.iniciado",
                ObservabilityLog.fields(
                        CAMADA_KEY, CAMADA,
                        COMPONENTE_KEY, COMPONENTE,
                        OPERACAO_KEY, OPERACAO,
                        DOSSIE_PRODUTO_ID_KEY, id,
                        PRODUTOS_QUANTIDADE_KEY, quantidadeProdutos,
                        "origem_dados", simuladorHabilitado ? "mock" : "mtr",
                        "simulador_habilitado", simuladorHabilitado));

        return casoDeUso.executar(comando)
                .invoke(resposta -> ObservabilityLog.info(
                        LOG,
                        "simtr-hub.dossie-produto.produto.service.concluido",
                        ObservabilityLog.fields(
                                CAMADA_KEY, CAMADA,
                                COMPONENTE_KEY, COMPONENTE,
                                OPERACAO_KEY, OPERACAO,
                                DOSSIE_PRODUTO_ID_KEY, id,
                                PRODUTOS_QUANTIDADE_KEY, quantidadeProdutos,
                                "resultado", "sucesso")))
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    ObservabilityLog.error(
                            LOG,
                            "simtr-hub.dossie-produto.produto.service.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA_KEY, CAMADA,
                                    COMPONENTE_KEY, COMPONENTE,
                                    OPERACAO_KEY, OPERACAO,
                                    DOSSIE_PRODUTO_ID_KEY, id,
                                    PRODUTOS_QUANTIDADE_KEY, quantidadeProdutos,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                });
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
}
