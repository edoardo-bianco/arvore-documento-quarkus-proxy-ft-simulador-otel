package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao.qualificador.GestaoDocumentoMtr;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.client.GestaoDocumentoClient;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.erro.GestaoDocumentoMtrException;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.mapper.CredencialContainerMtrMapper;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.erro.FalhaObtencaoCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
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
@GestaoDocumentoMtr
public class GestaoDocumentoMtrAdapter implements SolicitarCredencialContainer {

    private static final Logger LOG = Logger.getLogger(GestaoDocumentoMtrAdapter.class);
    private static final String PACOTE_EXCECAO_MTR_LEGADA =
            "br.gov.caixa.simtr.hub.arquitetura.excecao.";
    private static final String CAMADA = "infrastructure";
    private static final String COMPONENTE = "GestaoDocumentoGateway";
    private static final String DEPENDENCIA = "simtr-gestao-documento";
    private static final String OPERACAO = "gerar-credencial-container-v1";

    private final GestaoDocumentoClient client;
    private final CredencialContainerMtrMapper mapper;

    @Inject
    public GestaoDocumentoMtrAdapter(
            @RestClient GestaoDocumentoClient client,
            CredencialContainerMtrMapper mapper
    ) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    @WithSpan(value = "mtr.gestao-documento.credencial-container.gerar", kind = SpanKind.CLIENT)
    public Uni<CredencialContainer> obter() {
        Span span = Span.current();
        span.setAttribute("mtr.servico", DEPENDENCIA);
        span.setAttribute("mtr.api", "gestao-documento-v1");
        span.setAttribute("http.request.method", "POST");
        span.setAttribute(
                "url.path",
                "/simtr/gestao-documento/v1/storage/container/credencial"
        );

        ObservabilityLog.info(
                LOG,
                "mtr.gestao-documento.credencial-container.chamada.iniciada",
                ObservabilityLog.fields(
                        "camada", CAMADA,
                        "componente", COMPONENTE,
                        "dependencia", DEPENDENCIA,
                        "operacao", OPERACAO
                )
        );

        return client.gerarCredencialContainer()
                .invoke(resposta -> {
                    span.setAttribute("mtr.resposta.sucesso", true);
                    setStringAttribute(
                            span,
                            "gestao_documento.container.nome",
                            resposta != null ? resposta.nomeContainer() : null
                    );

                    ObservabilityLog.info(
                            LOG,
                            "mtr.gestao-documento.credencial-container.chamada.concluida",
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "dependencia", DEPENDENCIA,
                                    "operacao", OPERACAO,
                                    "nome_container",
                                    resposta != null ? resposta.nomeContainer() : null,
                                    "resultado", "sucesso"
                            )
                    );
                })
                .onFailure().invoke(erro -> {
                    span.recordException(erro);
                    span.setStatus(StatusCode.ERROR, String.valueOf(erro.getMessage()));
                    span.setAttribute("mtr.resposta.sucesso", false);
                    span.setAttribute("erro.tipo", tipoErroTelemetria(erro));

                    ObservabilityLog.error(
                            LOG,
                            "mtr.gestao-documento.credencial-container.chamada.falhou",
                            erro,
                            ObservabilityLog.fields(
                                    "camada", CAMADA,
                                    "componente", COMPONENTE,
                                    "dependencia", DEPENDENCIA,
                                    "operacao", OPERACAO,
                                    "erro_tipo", tipoErroSimplesTelemetria(erro),
                                    "resultado", "erro"
                            )
                    );
                })
                .map(mapper::paraDominio)
                .onFailure().transform(GestaoDocumentoMtrAdapter::traduzir);
    }

    private static void setStringAttribute(Span span, String nome, String valor) {
        if (valor != null) {
            span.setAttribute(nome, valor);
        }
    }

    private static String tipoErroTelemetria(Throwable erro) {
        if (erro instanceof GestaoDocumentoMtrException.Negocio) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrBusinessErrorException";
        }
        if (erro instanceof GestaoDocumentoMtrException.TecnicaCliente) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrClientTechnicalException";
        }
        if (erro instanceof GestaoDocumentoMtrException.Servidor) {
            return PACOTE_EXCECAO_MTR_LEGADA + "MtrServerErrorException";
        }
        return erro.getClass().getName();
    }

    private static String tipoErroSimplesTelemetria(Throwable erro) {
        String nomeCompleto = tipoErroTelemetria(erro);
        return nomeCompleto.substring(nomeCompleto.lastIndexOf('.') + 1);
    }

    private static Throwable traduzir(Throwable falha) {
        if (falha instanceof GestaoDocumentoMtrException mtr) {
            return traduzirMtr(mtr);
        }
        FalhaObtencaoCredencialContainer.Tipo tipo = falha instanceof TimeoutException
                ? FalhaObtencaoCredencialContainer.Tipo.TIMEOUT
                : FalhaObtencaoCredencialContainer.Tipo.DEPENDENCIA_INDISPONIVEL;
        return new FalhaObtencaoCredencialContainer(
                tipo,
                null,
                DEPENDENCIA,
                null,
                null,
                null,
                null,
                null,
                falha
        );
    }

    private static FalhaObtencaoCredencialContainer traduzirMtr(
            GestaoDocumentoMtrException falha
    ) {
        GestaoDocumentoMtrException.Erro erro = falha.erro();
        FalhaObtencaoCredencialContainer.Tipo tipo = switch (falha) {
            case GestaoDocumentoMtrException.Negocio ignored ->
                    FalhaObtencaoCredencialContainer.Tipo.NEGOCIO;
            case GestaoDocumentoMtrException.TecnicaCliente ignored ->
                    FalhaObtencaoCredencialContainer.Tipo.TECNICA_CLIENTE;
            case GestaoDocumentoMtrException.Servidor ignored ->
                    FalhaObtencaoCredencialContainer.Tipo.DEPENDENCIA_INDISPONIVEL;
            default -> FalhaObtencaoCredencialContainer.Tipo.DEPENDENCIA_INDISPONIVEL;
        };
        return new FalhaObtencaoCredencialContainer(
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

    private static List<String> mensagens(GestaoDocumentoMtrException.Erro erro) {
        if (erro == null || erro.erros() == null) {
            return null;
        }
        List<String> mensagens = new ArrayList<>(erro.erros().size());
        for (GestaoDocumentoMtrException.Mensagem mensagem : erro.erros()) {
            mensagens.add(mensagem != null ? mensagem.mensagem() : null);
        }
        return mensagens;
    }
}
