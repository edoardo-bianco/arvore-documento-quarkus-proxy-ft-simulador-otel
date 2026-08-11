package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ProdutoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ProdutoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ProdutoDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@ProdutoMtr
public class ProdutoDossieProdutoMtrAdapter
        implements SolicitarAlteracaoProdutosContratadosDossieProduto {

    private static final Logger LOG = Logger.getLogger(ProdutoDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String ALTERAR = "alterar-produtos-contratados-dossie-produto-v1";
    private static final String DOSSIE_PRODUTO_ID = "dossie_produto_id";
    private static final String PRODUTOS_QUANTIDADE = "produtos_quantidade";

    private final ProdutoDossieProdutoMtrClient client;
    private final ProdutoDossieProdutoMtrMapper mapper;

    @Inject
    public ProdutoDossieProdutoMtrAdapter(
            @RestClient ProdutoDossieProdutoMtrClient client,
            ProdutoDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.produto.alterar", kind = SpanKind.CLIENT)
    public Uni<Void> alterar(ComandoAlteracaoProdutosContratadosDossieProduto comando) {
        Long identificador = comando != null ? comando.identificadorDossieProduto() : null;
        Integer quantidadeProdutos = comando != null && comando.produtos() != null
                ? comando.produtos().size() : null;

        Span span = Span.current();
        span.setAttribute("mtr.servico", DOSSIE);
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "PATCH");
        span.setAttribute("url.path", "/simtr-dossie-produto/v1/dossie-produto/"
                + identificador + "/produto");
        setLongAttribute(span, "dossie_produto.id", identificador);
        setIntAttribute(span, "dossie_produto.produtos.quantidade", quantidadeProdutos);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.produto.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE,
                        COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        OPERACAO, ALTERAR,
                        DOSSIE_PRODUTO_ID, identificador,
                        PRODUTOS_QUANTIDADE, quantidadeProdutos));

        return client.alterar(identificador, mapper.paraMtr(comando))
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    ObservabilityLog.info(
                            LOG,
                            "mtr.dossie-produto.produto.chamada.concluida",
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE,
                                    COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE,
                                    OPERACAO, ALTERAR,
                                    DOSSIE_PRODUTO_ID, identificador,
                                    PRODUTOS_QUANTIDADE, quantidadeProdutos,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", erro.getClass().getName());
                    ObservabilityLog.error(
                            LOG,
                            "mtr.dossie-produto.produto.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE,
                                    COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE,
                                    OPERACAO, ALTERAR,
                                    DOSSIE_PRODUTO_ID, identificador,
                                    PRODUTOS_QUANTIDADE, quantidadeProdutos,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                })
                .onFailure().transform(ProdutoDossieProdutoMtrAdapter::traduzir);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof ProdutoDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        var tipo = falha instanceof TimeoutException
                ? FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.TIMEOUT
                : FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaAlteracaoProdutosContratadosDossieProduto(
                tipo, null, DOSSIE, null, null,
                null, null, null, falha);
    }

    private static FalhaAlteracaoProdutosContratadosDossieProduto traduzirMtr(
            ProdutoDossieProdutoMtrException falha
    ) {
        ProdutoDossieProdutoMtrException.Erro erro = falha.erro();
        var tipo = switch (falha) {
            case ProdutoDossieProdutoMtrException.Negocio _ ->
                    FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.NEGOCIO;
            case ProdutoDossieProdutoMtrException.TecnicaCliente _ ->
                    FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.TECNICA_CLIENTE;
            case ProdutoDossieProdutoMtrException.Servidor _ ->
                    FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default ->
                    FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaAlteracaoProdutosContratadosDossieProduto(
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

    @SuppressWarnings("java:S1168") // Null preserva a ausência da coleção no erro externo.
    private static List<String> mensagens(ProdutoDossieProdutoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (ProdutoDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
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
}
