package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ConsultaDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.consulta.ConsultaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ConsultaDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ConsultaDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
@ConsultaDossieProdutoMtr
public class ConsultaDossieProdutoMtrAdapter implements ObterDossieProduto {

    private static final Logger LOG = Logger.getLogger(ConsultaDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String CONSULTAR = "consultar-dossie-produto-v2";
    private static final String DOSSIE_PRODUTO_ID = "dossie_produto_id";
    private static final String RESPOSTA_INVALIDA =
            "Resposta MTR invalida para consulta de dossie produto";

    private final ConsultaDossieProdutoMtrClient client;
    private final ConsultaDossieProdutoMtrMapper mapper;

    @Inject
    public ConsultaDossieProdutoMtrAdapter(
            @RestClient ConsultaDossieProdutoMtrClient client,
            ConsultaDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.consultar", kind = SpanKind.CLIENT)
    public Uni<DossieProdutoConsultado> obter(IdentificadorDossieProduto identificador) {
        Long id = identificador != null ? identificador.valor() : null;
        Span span = Span.current();
        span.setAttribute("mtr.servico", DOSSIE);
        span.setAttribute("mtr.api", "dossie-produto-v2");
        span.setAttribute("http.request.method", "GET");
        span.setAttribute("url.path", "/simtr/dossie-produto/v2/dossie-produto/{id}");
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.consulta.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE,
                        COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        OPERACAO, CONSULTAR,
                        DOSSIE_PRODUTO_ID, id
                )
        );

        return client.consultar(id)
                .invoke(resposta -> validarResposta(id, resposta))
                .invoke(resposta -> registrarConclusao(span, id, resposta))
                .onFailure().invoke(erro -> registrarFalha(span, id, erro))
                .map(mapper::paraDominio)
                .onFailure().transform(ConsultaDossieProdutoMtrAdapter::traduzir);
    }

    private static void validarResposta(
            Long identificadorSolicitado,
            ConsultaDossieProdutoMtrResponse resposta
    ) {
        if (resposta == null || resposta.id() == null
                || identificadorSolicitado == null
                || !identificadorSolicitado.equals(resposta.id())) {
            throw new IllegalStateException(RESPOSTA_INVALIDA);
        }
    }

    private static void registrarConclusao(
            Span span,
            Long identificador,
            ConsultaDossieProdutoMtrResponse resposta
    ) {
        Integer quantidadeClientes = tamanho(resposta.clientes());
        Integer quantidadeUnidades = tamanho(resposta.unidadesTratamento());
        Integer quantidadeProdutos = tamanho(resposta.produtosContratados());

        span.setAttribute("mtr.resposta.sucesso", true);
        setIntAttribute(span, "dossie_produto.clientes.quantidade", quantidadeClientes);
        setIntAttribute(
                span,
                "dossie_produto.unidades_tratamento.quantidade",
                quantidadeUnidades
        );
        setIntAttribute(
                span,
                "dossie_produto.produtos_contratados.quantidade",
                quantidadeProdutos
        );

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.consulta.chamada.concluida",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE,
                        COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        OPERACAO, CONSULTAR,
                        DOSSIE_PRODUTO_ID, identificador,
                        "clientes_quantidade", quantidadeClientes,
                        "unidades_tratamento_quantidade", quantidadeUnidades,
                        "produtos_contratados_quantidade", quantidadeProdutos,
                        "resultado", "sucesso"
                )
        );
    }

    private static void registrarFalha(Span span, Long identificador, Throwable erro) {
        String tipoErro = erro.getClass().getSimpleName();
        span.setStatus(StatusCode.ERROR, "falha na consulta MTR de dossie produto");
        span.setAttribute("mtr.resposta.sucesso", false);
        span.setAttribute("erro.tipo", tipoErro);

        // O erro externo pode conter PII; o evento registra somente sua classificacao.
        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.consulta.chamada.falhou",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE,
                        COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        OPERACAO, CONSULTAR,
                        DOSSIE_PRODUTO_ID, identificador,
                        "erro_tipo", tipoErro,
                        "resultado", "erro"
                )
        );
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof ConsultaDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaConsultaDossieProduto.Tipo tipo = falha instanceof TimeoutException
                ? FalhaConsultaDossieProduto.Tipo.TIMEOUT
                : FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaConsultaDossieProduto(
                tipo,
                null,
                DOSSIE,
                null,
                null,
                null,
                null,
                null,
                falha
        );
    }

    private static FalhaConsultaDossieProduto traduzirMtr(
            ConsultaDossieProdutoMtrException falha
    ) {
        ConsultaDossieProdutoMtrException.Erro erro = falha.erro();
        FalhaConsultaDossieProduto.Tipo tipo = switch (falha) {
            case ConsultaDossieProdutoMtrException.Negocio _ ->
                    FalhaConsultaDossieProduto.Tipo.NEGOCIO;
            case ConsultaDossieProdutoMtrException.TecnicaCliente _ ->
                    FalhaConsultaDossieProduto.Tipo.TECNICA_CLIENTE;
            case ConsultaDossieProdutoMtrException.Servidor _ ->
                    FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaConsultaDossieProduto(
                tipo,
                falha.status(),
                erro != null ? erro.recurso() : null,
                erro != null ? erro.idErro() : null,
                erro != null ? erro.codigoErro() : null,
                mensagens(erro),
                erro != null ? erro.detalhe() : null,
                erro != null ? erro.stacktrace() : null,
                falha
        );
    }

    @SuppressWarnings("java:S1168") // Null preserva a ausência da coleção no erro externo.
    private static List<String> mensagens(ConsultaDossieProdutoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (ConsultaDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }

    private static Integer tamanho(List<?> elementos) {
        return elementos != null ? elementos.size() : null;
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
