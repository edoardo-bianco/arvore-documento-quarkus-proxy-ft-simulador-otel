package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CriacaoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.CriacaoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CriacaoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.CriacaoDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
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
@CriacaoMtr
public class CriacaoDossieProdutoMtrAdapter implements SolicitarCriacaoDossieProduto {

    private static final Logger LOG = Logger.getLogger(CriacaoDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String CRIAR = "criar-dossie-produto-v1";

    private final CriacaoDossieProdutoMtrClient client;
    private final CriacaoDossieProdutoMtrMapper mapper;

    @Inject
    public CriacaoDossieProdutoMtrAdapter(
            @RestClient CriacaoDossieProdutoMtrClient client,
            CriacaoDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.criar", kind = SpanKind.CLIENT)
    public Uni<ResultadoCriacaoDossieProduto> criar(ComandoCriacaoDossieProduto comando) {
        Long processo = comando != null ? comando.processo() : null;
        Long chaveCorrelacaoCanal = comando != null ? comando.chaveCorrelacaoCanal() : null;
        Integer quantidadeClientes = comando != null && comando.clientes() != null
                ? comando.clientes().size() : null;

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto");
        setLongAttribute(span, "dossie_produto.processo", processo);
        setLongAttribute(span, "dossie_produto.chave_correlacao_canal", chaveCorrelacaoCanal);
        setIntAttribute(span, "dossie_produto.clientes.quantidade", quantidadeClientes);

        ObservabilityLog.info(LOG, "mtr.dossie-produto.criacao.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE, OPERACAO, CRIAR,
                        "processo", processo,
                        "chave_correlacao_canal", chaveCorrelacaoCanal,
                        "clientes_quantidade", quantidadeClientes));

        return client.criar(mapper.paraMtr(comando))
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    if (resposta != null && resposta.id() != null) {
                        span.setAttribute("dossie_produto.id", resposta.id());
                    }
                    ObservabilityLog.info(LOG, "mtr.dossie-produto.criacao.chamada.concluida",
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE, OPERACAO, CRIAR,
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "dossie_produto_id", resposta != null ? resposta.id() : null,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", erro.getClass().getName());
                    ObservabilityLog.error(LOG, "mtr.dossie-produto.criacao.chamada.falhou", erro,
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE, OPERACAO, CRIAR,
                                    "processo", processo,
                                    "chave_correlacao_canal", chaveCorrelacaoCanal,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                })
                .map(mapper::paraResultado)
                .onFailure().transform(CriacaoDossieProdutoMtrAdapter::traduzir);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof CriacaoDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaCriacaoDossieProduto.Tipo tipo = falha instanceof TimeoutException
                ? FalhaCriacaoDossieProduto.Tipo.TIMEOUT
                : FalhaCriacaoDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaCriacaoDossieProduto(
                tipo, null, "simtr-dossie-produto", null, null,
                null, null, null, falha);
    }

    private static FalhaCriacaoDossieProduto traduzirMtr(CriacaoDossieProdutoMtrException falha) {
        CriacaoDossieProdutoMtrException.Erro erro = falha.erro();
        FalhaCriacaoDossieProduto.Tipo tipo = switch (falha) {
            case CriacaoDossieProdutoMtrException.Negocio ignored ->
                    FalhaCriacaoDossieProduto.Tipo.NEGOCIO;
            case CriacaoDossieProdutoMtrException.TecnicaCliente ignored ->
                    FalhaCriacaoDossieProduto.Tipo.TECNICA_CLIENTE;
            case CriacaoDossieProdutoMtrException.Servidor ignored ->
                    FalhaCriacaoDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaCriacaoDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaCriacaoDossieProduto(
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

    private static List<String> mensagens(CriacaoDossieProdutoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (CriacaoDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
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
