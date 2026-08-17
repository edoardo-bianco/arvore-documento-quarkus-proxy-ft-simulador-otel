package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.CapturaDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.captura.CapturaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CapturaDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.CapturaDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
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
@CapturaMtr
public class CapturaDossieProdutoMtrAdapter implements SolicitarCapturaDossieProduto {

    private static final Logger LOG = Logger.getLogger(CapturaDossieProdutoMtrAdapter.class);
    private static final String CAMADA = "camada";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMPONENTE = "componente";
    private static final String GATEWAY = "DossieProdutoGateway";
    private static final String DEPENDENCIA = "dependencia";
    private static final String DOSSIE = "simtr-dossie-produto";
    private static final String OPERACAO = "operacao";
    private static final String CAPTURAR = "capturar-dossie-produto-v1";
    private static final String DOSSIE_PRODUTO_ID = "dossie_produto_id";
    private static final String RESPOSTA_INVALIDA =
            "Resposta MTR invalida para captura de dossie produto";

    private final CapturaDossieProdutoMtrClient client;
    private final CapturaDossieProdutoMtrMapper mapper;

    @Inject
    public CapturaDossieProdutoMtrAdapter(
            @RestClient CapturaDossieProdutoMtrClient client,
            CapturaDossieProdutoMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.dossie-produto.capturar", kind = SpanKind.CLIENT)
    public Uni<ResultadoCapturaDossieProduto> capturar(
            IdentificadorDossieProduto identificador
    ) {
        Long id = identificador != null ? identificador.valor() : null;
        Span span = Span.current();
        span.setAttribute("mtr.servico", DOSSIE);
        span.setAttribute("mtr.api", "dossie-produto-v1");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute(
                "url.path",
                "/simtr/dossie-produto/v1/dossie-produto/{id}/capturar");
        setLongAttribute(span, "dossie_produto.id", id);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.captura.chamada.iniciada",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE,
                        COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        OPERACAO, CAPTURAR,
                        DOSSIE_PRODUTO_ID, id));

        return client.capturar(id)
                .invoke(resposta -> validarResposta(id, resposta))
                .invoke(_ -> registrarConclusao(span, id))
                .onFailure().invoke(erro -> registrarFalha(span, id, erro))
                .map(mapper::paraResultado)
                .onFailure().transform(CapturaDossieProdutoMtrAdapter::traduzir);
    }

    private static void validarResposta(
            Long identificadorSolicitado,
            CapturaDossieProdutoMtrResponse resposta
    ) {
        if (resposta == null || resposta.id() == null
                || identificadorSolicitado == null
                || !identificadorSolicitado.equals(resposta.id())) {
            throw new IllegalStateException(RESPOSTA_INVALIDA);
        }
    }

    private static void registrarConclusao(Span span, Long identificador) {
        span.setAttribute("mtr.resposta.sucesso", true);
        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.captura.chamada.concluida",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE,
                        COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        OPERACAO, CAPTURAR,
                        DOSSIE_PRODUTO_ID, identificador,
                        "resultado", "sucesso"));
    }

    private static void registrarFalha(Span span, Long identificador, Throwable erro) {
        String tipoErro = erro.getClass().getSimpleName();
        span.setStatus(StatusCode.ERROR, "falha na captura MTR de dossie produto");
        span.setAttribute("mtr.resposta.sucesso", false);
        span.setAttribute("erro.tipo", tipoErro);

        ObservabilityLog.info(
                LOG,
                "mtr.dossie-produto.captura.chamada.falhou",
                ObservabilityLog.fields(
                        CAMADA, INFRASTRUCTURE,
                        COMPONENTE, GATEWAY,
                        DEPENDENCIA, DOSSIE,
                        OPERACAO, CAPTURAR,
                        DOSSIE_PRODUTO_ID, identificador,
                        "erro_tipo", tipoErro,
                        "resultado", "erro"));
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof CapturaDossieProdutoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaCapturaDossieProduto.Tipo tipo = falha instanceof TimeoutException
                ? FalhaCapturaDossieProduto.Tipo.TIMEOUT
                : FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaCapturaDossieProduto(
                tipo,
                null,
                DOSSIE,
                null,
                null,
                null,
                null,
                null,
                falha);
    }

    private static FalhaCapturaDossieProduto traduzirMtr(
            CapturaDossieProdutoMtrException falha
    ) {
        CapturaDossieProdutoMtrException.Erro erro = falha.erro();
        FalhaCapturaDossieProduto.Tipo tipo = switch (falha) {
            case CapturaDossieProdutoMtrException.Negocio _ ->
                    FalhaCapturaDossieProduto.Tipo.NEGOCIO;
            case CapturaDossieProdutoMtrException.TecnicaCliente _ ->
                    FalhaCapturaDossieProduto.Tipo.TECNICA_CLIENTE;
            case CapturaDossieProdutoMtrException.Servidor _ ->
                    FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaCapturaDossieProduto(
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
    private static List<String> mensagens(CapturaDossieProdutoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (CapturaDossieProdutoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }

    private static void setLongAttribute(Span span, String nome, Long valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }
}
