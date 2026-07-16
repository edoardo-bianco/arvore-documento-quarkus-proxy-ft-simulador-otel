package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ValidacaoNegocialMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ValidacaoNegocialDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ValidacaoNegocialDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ValidacaoNegocialDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
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
@ValidacaoNegocialMtr
public class ValidacaoNegocialDossieProdutoMtrAdapter
        implements SolicitarRegistroValidacaoNegocialDossieProduto {

    private static final Logger LOG = Logger.getLogger(
            ValidacaoNegocialDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";

    private final ValidacaoNegocialDossieProdutoMtrClient client;
    private final ValidacaoNegocialDossieProdutoMtrMapper mapper;

    @Inject
    public ValidacaoNegocialDossieProdutoMtrAdapter(
            @RestClient ValidacaoNegocialDossieProdutoMtrClient client,
            ValidacaoNegocialDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.validacao-negocial.registrar",
            kind = SpanKind.CLIENT)
    public Uni<Void> registrar(ComandoRegistroValidacaoNegocialDossieProduto comando) {
        Long identificador = comando != null ? comando.identificadorDossieProduto() : null;
        Integer quantidadeVerificacoes = comando != null && comando.verificacoes() != null
                ? comando.verificacoes().size() : null;
        Integer quantidadeRespostas = comando != null && comando.respostasFormulario() != null
                ? comando.respostasFormulario().size() : null;

        Span span = Span.current();
        span.setAttribute("mtr.servico", "simtr-dossie-produto");
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "PATCH");
        span.setAttribute("url.path", "/simtr/dossie-produto/v1/dossie-produto/"
                + identificador + "/validacao-negocial");
        setLongAttribute(span, "dossie_produto.id", identificador);
        setIntAttribute(span, "dossie_produto.validacao.verificacoes.quantidade",
                quantidadeVerificacoes);
        setIntAttribute(span, "dossie_produto.validacao.respostas_formulario.quantidade",
                quantidadeRespostas);

        ObservabilityLog.info(LOG,
                "mtr.dossie-produto.validacao-negocial.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        "operacao", "registrar-validacao-negocial-dossie-produto-v1",
                        "dossie_produto_id", identificador,
                        "validacao_verificacoes_quantidade", quantidadeVerificacoes,
                        "validacao_respostas_formulario_quantidade", quantidadeRespostas));

        return client.registrar(identificador, mapper.paraMtr(comando))
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    ObservabilityLog.info(LOG,
                            "mtr.dossie-produto.validacao-negocial.chamada.concluida",
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE,
                                    "operacao",
                                    "registrar-validacao-negocial-dossie-produto-v1",
                                    "dossie_produto_id", identificador,
                                    "resultado", "sucesso"));
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", erro.getClass().getName());
                    ObservabilityLog.error(LOG,
                            "mtr.dossie-produto.validacao-negocial.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    CAMADA, INFRASTRUCTURE, COMPONENTE, GATEWAY,
                                    DEPENDENCIA, DOSSIE,
                                    "operacao",
                                    "registrar-validacao-negocial-dossie-produto-v1",
                                    "dossie_produto_id", identificador,
                                    "erro_tipo", erro.getClass().getSimpleName(),
                                    "resultado", "erro"));
                })
                .onFailure().transform(ValidacaoNegocialDossieProdutoMtrAdapter::traduzir);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof ValidacaoNegocialDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaRegistroValidacaoNegocialDossieProduto.Tipo tipo = falha instanceof TimeoutException
                ? FalhaRegistroValidacaoNegocialDossieProduto.Tipo.TIMEOUT
                : FalhaRegistroValidacaoNegocialDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaRegistroValidacaoNegocialDossieProduto(
                tipo, null, "simtr-dossie-produto", null, null,
                null, null, null, falha);
    }

    private static FalhaRegistroValidacaoNegocialDossieProduto traduzirMtr(
            ValidacaoNegocialDossieProdutoMtrException falha
    ) {
        ValidacaoNegocialDossieProdutoMtrException.Erro erro = falha.erro();
        FalhaRegistroValidacaoNegocialDossieProduto.Tipo tipo = switch (falha) {
            case ValidacaoNegocialDossieProdutoMtrException.Negocio ignored ->
                    FalhaRegistroValidacaoNegocialDossieProduto.Tipo.NEGOCIO;
            case ValidacaoNegocialDossieProdutoMtrException.TecnicaCliente ignored ->
                    FalhaRegistroValidacaoNegocialDossieProduto.Tipo.TECNICA_CLIENTE;
            case ValidacaoNegocialDossieProdutoMtrException.Servidor ignored ->
                    FalhaRegistroValidacaoNegocialDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default ->
                    FalhaRegistroValidacaoNegocialDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaRegistroValidacaoNegocialDossieProduto(
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

    private static List<String> mensagens(
            ValidacaoNegocialDossieProdutoMtrException.Erro erro
    ) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (ValidacaoNegocialDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
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
